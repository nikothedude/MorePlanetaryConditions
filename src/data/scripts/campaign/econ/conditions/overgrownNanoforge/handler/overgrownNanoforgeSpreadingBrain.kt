package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasons
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownSpreadingParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeManipulationIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeGrowthIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeSpreadingIntel
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION

class overgrownNanoforgeSpreadingBrain(
    val industryNanoforge: overgrownNanoforgeIndustryHandler
): EconomyTickListener {

    var totalCosts: Float = 0f
    var viewingMode = viewMode.DEFAULT

    var maxGrowthManipulation: Float = OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION
    var minGrowthManipulation: Float = -maxGrowthManipulation

    fun getOverallGrowthManipulation(): Float {
        var amount = 0f
        getAllIntel().forEach { amount += kotlin.math.abs(it.growthManipulation) }
        return amount
    }

    var hidden: Boolean = true
        set(value) {
            if (field != value) {
                updateIntelHiddenStatus(value)
            }
            field = value
        }

    private fun updateIntelHiddenStatus(value: Boolean) {
        getAllIntel().forEach { it.isHidden = value }
    }
    private fun getAllIntel(): MutableSet<baseOvergrownNanoforgeIntel> {
        val allIntel = HashSet<baseOvergrownNanoforgeIntel>()
        allIntel.addAll(intelInstances)
        spreadingIntel?.let { allIntel += it } //dont remove, order of initialization is fucked
        growthIntel?.let { allIntel += it }

        for (entry in getAllHandlers()) {
            entry.manipulationIntel?.let { allIntel += it }
        }
        return allIntel
    }

    private fun getAllHandlers(): Set<overgrownNanoforgeHandler> {
        return (industryNanoforge.junkHandlers.toMutableSet() + industryNanoforge)
    }

    val intelInstances: MutableSet<baseOvergrownNanoforgeManipulationIntel> = HashSet()
    val spreadingIntel: overgrownNanoforgeSpreadingIntel = createBaseSpreadingIntel()
    var growthIntel: overgrownNanoforgeGrowthIntel? = null

    var spreadingState: spreadingStates = spreadingStates.PREPARING
        set(value) {
            val oldField = field
            field = value
            if (oldField != value) {
                oldField.unapply(this)
                value.apply(this)
            }
        }

    private fun createBaseSpreadingIntel(): overgrownNanoforgeSpreadingIntel {
        val intel = overgrownNanoforgeSpreadingIntel(this)
        intel.init(hidden)
        return intel
    }

    fun init() {
        spreadingState.apply(this)
        Global.getSector().listenerManager.addListener(this, false)
    }

    fun startSpreading(): Boolean {
        val handlerParams = createNewCreationParams() ?: return false
        growthIntel = createNewGrowthIntel(handlerParams)

        return true
    }

    fun stopSpreading() {
        growthIntel?.delete()
    }

    fun createNewGrowthIntel(params: overgrownSpreadingParams): overgrownNanoforgeGrowthIntel {
        val intel = overgrownNanoforgeGrowthIntel(this, params.handler, params)
        intel.init(hidden)
        return intel
    }

    fun createNewCreationParams(): overgrownSpreadingParams? {
        val handler: overgrownNanoforgeJunkHandler = createHandlerForParams() ?: return null
        handler.init()
        return overgrownSpreadingParams(handler)
    }

    fun createHandlerForParams(): overgrownNanoforgeJunkHandler? {
        val designation = getMarket().getNextOvergrownJunkDesignation() ?: return null
        val newHandler = overgrownNanoforgeJunkHandler(getMarket(), industryNanoforge, designation, true)
        return newHandler
    }

    fun startDestroyingStructure(handler: overgrownNanoforgeHandler) {
        val newIntel = createDestructionIntel(handler)
        newIntel.init()
    }

    private fun createDestructionIntel(handler: overgrownNanoforgeHandler): baseOvergrownNanoforgeManipulationIntel {
        return baseOvergrownNanoforgeManipulationIntel(this, handler)
    }

    fun delete() {
        Global.getSector().listenerManager.removeListener(this)
        getAllIntel().forEach { it.delete() }
    }

    fun getMarket(): MarketAPI = industryNanoforge.market
    fun getManipulationBudget(intel: baseOvergrownNanoforgeIntel): Float {
        return (maxGrowthManipulation - (getOverallGrowthManipulation() - kotlin.math.abs(intel.growthManipulation)))
    }

    fun calculateOverallCreditCost(): Float {
        var amount = 0f

        for (intel in getAllIntel()) {
            if (!intel.shouldDeductCredits()) continue

            amount += intel.calculateCreditCost()
        }

        return amount
    }

    fun getOverallCullingStrength(): Float {
        return cullingStrengthReasons.getScoreFromReasons(cullingStrengthReasons.getReasons(getMarket()))
    }

    fun getUsedStrengthPercent(): Float {
        return (getOverallGrowthManipulation())
    }

    override fun reportEconomyTick(iterIndex: Int) {
        applyCosts()
    }

    private fun applyCosts() {

        val iterations = Global.getSettings().getFloat("economyIterPerMonth")
        val iterationMult = 1f / iterations

        if (getMarket().isPlayerOwned) return deductCredits(iterationMult)
        if (getMarket().isInhabited()) return deductNPCPoints(iterationMult)
    }

    private fun deductNPCPoints(iterationMult: Float) {
        return
    }

    private fun deductCredits(iterationMult: Float) {

        val rawCost = calculateOverallCreditCost()
        val credits = Global.getSector().playerFleet.cargo.credits

        val creditsAvailable = credits.get()
        val multipliedCost = (rawCost * iterationMult)

        if (multipliedCost > creditsAvailable) {
            return insufficientCredits()
            // fun fact! returning is okay here since order of operations says we manipulate progress AFTER this!
        }

        if (multipliedCost != 0f) {
            credits.subtract(multipliedCost)
            totalCosts += multipliedCost
            //Global.getSector().campaignUI.addMessage("Deducted $finalCost credits due to ${industryNanoforge.getCurrentName()}")
        }
    }

    private fun insufficientCredits() {
        resetManipulation()

        val marketName = getMarket().name
        val title = "Insufficient credits has led to all growth manipulation attempts on $marketName to be discontinued, " +
                "to avoid putting you in debt."
        val highlight = Misc.getNegativeHighlightColor()
        val intel = MessageIntel(
            title,
            Misc.getBasePlayerColor(), arrayOf<String>(marketName, "discontinued"), highlight
        )
        intel.icon = Global.getSettings().getSpriteName("intel", "niko_MPC_priceUpdate")
        Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f)

        Global.getSector().campaignUI.addMessage(intel)
    }

    private fun resetManipulation() {
        getAllIntel().forEach { it.growthManipulation = 0f }
    }

    override fun reportEconomyMonthEnd() {
        if (totalCosts != 0f) {
            val totalStr = Misc.getDGSCredits(totalCosts)

            val title = "Monthly cost of ${getMarket().name}'s ${industryNanoforge.getCurrentName()} culling efforts: $totalStr"

            val highlight = Misc.getNegativeHighlightColor()

            val intel = MessageIntel(
                title,
                Misc.getBasePlayerColor(), arrayOf(totalStr.toString()), highlight
            )
            intel.icon = Global.getSettings().getSpriteName("intel", "monthly_income_report")

            Global.getSector().campaignUI.addMessage(intel, MessageClickAction.INCOME_TAB, Tags.INCOME_REPORT)
        }
        totalCosts = 0f
    }
}

enum class spreadingStates {
    PREPARING {
        override fun apply(brain: overgrownNanoforgeSpreadingBrain) {
            val spreadingIntel = brain.spreadingIntel

            spreadingIntel.startPreparingToSpread()
        }

        override fun unapply(brain: overgrownNanoforgeSpreadingBrain) {
            val spreadingIntel = brain.spreadingIntel

            spreadingIntel.stopPreparing()
        }

        /*private fun getAdjustedAmount(amount: Float, brain: overgrownNanoforgeSpreadingBrain): Float {
            if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED && intel.isHidden) return 0f
            var adjustedAmount = amount
            if (!intel.getMarket().isInhabited()) adjustedAmount *= OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT

            val dayAmount = Misc.getDays(adjustedAmount)

            return dayAmount
        }*/
    },
    SPREADING {
        override fun apply(brain: overgrownNanoforgeSpreadingBrain) {
            val result = brain.startSpreading()
            if (!result) {
                return niko_MPC_debugUtils.displayError("failed start spreading on a brain")
            }
            brain.industryNanoforge.notifySpreadingStarted()
        }

        override fun unapply(brain: overgrownNanoforgeSpreadingBrain) {
            brain.stopSpreading()
            brain.industryNanoforge.notifySpreadingStopped()
        }
    };

    open fun advance(amount: Float, brain: overgrownNanoforgeSpreadingBrain) { return }
    abstract fun apply(brain: overgrownNanoforgeSpreadingBrain)
    abstract fun unapply(brain: overgrownNanoforgeSpreadingBrain)
}
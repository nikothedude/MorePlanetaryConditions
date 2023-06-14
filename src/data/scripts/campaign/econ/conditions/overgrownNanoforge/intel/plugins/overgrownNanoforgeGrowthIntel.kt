package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.spreadingStates
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownSpreadingParams
import data.utilities.niko_MPC_marketUtils.isPopulationAndInfrastructure
import data.utilities.niko_MPC_marketUtils.maxStructureAmount
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

class overgrownNanoforgeGrowthIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    override val ourHandler: overgrownNanoforgeJunkHandler,
    val params: overgrownSpreadingParams,
) : baseOvergrownNanoforgeManipulationIntel(brain, ourHandler) {

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getInitialProgress())
    }

    private fun getInitialProgress(): Int {
        return ((getStartingProgressPercent()/100f)*getMaxProgress()).roundToInt()
    }

    /** Treated as a percent of max progress. */
    private fun getStartingProgressPercent(): Float {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN,
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX
        )
    }
    var knowExactEffects: Boolean = false
    var estimatedScore: String = "Error"
        get() {
            if (estimatedScoreNeedsUpdate()) field = updateEstimatedScore()
            return field
        }

    var lastProgressCheckedForEstimatedScore: Int = -5

    fun estimatedScoreNeedsUpdate(): Boolean {
        return (lastProgressCheckedForEstimatedScore != getProgress())
    }

    fun updateEstimatedScore(): String {
        val score = ourHandler.getBaseBudget() ?: return "Error"
        if (knowExactEffects) return "$score"
        val anchor = getScoreDiscoveryProgressPercent()
        lastProgressCheckedForEstimatedScore = getProgress()
        val percentComplete = progressFraction * 100
        if (percentComplete <= OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE) return "Unknown"
        val percentToAnchor = (percentComplete/anchor) * 100f

        if (percentToAnchor >= 100) return "$score"

        val discoveryMult = 100 / percentToAnchor

        val minVar = MathUtils.getRandomNumberInRange(0.8f, 1.2f)
        val maxVar = MathUtils.getRandomNumberInRange(0.8f, 1.2f)

        val min = (score*(1 / discoveryMult)*minVar).coerceAtLeast(0f)
        val max = score*(1 * discoveryMult)*maxVar

        return "$min - $max"
    }

    private fun getScoreDiscoveryProgressPercent(): Int {
        return params.percentThresholdToTotalScoreKnowledge
    }

    override fun addEndStage() {
        overgrownNanoforgeFinishGrowthStage(this).init()
    }

    override fun addInitialStages() {
        super.addInitialStages()

        growthDiscoveryStages.TARGET.getChildren(this).forEach { it.init() }
        growthDiscoveryStages.EFFECTS.getChildren(this).forEach { it.init() }
    }

    fun growingComplete() {
        params.spread()

        delete()
    }

    override fun delete() {
        super.delete()
        brain.spreadingState = spreadingStates.PREPARING
    }

    override fun addParamsInfo(info: TooltipMakerAPI, width: Float, stageId: Any?): UIComponentAPI {
        val prevTable = super.addParamsInfo(info, width, stageId)

        val targetWidth = 180f
        val scoreWidth = 300f

        info.beginTable(factionForUIColors, 20f,
    "Target", targetWidth,
        )
        info.addTableHeaderTooltip(0, "The structure this growth is targeting. Guaranteed to be nothing if " +
                "the market does not have ${maxStructureAmount} visible structures. If this growth completes with an active target, " +
                "the target structure will be destroyed and replaced by the growth.")
        info.addTableHeaderTooltip(1, "If Population and Infrastructure is destroyed by the growth, the market will be " +
                "decivilized. Do not let this happen.")

        val targetData = getTargetData()

        val baseAlignment = Alignment.LMID
        val baseColor = Misc.getBasePlayerColor()

        val targetColor = if (params.ourIndustryTarget == null) baseColor else Misc.getNegativeHighlightColor()
        info.addRowWithGlow(
            baseAlignment, targetColor, targetData.name)
        targetData.industry?.let { info.setIdForAddedRow(it) }

        val opad = 5f
        info.addTable("None", -1, opad)
        info.addSpacer(3f)

        info.beginTable(factionForUIColors, 20f,
            "Overall score", scoreWidth)
        info.addTableHeaderTooltip(0, "An estimation of the overall score of the growth. " +
                "A higher score means more intense effects, both negative and positive.")
        info.addRowWithGlow(
            Alignment.MID, baseColor, estimatedScore
        )
        info.addTable("None", -1, opad)

        return info.prev
    }

    override fun getPositiveStringData(): MutableSet<stringData> {
        if (knowExactEffects) return super.getPositiveStringData()

        return setOf(stringData("Unknown"))
    }

    override fun getNegativeStringData(): MutableSet<stringData> {
        if (knowExactEffects) return super.getNegativeStringData()

        return setOf(stringData("Unknown"))
    }

    override fun tableRowClicked(ui: IntelUIAPI, data: IntelInfoPlugin.TableRowClickData) {
        super.tableRowClicked(ui, data)

        val id = data.rowId
        if (id is Industry) {
            val market = id.market
            // TODO open market screen
        }
    }

    private fun getTargetData(): targetData {
        if (params.nameKnown) return targetData(params.ourIndustryTarget, params.getIndustryName())

        val data = targetData(null, params.getIndustryName())
        return data
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        params.updateIndustryTarget()
    }

    override fun getName(): String {
        return "Growth on ${getMarket().name}"
    }

    override fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addBasicDescription(info, width, stageId)

        info.addPara("This specific ${ourHandler.getCurrentName()} is %s and is %s. As such, its" +
                " abilities and characteristics are %s, and they will %s until it is %s.", 5f,
        Misc.getHighlightColor(), "not established", "still growing", "unknown", "not take effect", "fully grown")
        info.addPara("As the ${ourHandler.getCurrentName()} grows, more details will reveal themselves, such as " +
                "the effects of the structure, the target it seeks to destroy, or the overall score of it.", 5f)
    }

    override fun culled() {
        super.culled()

        delete() // needed since the junk handler has no ref to us
    }

    override fun getSpreadingAdjective(): String = "spreading"

    override fun getTextForCulled(): String {
        return "The growth of the %s on %s has been stopped, restarting the process."
    }

    override fun getCulledTextHighlights(): Array<String> {
        return (arrayOf(ourHandler.getCurrentName(), getMarket().name))
    }

    override fun getIntelToLinkWhenCulled(): baseOvergrownNanoforgeIntel {
        return brain.spreadingIntel
    }

    fun alertPlayerTargetChanged(newIndustry: Industry?) {
        if (!playerCanManipulateGrowth()) return
        val suffix = if (newIndustry == null) "nothing" else newIndustry.currentName
        val base = "Growth on %s now targetting %s"
        val targetColor = if (newIndustry == null) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
        val intel = MessageIntel(
            base,
            Misc.getBasePlayerColor(),
            arrayOf(getMarket().name, suffix),
            Misc.getHighlightColor(), targetColor
        )
        intel.icon = icon
        if (newIndustry != null) {
            val soundId = if (newIndustry.isPopulationAndInfrastructure()) "cr_playership_critical" else "cr_playership_warning"
            Global.getSoundPlayer().playUISound(soundId, 1f, 1f)
        }
        Global.getSector().campaignUI.addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, this)
    }

    fun destroyTarget(): Boolean {
        val target = params.ourIndustryTarget
        if (target != null) {
            if (!isHidden) sendTargetDestroyedMessage(target)
            if (target.isPopulationAndInfrastructure()) {
                DecivTracker.decivilize(getMarket(), false)
                return true
            }
            getMarket().removeIndustry(target.id, null, false)
        }
        return false
    }

    protected fun sendTargetDestroyedMessage(target: Industry) {
        var message = "The ${ourHandler.getCurrentName()} on ${getMarket().name} has successfully %s, overtaking and %s the %s."
        var highlights = arrayOf("spreaded", "destroying", target.currentName)
        if (target.isPopulationAndInfrastructure()) {
            message += " The government has lost its final stronghold against the nanoforge menace, and the population has scattered. " +
                    "%s."
            highlights += "The colony is lost"
        }
        val intel = MessageIntel(
            message,
            Misc.getBasePlayerColor(),
            highlights,
            Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor()
        )
        intel.icon = icon
        Global.getSector().campaignUI.addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, brain.getIndustryIntel())
    }
}

class targetData(
    var industry: Industry?,
    val name: String = industry?.currentName ?: "None"
) {

}


class overgrownNanoforgeFinishGrowthStage(override val intel: overgrownNanoforgeGrowthIntel)
    : overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Growth Finished"
    override fun getDesc(): String = "Once reached, the growth will become permanent and begin applying its effects."

    override fun stageReached() {
        intel.growingComplete()
    }
    override fun getThreshold(): Int = intel.maxProgress
}
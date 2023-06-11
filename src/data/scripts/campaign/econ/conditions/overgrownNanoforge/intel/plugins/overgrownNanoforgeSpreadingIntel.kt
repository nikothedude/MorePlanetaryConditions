package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.spreadingStates
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.baseOvergrownNanoforgeEventFactor
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SPREADING_MAX_PROGRESS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SPREADING_PROGRESS
import org.lazywizard.lazylib.MathUtils
import java.awt.Color
import kotlin.math.roundToInt

class overgrownNanoforgeSpreadingIntel(
    brain: overgrownNanoforgeSpreadingBrain,
) : baseOvergrownNanoforgeIntel(brain) {


    fun getTimeTilNextSpread(): Int {
        return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    }

    override fun initializeProgress() {
        setProgress(0)
    }

    override fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addBasicDescription(info, width, stageId)

        info.addPara("This panel represents the nanoforge's constant attempts at spreading. When progress reaches %s, " +
                "a %s will begin, spawning a new intel panel representing a %s.", 5f,
            Misc.getHighlightColor(), "max", "new spreading attempt", "growth in progress")
        info.addPara("Keeping progress locked at %s is, while expensive, %s at %s.", 5f,
            Misc.getHighlightColor(), "0%", "effective", "suppressing the nanoforge")
    }

    fun startPreparingToSpread() {
        Global.getSector().intelManager.addIntel(this, true)
        setProgress(0)
        overgrownNanoforgePrepareGrowthFactor(this, getBaseProgress()).init()

        val max = getTimeTilNextSpread()
        setMaxProgress(max)
        overgrownNanoforgeStartSpreadStage(this).init()
    }

    fun getBaseProgress(): Int {
        return OVERGROWN_NANOFORGE_SPREADING_PROGRESS
    }

    fun stopPreparing() {
        Global.getSector().intelManager.removeIntel(this)
        removeFactorOfClass(overgrownNanoforgePrepareGrowthFactor::class.java as Class<EventFactor>)

        localGrowthManipulationPercent = 0f
/*        val iterator = stages.iterator()
        while (iterator.hasNext()) {
            val stage = iterator.next().id
            if (stage is overgrownNanoforgeStartSpreadStage) iterator.remove()
        }*/
    }//commented out due to commod error

    fun startSpreading() {
        brain.spreadingState = spreadingStates.SPREADING
    }

    fun stopSpreading() {
        brain.stopSpreading()
    }

    override fun getName(): String {
        return "Spreading progress of ${brain.industryNanoforge.getCurrentName()} on ${getMarket().name}"
    }

}

class overgrownNanoforgeStartSpreadStage(override val intel: overgrownNanoforgeSpreadingIntel)
    : overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Spread starts"
    override fun getDesc(): String = "Growth begins, creating a new growth that must be culled or cultivated."

    override fun stageReached() {
        intel.startSpreading()
    }

    override fun getThreshold(): Int = intel.maxProgress

}

class overgrownNanoforgePrepareGrowthFactor(
    override val overgrownIntel: overgrownNanoforgeSpreadingIntel,
    val baseProgress: Int)
    : baseOvergrownNanoforgeEventFactor(overgrownIntel) {

    override fun getProgress(intel: BaseEventIntel?): Int {
        return getAdjustedProgress()
    }

    private fun getAdjustedProgress(): Int {
        val industryHandler = overgrownIntel.brain.industryNanoforge
        val maxJunk = industryHandler.getMaxJunkAllowed()
        if (getMarket().industries.size >= maxJunk) return 0
        val discovered = industryHandler.discovered
        var adjustedProgress: Float = baseProgress.toFloat()
        if (!discovered) {
            if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED) return 0
        }
        if (!industryHandler.market.isInhabited()) adjustedProgress *= OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT

        return adjustedProgress.roundToInt()
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Spreading preparation"
    }

    override fun createTooltip(): BaseFactorTooltip {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                if (tooltip == null) return
                val opad = 10f

                val stringToAdd = "The Overgrown Nanoforge on ${getMarket().name} is preparing for a growth at a rate of ${getProgress(overgrownIntel)} per month."
                tooltip.addPara(stringToAdd, opad)
            }
        }
    }

    override fun getProgressColor(intel: BaseEventIntel?): Color {
        return Misc.getNegativeHighlightColor()
    }

    override fun getDescColor(intel: BaseEventIntel?): Color {
        return Misc.getNegativeHighlightColor()
    }

}
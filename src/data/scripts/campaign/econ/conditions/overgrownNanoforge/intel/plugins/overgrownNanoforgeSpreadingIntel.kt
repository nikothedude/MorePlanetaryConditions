package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.spreadingStates
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.baseOvergrownNanoforgeEventFactor
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SPREADING_PROGRESS
import org.lazywizard.lazylib.MathUtils
import java.awt.Color
import kotlin.math.roundToInt

class overgrownNanoforgeSpreadingIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    hidden: Boolean = true,
) : baseOvergrownNanoforgeIntel(brain, hidden) {


    fun getTimeTilNextSpread(): Int {
        return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    }

    override fun init() {
        super.init()
    }

    override fun initializeProgress() {
        setProgress(0)
    }

    fun startPreparingToSpread() {
        Global.getSector().intelManager.addIntel(this, true)
        setProgress(0)
        addFactorWrapped(overgrownNanoforgePrepareGrowthFactor(this, getBaseProgress()))
        val max = getTimeTilNextSpread()
        setMaxProgress(max)
        addStage(overgrownNanoforgeStartSpreadStage(this), getMaxProgress())
    }

    fun getBaseProgress(): Int {
        return OVERGROWN_NANOFORGE_SPREADING_PROGRESS
    }

    fun stopPreparing() {
        Global.getSector().intelManager.removeIntel(this)
        removeFactorOfClass(overgrownNanoforgePrepareGrowthFactor::class.java as Class<EventFactor>)
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
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        intel.startSpreading()
    }

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
        val discovered = industryHandler.discovered
        var adjustedProgress: Float = baseProgress.toFloat()
        if (!discovered) {
            if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED) return 0
        }
        if (!industryHandler.market.isInhabited()) adjustedProgress *= OVERGROWN_NANOFORGE_NOT_INHABITED_PROGRESS_MULT

        return adjustedProgress.roundToInt()
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "awdhjuwad"
    }

    override fun getProgressColor(intel: BaseEventIntel?): Color {
        return Misc.getNegativeHighlightColor()
    }

    override fun getDescColor(intel: BaseEventIntel?): Color {
        return Misc.getNegativeHighlightColor()
    }

}
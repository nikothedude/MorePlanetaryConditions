package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeGrowthIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    nanoforge: overgrownNanoforgeHandler
) : baseOvergrownNanoforgeManipulationIntel(brain, nanoforge) {

    enum class spreadingStates {
        IN_BETWEEN {
            override fun apply(intel: overgrownNanoforgeGrowthIntel) {
                intel.addFactor(overgrownNanoforgeGrowthCooldownFactor(this))
                val max = MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
                intel.setMaxProgress(max)
                intel.addStage(overgrownNanoforgeBeginSpreadingStage(intel.brain, intel), intel.getMaxProgress())
            }

            override fun unapply(intel: overgrownNanoforgeGrowthIntel) {
                intel.removeFactorOfClass(overgrownNanoforgeGrowthCooldownFactor::class.java as Class<EventFactor>)
                val iterator = intel.stages.iterator()
                while (iterator.hasNext()) {
                    val stage = iterator.next()
                    if (stage is overgrownNanoforgeBeginSpreadingStage) iterator.remove()
                }
                intel.spreadIntervalTimer.advance(Float.MAX_VALUE)
            }

            private fun getAdjustedAmount(amount: Float, intel: overgrownNanoforgeGrowthIntel): Float {
                if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED && intel.isHidden) return 0f
                var adjustedAmount = amount
                if (!intel.getMarket().isInhabited()) adjustedAmount *= OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT

                val dayAmount = Misc.getDays(adjustedAmount)

                return dayAmount
            }
        },
        SPREADING {
            override fun apply(intel: overgrownNanoforgeGrowthIntel) {
                intel.paramsForSpreading = intel.createNewParamsForSpreading() ?: return
                val newHandler = intel.paramsForSpreading!!.handler
                intel.setMaxProgress(newHandler.cullingResistance)
                intel.addStage(overgrownNanoforgeCompleteSpreadStage(intel.brain, intel), intel.getMaxProgress())
                intel.addFactor(overgrownNanoforgePassiveRegenerationFactor(intel.brain, intel, newHandler))
            }

            override fun unapply(intel: overgrownNanoforgeGrowthIntel) {
                intel.removeFactorOfClass(overgrownNanoforgePassiveRegenerationFactor::class.java as Class<EventFactor>)
                val iterator = intel.stages.iterator()
                while (iterator.hasNext()) {
                    val stage = iterator.next()
                    if (stage is overgrownNanoforgeCompleteSpreadStage) iterator.remove()
                }
            }
        };
        open fun advance(amount: Float, intel: overgrownNanoforgeGrowthIntel) { return }
        abstract fun apply(intel: overgrownNanoforgeGrowthIntel)
        abstract fun unapply(intel: overgrownNanoforgeGrowthIntel)
    }

    var spreadingState: spreadingStates = spreadingStates.IN_BETWEEN
        set(value) {
            if (value != field) {
                field.unapply(this)
                value.apply(this)
            }
            field = value
        }

    val spreadIntervalTimer: IntervalUtil = IntervalUtil(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    var paramsForSpreading: overgrownSpreadingParams? = null
    private fun createNewParamsForSpreading(): overgrownSpreadingParams? {
        val coreHandler = getMarket().getOvergrownNanoforgeIndustryHandler() ?: return null
        val designation = getMarket().getNextOvergrownJunkDesignation() ?: return null
        val handler = overgrownNanoforgeJunkHandler(getMarket(), coreHandler, designation)
        val params = overgrownSpreadingParams(handler)
        params.init()
        return overgrownSpreadingParams(handler)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        spreadingState.advance(amount, this)
    }

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)
        addParamsInfo(info, width, stageId)
    }

}
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
    nanoforge: overgrownNanoforgeHandler,
    ourHandler: overgrownNanoforgeHandler
    val params: overgrownSpreadingParams
) : baseOvergrownNanoforgeManipulationIntel(brain, nanoforge, ourHandler) {

    override fun addEndStage() {
        addStage(overgrownNanoforgeFinishGrowthStage(brain, this))
    }

    fun growingComplete() {
        params.handler.instantiate()

        brain.spreadingState = spreadingStates.PREPARING
    }

    override fun delete() {
        super.delete()
        params.handler.delete()
        brain.spreadingState = spreadingStates.PREPARING
    }

}

class overgrownNanoforgeFinishGrowthStage(brain: overgrownNanoforgeSpreadingBrain, intel: overgrownNanoforgeGrowthIntel)
    : overgrownNanoforgeIntelStage(brain, intel) {
    
    val castedIntel: overgrownNanoforgeGrowthIntel = intel
    
    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        castedIntel.growingComplete()
    }

}
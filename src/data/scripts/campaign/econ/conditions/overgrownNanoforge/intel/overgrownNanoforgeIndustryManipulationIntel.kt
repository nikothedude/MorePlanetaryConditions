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

class overgrownNanoforgeIndustryManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    nanoforge: overgrownNanoforgeHandler,
    override val ourHandler: overgrownNanoforgeIndustryHandler
): baseOvergrownNanoforgeManipulationIntel(brain, nanoforge, ourHandler) {

    var exposed: Boolean = false
        set(value) {
            val oldField = field
            field = value
            if (oldField != field) {
                updateExposed()
            }
        }

    fun updateExposed() {
        updateFactors()
        updateUI()

        if (!exposed) growthManipulation = 0f
    }

    override fun playerCanManipulateGrowth(): Boolean {
        return (exposed && super.playerCanManipulateGrowth())
    }

    fun updateFactors() {
        addFactors()
    }

    fun updateUI() {

    }

    override fun addCountermeasuresFactor() {
        addFactor(overgrownNanoforgeIndustryIntelCountermeasures(this))
    }
}

open class overgrownNanoforgeIndustryIntelCountermeasures(override val overgrownIntel: overgrownNanoforgeIndustryManipulationIntel) : baseOvergrownNanoforgeEventFactor(
    overgrownIntel
): overgrownNanoforgeIntelFactorCountermeasures(overgrownIntel) {

    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!overgrownIntel.exposed) return 0
        return super.getProgress(intel)
    }

}

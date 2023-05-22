package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorCountermeasures
import java.awt.Color

class overgrownNanoforgeIndustryManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    override val ourHandler: overgrownNanoforgeIndustryHandler,
): baseOvergrownNanoforgeManipulationIntel(brain, ourHandler) {

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

        if (!exposed) growthManipulation = 0f
    }

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)
    }

    override fun playerCanManipulateGrowth(): Boolean {
        return (exposed && super.playerCanManipulateGrowth())
    }

    override fun getCantInteractWithInputReasons(): MutableSet<String> {
        val reasons = super.getCantInteractWithInputReasons()

        if (!exposed) reasons += "The nanoforge itself is protected by its growths, remove them first."

        return reasons
    }

    fun updateFactors() {
        addFactors()
    }

    override fun addCountermeasuresFactor() {
        addFactorWrapped(overgrownNanoforgeIndustryIntelCountermeasures(this))
    }
}

open class overgrownNanoforgeIndustryIntelCountermeasures(override val overgrownIntel: overgrownNanoforgeIndustryManipulationIntel
): overgrownNanoforgeIntelFactorCountermeasures(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!overgrownIntel.exposed) return 0
        return super.getProgress(intel)
    }
}

package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorCountermeasures
import java.awt.Color

class overgrownNanoforgeIndustryManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    override val ourHandler: overgrownNanoforgeIndustryHandler,
    hidden: Boolean = true,
): baseOvergrownNanoforgeManipulationIntel(brain, ourHandler, hidden) {

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

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)
    }

    override fun playerCanManipulateGrowth(): Boolean {
        return (exposed && super.playerCanManipulateGrowth())
    }

    fun updateFactors() {
        addFactors()
    }

    override fun addCountermeasuresFactor() {
        addFactorWrapped(overgrownNanoforgeIndustryIntelCountermeasures(this))
    }

    override fun createCantInteractWithInputTooltip(): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)
                if (tooltip == null) return

                tooltip.addPara("The nanoforge itself is protected by its growths, remove them first.", 5f)
            }
        }
    }
}

open class overgrownNanoforgeIndustryIntelCountermeasures(override val overgrownIntel: overgrownNanoforgeIndustryManipulationIntel
): overgrownNanoforgeIntelFactorCountermeasures(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!overgrownIntel.exposed) return 0
        return super.getProgress(intel)
    }
}

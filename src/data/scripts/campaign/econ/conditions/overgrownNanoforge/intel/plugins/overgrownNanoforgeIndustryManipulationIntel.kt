package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorCountermeasures
import data.scripts.campaign.intel.baseNikoEventStageInterface

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

        if (!exposed) localGrowthManipulationPercent = 0f
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
        addInitialFactors()
    }

    override fun addCountermeasuresFactor() {
        overgrownNanoforgeIndustryIntelCountermeasures(this).init()
    }

    override fun isLargeIntel(): Boolean = true

    override fun getTextForCulled(): String {
        val original = super.getTextForCulled()
        return "$original Due to the fact this was the core of the nanoforge, the %s on said market has been destroyed, and " +
                "a %s has been deposited into it's storage."
    }

    override fun getCulledTextHighlights(): Array<String> {
        val original = super.getCulledTextHighlights()
        return (arrayOf(*original, ourHandler.getCurrentName(), "special item"))
    }

    override fun getIntelToLinkWhenCulled(): baseOvergrownNanoforgeIntel? {
        return null
    }
}

open class overgrownNanoforgeIndustryIntelCountermeasures(override val overgrownIntel: overgrownNanoforgeIndustryManipulationIntel
): overgrownNanoforgeIntelFactorCountermeasures(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!overgrownIntel.exposed) return 0
        return super.getProgress(intel)
    }
}

package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventClass
import com.fs.starfarer.api.input.InputEventType
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorCountermeasures
import data.scripts.campaign.intel.baseNikoEventStageInterface
import data.utilities.niko_MPC_settings
import lunalib.lunaExtensions.addLunaToggleButton
import lunalib.lunaUI.elements.LunaProgressBar

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

    override fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addBasicDescription(info, width, stageId)

        info.addPara(
            "Culling the core will allow the usage of it as a potent %s, which is installable in any industry and " +
                    "massively boosts production and fleet size (if on heavy industry) at the cost of demand, upkeep, and ship quality.",
            5f,
            Misc.getHighlightColor(),
            "colony item")

    }

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)
        val overallSuppressionMeter = overallManipulationMeter?.get() ?: return
        if (playerCanManipulateGrowth()) {
            addFocusButton(info, overallSuppressionMeter)
        }
    }

    fun addFocusButton(info: TooltipMakerAPI, overallSuppressionMeter: LunaProgressBar) {
        val focusButton = info.addLunaToggleButton(ourHandler.focusingOnExistingCommodities, 100f, overallSuppressionMeter.height)
        focusButton.position.rightOfMid(overallSuppressionMeter.elementPanel, 5f)
        focusButton.onInput { ourHandler.focusingOnExistingCommodities = focusButton.value }
        val extraWeight = niko_MPC_settings.OVERGROWN_NANOFORGE_ALREADY_PRODUCING_COMMODITY_WEIGHT_MULT
        val descTooltip = focusButton.addTooltip(
            "If enabled, commodities already being produced are %s more likely to be picked for new growths.",
            300f,
            TooltipMakerAPI.TooltipLocation.BELOW,
            "${extraWeight}x"
        )
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

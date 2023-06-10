package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeManipulationIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeGrowthIntel
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import java.awt.Color

class overgrownNanoforgeIntelFactorStructureRegeneration(
    override val overgrownIntel: baseOvergrownNanoforgeManipulationIntel
): baseOvergrownNanoforgeEventFactor(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        return overgrownIntel.ourHandler.cullingResistanceRegeneration
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Base spreading rate"
    }

    override fun getDescColor(intel: BaseEventIntel?): Color {
        return Misc.getNegativeHighlightColor()
    }

    override fun createTooltip(): BaseFactorTooltip {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                if (tooltip == null) return
                val opad = 10f

                val stringToAdd = "The ${getNanoforgeName()} on ${getMarket().name} is ${overgrownIntel.getSpreadingAdjective()} " +
                        "at a rate of ${getProgress(overgrownIntel)} per month."
                tooltip.addPara(stringToAdd, opad)
            }
        }
    }
    override fun isNaturalGrowthFactor(): Boolean = true
}

class overgrownNanoforgeIntelFactorUndiscovered(
    override val overgrownIntel: overgrownNanoforgeGrowthIntel
    ): baseOvergrownNanoforgeEventFactor(overgrownIntel) {

    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED) return 0
        if (!overgrownIntel.isHidden) return 0
        return -(overgrownIntel.getNaturalGrowthInt())
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "You shouldn't see this"
    }
}

class overgrownNanoforgeIntelFactorTooManyStructures(
    override val overgrownIntel: overgrownNanoforgeGrowthIntel
): baseOvergrownNanoforgeEventFactor(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        return if (overgrownIntel.getMarket().exceedsMaxStructures()) -overgrownIntel.getNaturalGrowthInt() else 0
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Not enough space to grow"
    }

    override fun createTooltip(): BaseFactorTooltip {
        return object: BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                if (tooltip == null) return
                val opad = 10f

                val stringToAdd = "${getMarket().name} has too many structures, making the ${getNanoforgeName()} unable to expand. " +
                        "This has the effect of completely halting all natural growth, making it far easier to cull any growths."
                tooltip.addPara(stringToAdd, opad)
            }
        }
    }
}

open class overgrownNanoforgeIntelFactorCountermeasures(overgrownIntel: baseOvergrownNanoforgeIntel) : baseOvergrownNanoforgeEventFactor(
    overgrownIntel
) {

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        val initial = overgrownIntel.getOverallCullingStrength(getMarket())
        val initialTwo = -(overgrownIntel.localGrowthManipulationPercent/100f)
        val result = initial * initialTwo
        return result.toInt()
    }

    private fun isCulling(): Boolean {
        return getProgress(overgrownIntel) <= 0
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return if (isCulling()) "Growth Suppression" else "Growth Cultivation"
    }

    override fun getDescColor(intel: BaseEventIntel?): Color {
        if (getProgress(intel) == 0) return Misc.getGrayColor()
        return if (isCulling()) Misc.getPositiveHighlightColor() else (Misc.getHighlightColor())
    }

    override fun shouldBeRemovedWhenSpreadingStops(): Boolean {
        return false
    }

    override fun createTooltip(): BaseFactorTooltip {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                if (tooltip == null) return
                val opad = 10f

                val stringToAdd =
                    "${getMarket().name}'s government is currently using ${getUsedStrengthPercent()}% of its" +
                            " culling strength towards ${cullingOrCultivating()} the ${overgrownIntel.brain.industryNanoforge.getCurrentName()}'s growth."

                tooltip.addPara(stringToAdd, opad)
            }
        }
    }

    protected fun cullingOrCultivating(): String {
        return if (isCulling()) "culling" else "cultivating"
    }

    protected fun getUsedStrengthPercent(): Float {
        return overgrownIntel.brain.getUsedStrengthPercent()
    }
}
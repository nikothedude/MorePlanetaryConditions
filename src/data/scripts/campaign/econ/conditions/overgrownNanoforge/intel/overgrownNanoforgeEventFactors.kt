package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED
import kotlin.math.roundToInt

class overgrownNanoforgeBaseIntelFactor(
    val baseProgress: Int, overgrownIntel: overgrownNanoforgeIntel
): baseOvergrownNanoforgeEventFactor(overgrownIntel) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        return baseProgress
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "a"
    }

    override fun addExtraRows(info: TooltipMakerAPI?, intel: BaseEventIntel?) {
        super.addExtraRows(info, intel)
    }
}

class overgrownNanoforgeIntelFactorUndiscovered(overgrownIntel: overgrownNanoforgeIntel) :
    baseOvergrownNanoforgeEventFactor(overgrownIntel), EventFactor {
    override fun getProgress(intel: BaseEventIntel?): Int {
        if (!OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED) return 0
        if (!overgrownIntel.isHidden) return 0
        return -(overgrownIntel.baseFactor!!.baseProgress * 0.9).roundToInt()
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "You shouldn't see this"
    }
}

class overgrownNanoforgeIntelFactorTooManyStructures(overgrownIntel: overgrownNanoforgeIntel) : baseOvergrownNanoforgeEventFactor(
    overgrownIntel
) {
    override fun getProgress(intel: BaseEventIntel?): Int {
        return if (overgrownIntel.getMarket().exceedsMaxStructures()) -overgrownIntel.baseFactor!!.baseProgress else 0
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "${getMarket().name} has too many structures, making the overgrown nanoforge unable to expand."
    }
}
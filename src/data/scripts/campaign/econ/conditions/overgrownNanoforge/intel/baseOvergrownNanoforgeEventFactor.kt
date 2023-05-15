package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI

abstract class baseOvergrownNanoforgeEventFactor(
    val overgrownIntel: overgrownNanoforgeIntel
): baseNikoEventFactor() {
    open fun shouldBeRemovedWhenSpreadingStops(): Boolean {
        return true
    }

    fun getMarket(): MarketAPI {
        return overgrownIntel.getMarket()
    }

    open fun createTooltip(): BaseFactorTooltip? = null

    fun getNanoforgeName(): String = overgrownIntel.ourNanoforgeHandler.getCurrentName()

    override fun addExtraRows(info: TooltipMakerAPI?, intel: BaseEventIntel?) {
        if (info == null) return
        val tooltip = createTooltip() ?: return

        info.addTooltipToAddedRow(tooltip, TooltipMakerAPI.TooltipLocation.RIGHT, false)
    }
}
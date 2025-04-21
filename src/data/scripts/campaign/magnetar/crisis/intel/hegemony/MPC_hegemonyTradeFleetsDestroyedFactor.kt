package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator

open class MPC_hegemonyTradeFleetsDestroyedFactor(points: Int) : BaseOneTimeFactor(points) {

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Trade fleet ships destroyed"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    ("Ships belonging to trade fleets and smugglers shipping goods "
                            + "to or from a Hegemony colony, "
                            + "regardless of their faction, destroyed by your fleet."),
                    0f
                )
            }
        }
    }
}
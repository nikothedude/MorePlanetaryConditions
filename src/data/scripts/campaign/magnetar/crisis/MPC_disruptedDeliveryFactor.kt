package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator

class MPC_disruptedDeliveryFactor(points: Int) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String {
        return "Enigmatic delivery disrupted"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator? {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "You've seized a cache of supplies being hidden within your territory. While this will no doubt " +
                        "set the perpetrator's plans back, there's no telling how many more you haven't found.",
                    0f
                )
            }
        }
    }
}
package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator

open class MPC_IAIICMilitaryDestroyedFactor(points: Int) : BaseOneTimeFactor(points) {

    override fun getDesc(intel: BaseEventIntel?): String {
        return "IAIIC patrols destroyed"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "IAIIC patrols in your space, destroyed by your fleet.",
                    0f
                )
            }
        }
    }
}
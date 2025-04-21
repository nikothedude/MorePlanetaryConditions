package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator

open class MPC_hegemonyMilHouseRaidIndFactor(points: Int, val desc: String) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String? {
        return desc
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    ("Disrupted operations "
                            + "on Hegemony colonies, through raids or bombardment. More effective and longer lasting"
                            + " disruptions result in more event progress points."),
                    0f
                )
                tooltip.addPara(
                    "Repeatedly disrupting the same industry will have no additional effect.",
                    0f
                )
            }
        }
    }
}
package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator

open class MPC_hegemonyMilHouseNanoforgeStolenFactor(points: Int, val desc: String) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String? {
        return desc
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                tooltip.addPara(
                    "Stealing the pristine nanoforge from Chicomoztoc, if it has one, will have a tremendous effect on the Mellour's resolve.",
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
package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseOneTimeFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor

open class MPC_IAIICTradeDestroyedFactor(points: Int) : BaseOneTimeFactor(points) {
    override fun getDesc(intel: BaseEventIntel?): String {
        return "IAIIC trade disrupted"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "Trade fleets headed to or from %s, destroyed by your fleet.",
                    0f,
                    Misc.getHighlightColor(),
                    "${MPC_fractalCoreFactor.getFOB()?.name}"
                )
            }
        }
    }
}
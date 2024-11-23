package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.utilities.niko_MPC_ids

class MPC_IAIICEscalationFactor: BaseEventFactor() {

    companion object {
        const val ESCALATION_TO_POINTS_MULT = 3f
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        val FOB = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return 0
        val escalation = FOB.memoryWithoutUpdate.getFloat(niko_MPC_ids.MPC_IAIIC_ESCALATION_ID)
        if (escalation == 0f) return 0

        return (escalation * ESCALATION_TO_POINTS_MULT).toInt()
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Escalation"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "Seeking a hasty end to this conflict, the IAIIC's benefactors are investing further in the project. " +
                        "While has the desired effect of increasing military strength, it also hastens their impatience.",
                    0f
                )
            }
        }
    }
}
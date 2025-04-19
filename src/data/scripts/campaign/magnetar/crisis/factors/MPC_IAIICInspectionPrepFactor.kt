package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_IAIICInterferenceCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionPrepIntel
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_IAIICInspectionPrepFactor: BaseEventFactor() {
    companion object {
        const val BASE_PROGRESS = 35
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        val prepIntel = MPC_IAIICInspectionPrepIntel.get() ?: return 0
        val prepState = prepIntel.getPreparingState()
        val ourIntel = MPC_IAIICFobIntel.get() ?: return 0
        if (ourIntel.currentAction != null) return 0
        if (ourIntel.disruptedCommandDaysLeft > 0f) return 0
        if (prepState.isPreparing) return BASE_PROGRESS else return 0
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Base progress"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {

                val ourIntel = MPC_IAIICFobIntel.get() ?: return
                if (ourIntel.currentAction != null) {
                    tooltip.addPara(
                        "Progress is paused while a hostile action is on-going.",
                        5f
                    )
                    return
                }
                if (ourIntel.disruptedCommandDaysLeft > 0f) {
                    tooltip.addPara(
                        "Due to the recent blow to the IAIIC's command structure, inspections are %s for %s days.",
                        5f,
                        Misc.getHighlightColor(),
                        "postponed", "${MPC_IAIICFobIntel.get()?.disruptedCommandDaysLeft?.roundNumTo(1)?.trimHangingZero()}"
                    )
                    return
                }

                val prepIntel = MPC_IAIICInspectionPrepIntel.get() ?: return
                val prepState = prepIntel.getPreparingState()
                prepState.createDesc(tooltip, getProgress(prepIntel))
            }
        }
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }
}
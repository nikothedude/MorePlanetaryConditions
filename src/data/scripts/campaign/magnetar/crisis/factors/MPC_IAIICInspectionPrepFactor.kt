package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_IAIICInterferenceCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionPrepIntel

class MPC_IAIICInspectionPrepFactor: BaseEventFactor() {
    companion object {
        const val BASE_PROGRESS = 1
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        val prepIntel = MPC_IAIICInspectionPrepIntel.get() ?: return 0
        val prepState = prepIntel.getPreparingState()
        if (prepState.isPreparing) return BASE_PROGRESS else return 0
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Base progress"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
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
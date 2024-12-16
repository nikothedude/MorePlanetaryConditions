package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.MPC_IAIICInterferenceCondition
import kotlin.math.roundToInt

class MPC_IAIICAttritionFactor: BaseEventFactor() {

    companion object {
        const val NON_HOSTILE_PROGRESS_MULT = 0.25f

        const val BASE_PROGRESS = 30f
        const val MIN_PROGRESS = 1f
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        if (MPC_IAIICFobIntel.get()?.currentAction != null) return 0
        var progress = BASE_PROGRESS

        val IAIICStrength = MPC_IAIICFobIntel.getIAIICStrengthInSystem()
        progress *= (1f - IAIICStrength)
        if (MPC_IAIICInterferenceCondition.isHostile()) {
            progress *= NON_HOSTILE_PROGRESS_MULT
        }
        progress = progress.coerceAtLeast(MIN_PROGRESS)

        return progress.roundToInt()
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Attrition"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "The hands behind the IAIIC grow weary as time goes on and resource expenditure climbs. Attrition is based on " +
                        "the IAIIC's relative strength in your systems, which is currently %s.",
                    5f,
                    Misc.getHighlightColor(),
                    ("${(MPC_IAIICFobIntel.getIAIICStrengthInSystem() * 100f).toInt()}%")
                )
                if (MPC_IAIICFobIntel.get()?.currentAction != null) {
                    tooltip.addPara(
                        "Progress is paused while a hostile action is on-going.",
                        5f
                    )
                    return
                }
                tooltip.addPara(
                    "Progress can never fall below %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "${MIN_PROGRESS.toInt()}"
                )

                if (!MPC_IAIICInterferenceCondition.isHostile()) {
                    tooltip.addPara(
                        "Due to their nominally \"non-hostile\" stance against you, the IAIIC's rate of attrition is %s. Declaring war would" +
                        " surely escalate the conflict and speed things up.",
                        5f,
                        Misc.getNegativeHighlightColor(),
                        "severely limited"
                    )
                }
            }
        }
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return true
    }

}
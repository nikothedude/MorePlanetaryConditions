package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.roundToInt

class MPC_IAIICShortageFactor: BaseEventFactor() {

    companion object {
        const val SHORTAGE_TO_PROGRESS_MULT = 1f
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "${MPC_fractalCoreFactor.getFOB()?.name} shortages"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                tooltip.addPara(
                    "Without proper supply, the IAIIC is unable to properly conduct operations in your space, and secret " +
                        "benefactors may begin having second thoughts.",
                    0f
                )
                tooltip.addPara(
                    "Shortages can be caused by %s, %s, or %s. Note that the ${MPC_fractalCoreFactor.getFOB()?.name} has stockpiles, " +
                        "so it may be difficult to cause a shortage.",
                    5f,
                    Misc.getHighlightColor(),
                    "destroying incoming trade fleets", "reducing accessibility", "damaging inter-faction relations"
                )
            }
        }
    }

    override fun getProgress(intel: BaseEventIntel?): Int {
        return ceil((getShortagePoints() * SHORTAGE_TO_PROGRESS_MULT).toDouble()).toInt()
    }

    fun getShortagePoints(): Float {
        var points = 0f

        val market = MPC_fractalCoreFactor.getFOB() ?: return 0f
        for (demandedComm in market.demandData.demandList) {
            val data = market.getCommodityData(demandedComm.baseCommodity.id) ?: continue
            val deficit = (data.deficitQuantity / 1000f)
            points += deficit
        }

        return points
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean = true

    override fun getDescColor(intel: BaseEventIntel?): Color {
        if (getProgress(intel) <= 0) return Misc.getGrayColor() else return super.getDescColor(intel)
    }
}
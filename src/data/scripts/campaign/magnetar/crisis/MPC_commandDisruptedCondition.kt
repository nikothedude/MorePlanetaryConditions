package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_commandDisruptedCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val FLEET_SIZE_MULT = 0.4f
    }

    override fun apply(id: String) {
        super.apply(id)

        if (showIcon()) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(
                modId,
                FLEET_SIZE_MULT, name
            )
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(modId)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "Fleet size reduced by %s",
            10f,
            Misc.getNegativeHighlightColor(),
            "${FLEET_SIZE_MULT}x"
        )
        tooltip.addPara(
            "%s days left",
            10f,
            Misc.getHighlightColor(),
            "${MPC_IAIICFobIntel.get()?.disruptedCommandDaysLeft?.roundNumTo(1)?.trimHangingZero()}"
        )
        tooltip.addPara(
            "%s postponed",
            10f,
            Misc.getHighlightColor(),
            "inspections"
        )
    }

    override fun showIcon(): Boolean {
        val intel = MPC_IAIICFobIntel.get() ?: return false
        return intel.disruptedCommandDaysLeft > 0f
    }
}
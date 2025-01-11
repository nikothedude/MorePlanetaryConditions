package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import niko.MCTE.utils.MCTE_mathUtils.roundTo

class MPC_disarmedCondition: niko_MPC_baseNikoCondition() {
    override fun apply(id: String) {
        super.apply(id)

        if (showIcon()) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(
                modId,
                MPC_IAIICFobIntel.DISARMAMENT_FLEET_SIZE_MULT, name
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
            "${MPC_IAIICFobIntel.DISARMAMENT_FLEET_SIZE_MULT}x"
        )
        tooltip.addPara(
            "%s days left",
            10f,
            Misc.getHighlightColor(),
            "${MPC_IAIICFobIntel.get()?.disarmTimeLeft?.roundTo(1)}"
        )
        tooltip.addPara(
            "Days remaining reduced to at most %s if the %s is %s",
            10f,
            Misc.getHighlightColor(),
            "${MPC_IAIICFobIntel.DISARMAMENT_PREMATURE_DAYS}", "IAIIC", "provoked"
        )
    }

    override fun showIcon(): Boolean {
        val intel = MPC_IAIICFobIntel.get() ?: return false
        return intel.disarmTimeLeft > 0f
    }
}
package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_stringUtils

class MPC_benefactorCondition: niko_MPC_baseNikoCondition() {

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        val intel = MPC_IAIICFobIntel.get() ?: return
        val fleetMult = intel.getFleetMultFromContributingFactions()

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, fleetMult, name)
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        if (id == null) return

        val market = getMarket() ?: return
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return
        val intel = MPC_IAIICFobIntel.get() ?: return
        val fleetMult = intel.getFleetMultFromContributingFactions()

        tooltip.addPara(
            "Fleet size increased by %s, based on contributions by external factions",
            5f,
            Misc.getHighlightColor(),
            niko_MPC_stringUtils.toPercent(fleetMult)
        )
    }
}
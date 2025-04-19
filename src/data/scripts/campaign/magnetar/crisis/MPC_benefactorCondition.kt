package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.skills.MPC_spaceOperations
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_stringUtils
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_benefactorCondition: niko_MPC_baseNikoCondition() {

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        val intel = MPC_IAIICFobIntel.get() ?: return
        val escalationLevel = (1 + intel.escalationLevel)
        val fleetMult = (intel.getFleetMultFromContributingFactions() * escalationLevel) - 1f

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, fleetMult, name)
        market.upkeepMult.modifyMult(id, escalationLevel, "Escalation")
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        if (id == null) return

        val market = getMarket() ?: return
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
        market.upkeepMult.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return
        val intel = MPC_IAIICFobIntel.get() ?: return
        val fleetMult = intel.getFleetMultFromContributingFactions()

        val escalationLevel = (1 + intel.escalationLevel)
        tooltip.addPara(
            "Fleet size increased by %s, based on contributions from external factions",
            5f,
            Misc.getHighlightColor(),
            niko_MPC_stringUtils.toPercent(fleetMult - 1)
        )
        tooltip.addPara(
            "Fleet size contribution and market upkeep multiplied by %s due to escalation",
            5f,
            Misc.getHighlightColor(),
            "x${escalationLevel.roundNumTo(1).trimHangingZero()}"
        )
    }
}
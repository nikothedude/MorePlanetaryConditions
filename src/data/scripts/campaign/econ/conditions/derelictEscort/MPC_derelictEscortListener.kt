package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.utilities.niko_MPC_ids

class MPC_derelictEscortListener(
    var market: MarketAPI
): BaseCampaignEventListener(false) {
    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        if (fleet == null || to == null) return

        val containingLocation = market.containingLocation
        if (to != containingLocation) return

        val condition = getCondition() ?: return
        condition.tryToSpawnEscortOn(fleet, null)
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        if (fleet == null) return
        val location = fleet.containingLocation
        val containingLocation = market.containingLocation
        if (location != containingLocation) return

        val condition = getCondition() ?: return
        condition.tryToSpawnEscortOn(fleet, null)
    }

    private fun getCondition(): niko_MPC_derelictEscort? {
        return market.getCondition("niko_MPC_derelictEscort")?.plugin as? niko_MPC_derelictEscort
    }

    fun start() {
        if (Global.getSector().listenerManager.hasListener(this)) return
        Global.getSector().addTransientListener(this)
    }

    fun stop() {
        Global.getSector().removeListener(this)
    }
}
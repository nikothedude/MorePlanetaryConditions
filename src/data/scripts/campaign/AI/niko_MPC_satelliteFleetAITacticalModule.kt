package data.scripts.campaign.AI

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.campaign.ai.TacticalModule
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_satelliteUtils

class niko_MPC_satelliteFleetAITacticalModule(fleet: CampaignFleet?, ai: ModularFleetAIAPI?) : TacticalModule(fleet, ai) {

    val ourFleet = fleet

    override fun wantsToJoin(battle: BattleAPI?, considerPlayTransponderStatus: Boolean): Boolean {
        val handler: niko_MPC_satelliteHandlerCore = ourFleet?.getSatelliteEntityHandler() ?: return false
        val tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker() ?: return false
        if (battle == null) return false
        return (!tracker.areSatellitesInvolvedInBattle(battle, handler))
        // if our handler isnt influencing the battle, we can join
    }

}
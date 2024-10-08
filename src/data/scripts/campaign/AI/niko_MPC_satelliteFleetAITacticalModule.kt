package data.scripts.campaign.AI

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.campaign.ai.TacticalModule
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_satelliteUtils

class niko_MPC_satelliteFleetAITacticalModule(fleet: CampaignFleet?, ai: ModularFleetAIAPI?): TacticalModule(fleet, ai) {

    val ourFleet = fleet

    override fun advance(p0: Float) {
        super.advance(p0)
    }

    override fun wantsToJoin(battle: BattleAPI?, considerPlayTransponderStatus: Boolean): Boolean {
        val handler: niko_MPC_satelliteHandlerCore = ourFleet?.getSatelliteEntityHandler() ?: return false
        val tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker() ?: return false
        if (battle == null) return false
        return (!tracker.areSatellitesInvolvedInBattle(battle, handler))
        // if our handler isnt influencing the battle, we can join
    }

    override fun pickEncounterOption(
        context: FleetEncounterContextPlugin?,
        otherFleet: CampaignFleetAPI?,
        pureCheck: Boolean
    ): CampaignFleetAIAPI.EncounterOption {
        val satelliteFleet: CampaignFleet = ourFleet ?: return CampaignFleetAIAPI.EncounterOption.HOLD //todo: this fucking sucks
        val battle = satelliteFleet.battle
        if (battle != null) {
            var effectiveHostileStrength = 0f
            for (hostileFleet in battle.getOtherSideFor(satelliteFleet)) {
                effectiveHostileStrength += hostileFleet.effectiveStrength
            }
            return if (satelliteFleet.effectiveStrength < effectiveHostileStrength) {
                CampaignFleetAIAPI.EncounterOption.HOLD_VS_STRONGER
            } else CampaignFleetAIAPI.EncounterOption.HOLD
        } else if (otherFleet != null){
            if (satelliteFleet.effectiveStrength < otherFleet.effectiveStrength) {
                return CampaignFleetAIAPI.EncounterOption.HOLD_VS_STRONGER
            }
        }
        return CampaignFleetAIAPI.EncounterOption.HOLD
    }

    override fun pickEncounterOption(
        context: FleetEncounterContextPlugin?,
        otherFleet: CampaignFleetAPI?
    ): CampaignFleetAIAPI.EncounterOption {
        return pickEncounterOption(context, otherFleet, false)
    }

}
package data.scripts.campaign.AI

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.EncounterOption
import com.fs.starfarer.campaign.ai.ModularFleetAI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_debugUtils.assertEntityHasSatellites
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker

class niko_MPC_satelliteFleetAI(campaignFleet: CampaignFleet?) : ModularFleetAI(campaignFleet) {
    override fun wantsToJoin(battle: BattleAPI, considerPlayTransponderStatus: Boolean): Boolean {
        if (!assertEntityHasSatellites(fleet)) return true
        val handler: niko_MPC_satelliteHandlerCore = niko_MPC_satelliteUtils.getHandlerForCondition(fleet)
        val tracker = getSatelliteBattleTracker()
        return if (tracker.areSatellitesInvolvedInBattle(battle, handler)) {
            false
        } else true
    }

    override fun pickEncounterOption(
        context: FleetEncounterContextPlugin,
        otherFleet: CampaignFleetAPI,
        pureCheck: Boolean
    ): EncounterOption {
        val satelliteFleet: CampaignFleetAPI = fleet //todo: this fucking sucks
        val battle = satelliteFleet.battle
        if (battle != null) {
            var effectiveHostileStrength = 0f
            for (hostileFleet in battle.getOtherSideFor(satelliteFleet)) {
                effectiveHostileStrength += hostileFleet.effectiveStrength
            }
            return if (satelliteFleet.effectiveStrength < effectiveHostileStrength) {
                EncounterOption.HOLD_VS_STRONGER
            } else EncounterOption.HOLD
        } else {
            if (satelliteFleet.effectiveStrength < otherFleet.effectiveStrength) {
                return EncounterOption.HOLD_VS_STRONGER
            }
        }
        return EncounterOption.HOLD
    }

    override fun pickEncounterOption(
        context: FleetEncounterContextPlugin,
        otherFleet: CampaignFleetAPI
    ): EncounterOption {
        return pickEncounterOption(context, otherFleet, false)
    }
}
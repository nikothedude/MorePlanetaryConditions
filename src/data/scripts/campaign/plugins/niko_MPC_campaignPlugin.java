package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;
import org.lazywizard.lazylib.MathUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin { //todo: add to modplugin

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if (interactionTarget instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) interactionTarget;
            BattleAPI battle = fleet.getBattle();
            spawnSatelliteFleetsOnPlayerIfAble(fleet, battle);
        }
  /*      else if (niko_MPC_satelliteUtils.defenseSatellitesApplied(interactionTarget)) {
            if (niko_MPC_satelliteUtils.doEntitySatellitesWantToBlock(interactionTarget, playerFleet) && niko_MPC_satelliteUtils.areEntitySatellitesCapableOfBlocking(interactionTarget, playerFleet)) {
                Set<SectorEntityToken> entitiesWillingAndAbleToFight = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingAndCapableToFight(playerFleet);
                for (SectorEntityToken entity : entitiesWillingAndAbleToFight) {
                    niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
                    niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, playerFleet);
                }
            }
        } */
        return null;
    }

    private void spawnSatelliteFleetsOnPlayerIfAble(CampaignFleetAPI fleet, BattleAPI battle) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (battle != null) {
            HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoinBattle = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
            for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoinBattle.entrySet()) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entry.getKey());
                niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

                if (!tracker.areSatellitesInvolvedInBattle(battle, params)) {
                    CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, playerFleet); //todo: make it so that the fleets despawn if the dialog is exited and not engaged
                }
            }
        } else {
            HashMap<SectorEntityToken, CampaignFleetAPI> entitiesWillingToFight = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(fleet, playerFleet, fleet);
            for (Map.Entry<SectorEntityToken, CampaignFleetAPI> entry : entitiesWillingToFight.entrySet()) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entry.getKey());
                CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, playerFleet);
            }
        }
    }

    @Override
    public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) {

        for (CampaignFleetAPI satelliteFleet : battle.getBothSides()) {
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(satelliteFleet)) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(satelliteFleet);

                if (!MathUtils.isWithinRange(params.entity, satelliteFleet, params.satelliteInterferenceDistance)) {
                    niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);
                }
            }
        }
        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoin = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
        for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoin.entrySet()) {
            SectorEntityToken entity = entry.getKey();
            niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            if (!tracker.areSatellitesInvolvedInBattle(battle, params)) {
                CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, battle.computeCenterOfMass(), entity.getContainingLocation());
                if (!battle.join(satelliteFleet)) {
                    niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);
                }
            }
        }
        return null; //todo: hatred.
    }
}

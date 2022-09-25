package data.scripts.campaign.plugins;

import com.fs.starfarer.E.I;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.campaign.fleet.Battle;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_dialogUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;
import org.lazywizard.lazylib.MathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin { //todo: add to modplugin

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        BattleAPI battle = null;
        boolean spawnedFleets = false;
        if (interactionTarget instanceof CampaignFleetAPI) {
            CampaignFleetAPI fleet = (CampaignFleetAPI) interactionTarget;
            battle = fleet.getBattle();
            if (battle != null) {
                if (battle.isStationInvolved()) {
                    for (CampaignFleetAPI potentialStationFleet : battle.getStationSide()) {
                        if (potentialStationFleet.isStationMode()) {
                            spawnSatelliteFleetsOnPlayerIfAble(potentialStationFleet, battle, true);
                            spawnedFleets = true;
                        }
                    }
                }
            }

            if (!spawnedFleets) {
                spawnSatelliteFleetsOnPlayerIfAble(fleet, battle);
                spawnedFleets = true;
            }

            if (battle != null) {
                niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
                if (tracker.areAnySatellitesInvolvedInBattle(battle)) {
                    return new PluginPick<InteractionDialogPlugin>(new niko_MPC_satelliteInteractionDialogPlugin(), PickPriority.MOD_GENERAL);
                }
            }
        }
        else {
            SectorEntityToken dugUpEntity = niko_MPC_dialogUtils.digForSatellitesInEntity(interactionTarget);
            if (dugUpEntity != interactionTarget) {
                if (interactionTarget.hasTag(Tags.STATION)) {
                    MemoryAPI entityMemory = interactionTarget.getMemoryWithoutUpdate();
                    CampaignFleetAPI stationFleet = entityMemory.getFleet(MemFlags.STATION_FLEET);
                    if (stationFleet != null) {
                        battle = stationFleet.getBattle();
                        if (battle != null) {
                            spawnSatelliteFleetsOnPlayerIfAble(stationFleet, battle, true);
                        }
                    }
                }
            }
        }

        return null;
    }

    private void spawnSatelliteFleetsOnPlayerIfAble(CampaignFleetAPI fleet, BattleAPI battle) {
        spawnSatelliteFleetsOnPlayerIfAble(fleet, battle, false);
    }

    private void spawnSatelliteFleetsOnPlayerIfAble(CampaignFleetAPI fleet, BattleAPI battle, boolean isStationBattle) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        SectorEntityToken entityToAlwaysSpawnFleetsFrom = null;

        if (isStationBattle) {
            MemoryAPI stationFleetMemory = fleet.getMemoryWithoutUpdate();
            MarketAPI stationMarket = (MarketAPI) stationFleetMemory.get(MemFlags.STATION_MARKET);
            if (stationMarket != null) {
                SectorEntityToken stationEntity = stationMarket.getPrimaryEntity();
                if (stationEntity != null) {
                    entityToAlwaysSpawnFleetsFrom = stationEntity;
                }
            }
        }

        if (battle != null) {
            HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoinBattle = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
            if (entityToAlwaysSpawnFleetsFrom != null) {
                BattleAPI.BattleSide side = niko_MPC_satelliteUtils.getSideForSatellites(entityToAlwaysSpawnFleetsFrom, battle);
                if (side != BattleAPI.BattleSide.NO_JOIN) {
                    entitiesWillingToJoinBattle.put(entityToAlwaysSpawnFleetsFrom, side);
                }
            }
            for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoinBattle.entrySet()) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entry.getKey());
                niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

                if (!tracker.areSatellitesInvolvedInBattle(battle, params)) {
                    CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, playerFleet); //todo: make it so that the fleets despawn if the dialog is exited and not engaged
                }
            }
        } else {
            HashMap<SectorEntityToken, CampaignFleetAPI> entitiesWillingToFight = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(fleet, playerFleet, fleet);
            if (entityToAlwaysSpawnFleetsFrom != null) {
                CampaignFleetAPI chosenFleet = niko_MPC_satelliteUtils.getSideForSatellitesAgainstFleets(fleet, playerFleet, fleet, true);
                if (chosenFleet != null) entitiesWillingToFight.put(entityToAlwaysSpawnFleetsFrom, chosenFleet);
            }
            for (Map.Entry<SectorEntityToken, CampaignFleetAPI> entry : entitiesWillingToFight.entrySet()) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entry.getKey());
                CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, playerFleet);
            }
        }
    }

    @Override
    public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) { //imperfect, it seems this doesnt allow the spawned
        //fleets to engage in this autoresolve round

        for (CampaignFleetAPI satelliteFleet : battle.getBothSides()) {
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(satelliteFleet)) {
                niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(satelliteFleet);

                /*if (!MathUtils.isWithinRange(params.entity, satelliteFleet, params.satelliteInterferenceDistance)) {
                    niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);
                }*/
            }
        }
        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoin = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        if (battle.isStationInvolved()) {
            for (CampaignFleetAPI stationFleet : battle.getStationSide()) {
                if (stationFleet.isStationMode()) {
                    MarketAPI stationMarket = (MarketAPI) stationFleet.getMemoryWithoutUpdate().get(MemFlags.STATION_MARKET);
                    if (stationMarket != null) {
                        SectorEntityToken primaryEntity = stationMarket.getPrimaryEntity();
                        if (primaryEntity != null) {
                            niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(primaryEntity);
                            if (params != null && (!tracker.areSatellitesInvolvedInBattle(battle, params))) {
                                niko_MPC_fleetUtils.joinBattleWithNewSatellites(battle, params, primaryEntity);
                            }
                            break;
                        }
                    }
                }
            }
        }
        for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoin.entrySet()) {
            SectorEntityToken entity = entry.getKey();
            niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
            if (!tracker.areSatellitesInvolvedInBattle(battle, params)) {
                niko_MPC_fleetUtils.joinBattleWithNewSatellites(battle, params, params.entity);
            }
        }
        return null; //todo: hatred.
    }
}

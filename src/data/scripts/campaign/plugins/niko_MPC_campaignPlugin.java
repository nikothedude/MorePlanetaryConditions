package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin {

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        SectorEntityToken entityToExpandRadiusFrom = null;
        BattleAPI battle = null;
        boolean attempedToSpawnFleets = false;
        CampaignFleetAPI targetFleet = null;
        if (interactionTarget instanceof CampaignFleetAPI) {
            targetFleet = (CampaignFleetAPI) interactionTarget;
            battle = targetFleet.getBattle();
            if (battle != null) {
                if (battle.isStationInvolved()) { // the only times its important to check for a station fleet is if we're
                    // interacting with something fighting one since we can already get that info from interacting
                    // with a station
                    for (CampaignFleetAPI potentialStationFleet : battle.getStationSide()) {
                        if (potentialStationFleet.isStationMode()) {
                            SectorEntityToken primaryEntity = getStationHolderPrimaryEntity(potentialStationFleet);
                            if (primaryEntity != null) { //doesnt matter if we get a station or not, we got the primary entity
                                /*if (!stationIsItsOwnMarket(stationEntity)) { // if its its own market
                                    // then by god, we are 100% already in its interference distance
                                    // probably. so its fine, the satellites spawn already
                                    // so we dont need to set attemptedtospawnfleets to true*/

                                if (niko_MPC_satelliteUtils.defenseSatellitesApplied(primaryEntity)) {
                                    entityToExpandRadiusFrom = getStationPrimaryEntity(potentialStationFleet); //im leaving this for sanity
                                }
                                // since this returns the primaryentity of the market... we SHOULD be able to
                                // always know if its its own market or not? since it can be itself?
                                }
                            }
                        }
                    }
                }
            if (!attempedToSpawnFleets) {
                spawnSatelliteFleetsOnPlayerIfAble(targetFleet, battle, entityToExpandRadiusFrom);
                attempedToSpawnFleets = true;
            }
        }

        if (battle != null) {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            if (tracker.areAnySatellitesInvolvedInBattle(battle)) {
                return new PluginPick<InteractionDialogPlugin>(new niko_MPC_satelliteInteractionDialogPlugin(), PickPriority.MOD_GENERAL);
            }
        }
        else { // we did NOT end up interacting with any actual fleet
            SectorEntityToken dugUpEntity = niko_MPC_dialogUtils.digForSatellitesInEntity(interactionTarget);
            if (dugUpEntity != interactionTarget) {
                if (interactionTarget.hasTag(Tags.STATION)) { //we interacted with dugupentity's station
                    MemoryAPI entityMemory = interactionTarget.getMemoryWithoutUpdate();
                    targetFleet = entityMemory.getFleet(MemFlags.STATION_FLEET);
                    if (targetFleet != null) { //stationfleet can be null if it was defeated, i believe
                        battle = targetFleet.getBattle();
                    }
                    if (niko_MPC_satelliteUtils.defenseSatellitesApplied(dugUpEntity)) { // to avoid an error message
                        entityToExpandRadiusFrom = dugUpEntity; //lets make it so the station's entity knows to deploy satellites
                    }
                }
            }
            spawnSatelliteFleetsOnPlayerIfAble(targetFleet, battle, entityToExpandRadiusFrom); //if stationfleet is null, no point in trying to find any other fleet, just pass it as null
        }
        return null;
    }

    @Nullable
    private SectorEntityToken getStationPrimaryEntity(CampaignFleetAPI potentialStationFleet) {
        MemoryAPI fleetMemory = potentialStationFleet.getMemoryWithoutUpdate();
        MarketAPI fleetMarket = (MarketAPI) fleetMemory.get(MemFlags.STATION_MARKET);

        if (fleetMarket != null) {
            return fleetMarket.getPrimaryEntity();
        }
        return null;
    }

    @Nullable
    private SectorEntityToken getStationHolderPrimaryEntity(CampaignFleetAPI potentialStationFleet) {
        MemoryAPI stationFleetMemory = potentialStationFleet.getMemoryWithoutUpdate();
        MarketAPI stationMarket = (MarketAPI) stationFleetMemory.get(MemFlags.STATION_MARKET);
        if (stationMarket != null) {
            SectorEntityToken stationEntity = stationMarket.getPrimaryEntity();
            return stationEntity;
        } else {
            niko_MPC_debugUtils.displayError("how the FUCK did " + potentialStationFleet + "not have a market" +
                    " it is LITERALLY a station fleet.");
        }
        return null;
    }

    private boolean stationIsItsOwnMarket(SectorEntityToken station) {
        MemoryAPI stationMemory = station.getMemoryWithoutUpdate();
        CampaignFleetAPI stationFleet = stationMemory.getFleet(MemFlags.STATION_FLEET);
        SectorEntityToken orbitTarget = station.getOrbitFocus();
        if (stationFleet != null) {
            MarketAPI stationFleetMarket = (MarketAPI) stationFleet.getMemoryWithoutUpdate().get(MemFlags.STATION_MARKET);
            return stationFleetMarket != null && stationFleetMarket == station.getMarket();
        }
        else {
            if (orbitTarget != null && orbitTarget.getMarket() == station.getMarket()) {
                return true;
            }
        }
        return false;
    }


    private void spawnSatelliteFleetsOnPlayerIfAble(CampaignFleetAPI fleet, BattleAPI battle) {
        spawnSatelliteFleetsOnPlayerIfAble(fleet, battle, null);
    }

    /**
     *
     * @param fleet IMPORTANT. This is only checked in the context of the dummy fleet being hostile to it.
     * @param battle
     * @param entityToAlwaysTryToSpawnFrom
     * @return
     */
    private boolean spawnSatelliteFleetsOnPlayerIfAble(CampaignFleetAPI fleet, BattleAPI battle, SectorEntityToken entityToAlwaysTryToSpawnFrom) {
        boolean spawnedFleets = false;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        if (battle != null) {
            HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoinBattle = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
            if (entityToAlwaysTryToSpawnFrom != null) {
                BattleAPI.BattleSide side = niko_MPC_satelliteUtils.getSideForSatellites(entityToAlwaysTryToSpawnFrom, battle);
                if (niko_MPC_satelliteUtils.isSideValid(side)) {
                    entitiesWillingToJoinBattle.put(entityToAlwaysTryToSpawnFrom, side);
                }
            }
            for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoinBattle.entrySet()) {
                niko_MPC_satelliteHandler handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(entry.getKey());
                niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

                if (!tracker.areSatellitesInvolvedInBattle(battle, handler)) { // a bit of sanity for safety
                    CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleetForPlayerDialog(handler, playerFleet); //todo: make it so that the fleets despawn if the dialog is exited and not engaged
                    spawnedFleets = true;
                }
            }
        }
        else {
            SectorEntityToken locationToScanFrom = fleet;
            if (locationToScanFrom == null) {
                locationToScanFrom = playerFleet;
            }
            Set<SectorEntityToken> entitiesWithinRange = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellites(locationToScanFrom.getLocation(), locationToScanFrom.getContainingLocation());
            if (entityToAlwaysTryToSpawnFrom != null && niko_MPC_satelliteUtils.defenseSatellitesApplied(entityToAlwaysTryToSpawnFrom)) {
                CampaignFleetAPI chosenFleet = niko_MPC_satelliteUtils.getSideForSatellitesAgainstFleets(entityToAlwaysTryToSpawnFrom, playerFleet, fleet, true);
                if (chosenFleet != null) entitiesWithinRange.add(entityToAlwaysTryToSpawnFrom);
            }
            for (SectorEntityToken entity : entitiesWithinRange) {
                if (niko_MPC_satelliteUtils.areEntitySatellitesCapableOfBlocking(entity, playerFleet)) { // no want check because player might do a bit of trolling (raiding)
                    niko_MPC_satelliteHandler handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(entity);
                    CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleetForPlayerDialog(handler, playerFleet);
                    spawnedFleets = true;
                }
            }
        }
        return spawnedFleets;
    }

    /*@Override
    public PluginPick<BattleAutoresolverPlugin> pickBattleAutoresolverPlugin(BattleAPI battle) { //imperfect, it seems this doesnt allow the spawned
        //fleets to engage in this autoresolve round

        for (CampaignFleetAPI satelliteFleet : battle.getBothSides()) {
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(satelliteFleet)) {
                niko_MPC_satelliteHandler params = niko_MPC_satelliteUtils.getEntitySatelliteHandler(satelliteFleet);

                /*if (!MathUtils.isWithinRange(params.entity, satelliteFleet, params.satelliteInterferenceDistance)) {
                    niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);
                }
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
                            niko_MPC_satelliteHandler params = niko_MPC_satelliteUtils.getEntitySatelliteHandler(primaryEntity);
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
            niko_MPC_satelliteHandler params = niko_MPC_satelliteUtils.getEntitySatelliteHandler(entity);
            if (!tracker.areSatellitesInvolvedInBattle(battle, params)) {
                niko_MPC_fleetUtils.joinBattleWithNewSatellites(battle, params, params.entity);
            }
        }
        return null; //todo: hatred.
    } */
}

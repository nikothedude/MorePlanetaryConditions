package data.scripts.campaign.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore;
import data.utilities.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static data.utilities.niko_MPC_ids.niko_MPC_campaignPluginId;

public class niko_MPC_campaignPlugin extends BaseCampaignPlugin {

    public String getId() {
        return niko_MPC_campaignPluginId;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    /**
     * A hack that causes satellite fleets to spawn whenever the player interacts with anything. This is mandatory for
     * ensuring proper behavior in things like interacting with a fleet over jangala and having jangala's satellites appear
     * if you choose to fight it, or having jangala's satellites defend its station in a player-driven battle.
     * <p>
     * The station searching part of this method is necessary due to the way stations work. Since I always want satellites to interfere with a station battle
     * if the station is associated with the satellite planet, regardless of distance, I need to always find the station's
     * entity, E.G. either itself or the planet it orbits, so I can manually add it to the list of entities to try to spawn
     * satellites from, bypassing the distance check.
     *
     * @param interactionTarget If this is a fleet, checks to see if it has a battle. If it does, and if a station is involved in it,
     * scans every fleet on the side of the station to see if it's the station fleet. Once it finds it, it will grab the market
     * of the fleet, then return the primary entity of that market, which can, in 99% of cases, be either the planet
     * the station orbits, or the station itself, in the case of say, kantas den. 99% sure a getMarket() call doesn't
     * work here, as the fleet doesn't hold a ref to the market directly.
     * <p>
     * If it is NOT a fleet, we can assume it is either a station of a market, a market holder itself, or something mundane. In either
     * case, we know it's market is stored in getMarket(), so we get that, then get the result's getEntity(), which
     * may or may not be interactionTarget, or something else.
     *
     * @return An interactionplugin. Can either be null or a instance of satelliteInteractionDialogPlugin, depending on if
     * interactionTarget is engaged in a battle or not.
     */
    @Override
    @Nullable
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if (niko_MPC_satelliteUtils.isCustomEntitySatellite(interactionTarget)) return null;

        SectorEntityToken entityToExpandRadiusFrom = null; // this entity will always be checked to see if it should deploy satellites

        BattleAPI battle = null;
        CampaignFleetAPI targetFleet = null;

        if (interactionTarget instanceof CampaignFleetAPI) {
            targetFleet = (CampaignFleetAPI) interactionTarget; // we're interacting with a fleet
            battle = targetFleet.getBattle();
            if (battle != null) {
                if (battle.isStationInvolved()) { // the literal only situation in which we should try to dig for an entity
                    CampaignFleetAPI stationFleet = niko_MPC_battleUtils.getStationFleetOfBattle(battle);
                    // ^ this is necessary due to the fact that we still need to get the primaryentity of the station the
                    // battle has involved. we have no link to the station other than the station fleet which holds a ref to
                    // the market holding the station. the story is different if we interact with the station itself,
                    // which we handle in the else statement lower down in this method
                    if (stationFleet == null) { // this should never happen, but lets just be safe
                        niko_MPC_debugUtils.displayError("null stationfleet when expecting station, interaction target: " + interactionTarget.getName());
                        return null;
                    }
                    SectorEntityToken primaryEntity = getStationHolderPrimaryEntity(stationFleet);
                    if (primaryEntity != null) { //doesnt matter if we get a station or not, we got the primary entity

                        if (niko_MPC_satelliteUtils.defenseSatellitesApplied(primaryEntity)) {
                            entityToExpandRadiusFrom = getStationPrimaryEntity(stationFleet); //im leaving this for sanity
                        }
                        // since this returns the primaryentity of the market... we SHOULD be able to
                        // always know if its own market or not? since it can be itself?
                    }
                }
            }
        } else { // we're interacting with a non-fleet entity, meaning theres a chance it might be a station
            SectorEntityToken dugUpEntity = niko_MPC_dialogUtils.digForSatellitesInEntity(interactionTarget);
            if (dugUpEntity != interactionTarget) { // we're interacting with something that isn't the actual entity linked to the market
                if (interactionTarget.hasTag(Tags.STATION)) { //we interacted with dugupentity's station
                    // ^ means it must have a fleet, so let's get it
                    targetFleet = getStationFleetFromStation(interactionTarget);
                    // we're only getting this info to pass it to the spawn method
                    // you may ask "why pass it"? good question i havent thought it through but it works lol
                    if (targetFleet != null) { // we are almost guaranteed to get a non-null value, but, lets just be safe
                        battle = targetFleet.getBattle();
                    }
                }
                // regardless of if we interacted with a station or not, we still got something different
                if (niko_MPC_satelliteUtils.defenseSatellitesApplied(dugUpEntity)) { // to avoid an error message
                    entityToExpandRadiusFrom = dugUpEntity; //lets make it so the station's entity knows to deploy satellites
                }
            }
        }

        spawnSatelliteFleetsOnPlayerIfAble(targetFleet, battle, entityToExpandRadiusFrom);

        if (battle != null) {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            if (tracker.areAnySatellitesInvolvedInBattle(battle)) {
                return new PluginPick<InteractionDialogPlugin>(new niko_MPC_satelliteInteractionDialogPlugin(), PickPriority.MOD_GENERAL);
                // general priority because i want it to be overridable easily
            }
        }

        return null;
    }

    @Nullable
    private CampaignFleetAPI getStationFleetFromStation(@NotNull SectorEntityToken station) {
        MemoryAPI stationMemory = station.getMemoryWithoutUpdate();
        CampaignFleetAPI fleet = stationMemory.getFleet(MemFlags.STATION_FLEET);
        if (fleet == null) {
            fleet = stationMemory.getFleet(MemFlags.STATION_BASE_FLEET);
        }
        // it seems that if the station fleet is destroyed, the station base fleet takes its place
        return fleet;
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
    private List<CampaignFleetAPI> spawnSatelliteFleetsOnPlayerIfAble(@Nullable CampaignFleetAPI fleet, @Nullable BattleAPI battle,
                                                                      @Nullable SectorEntityToken entityToAlwaysTryToSpawnFrom) {

        // we only spawn things for PROTECTION here not ATTACK. this means we want these fleets to be NEAR the entity,
        // but not actively attacking, this is so that if a friendly player decides to attack the market, they will
        // still have to go through the satellites

        //tldr this method exists so if the player enters a battle the satellites will interfere

        List<CampaignFleetAPI> spawnedFleets;

        if (battle != null) {
            spawnedFleets = spawnSatelliteFleetsForBattle(battle, entityToAlwaysTryToSpawnFrom);
        }
        else {
            spawnedFleets = spawnSatelliteFleetsForFleet(fleet, entityToAlwaysTryToSpawnFrom);
        }
        return spawnedFleets;
    }

    @NotNull
    private List<CampaignFleetAPI> spawnSatelliteFleetsForFleet(@Nullable CampaignFleetAPI fleet, @Nullable SectorEntityToken entityToAlwaysTryToSpawnFrom) {

        List<CampaignFleetAPI> spawnedFleets = new ArrayList<>();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

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
                niko_MPC_satelliteHandlerCore handler = niko_MPC_satelliteUtils.getHandlerForCondition(entity);
                CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.getHandlerDialogFleet(handler, playerFleet);
                spawnedFleets.add(satelliteFleet);
            }
        }
        return spawnedFleets;
    }

    @NotNull
    private List<CampaignFleetAPI> spawnSatelliteFleetsForBattle(@NotNull BattleAPI battle, @Nullable SectorEntityToken entityToAlwaysTryToSpawnFrom) {
        List<CampaignFleetAPI> spawnedFleets = new ArrayList<>();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToJoinBattle = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
        // any entity thats already influencing the battle wont spawn anything

        if (entityToAlwaysTryToSpawnFrom != null) {
            BattleAPI.BattleSide side = niko_MPC_satelliteUtils.getSideForSatellites(entityToAlwaysTryToSpawnFrom, battle);
            if (niko_MPC_satelliteUtils.isSideValid(side)) {
                // same story here, any entity that is already influencing return NO_SIDE
                entitiesWillingToJoinBattle.put(entityToAlwaysTryToSpawnFrom, side);
            }
        }
        for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entitiesWillingToJoinBattle.entrySet()) {
            niko_MPC_satelliteHandlerCore handler = niko_MPC_satelliteUtils.getHandlerForCondition(entry.getKey());
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

            if (!tracker.areSatellitesInvolvedInBattle(battle, handler)) { // a bit of sanity for safety
                CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.getHandlerDialogFleet(handler, playerFleet);
                spawnedFleets.add(satelliteFleet);
            }
        }
        return spawnedFleets;
    }


    @Nullable
    private SectorEntityToken getStationPrimaryEntity(@NotNull CampaignFleetAPI potentialStationFleet) {
        MemoryAPI fleetMemory = potentialStationFleet.getMemoryWithoutUpdate();
        MarketAPI fleetMarket = (MarketAPI) fleetMemory.get(MemFlags.STATION_MARKET);

        if (fleetMarket != null) {
            return fleetMarket.getPrimaryEntity();
        }
        return null;
    }

    @Nullable
    private SectorEntityToken getStationHolderPrimaryEntity(@NotNull CampaignFleetAPI potentialStationFleet) {
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

    private boolean stationIsItsOwnMarket(@NotNull SectorEntityToken station) {
        MemoryAPI stationMemory = station.getMemoryWithoutUpdate();
        CampaignFleetAPI stationFleet = getStationFleetFromStation(station);
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
}

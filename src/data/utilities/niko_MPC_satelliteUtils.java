package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_ids.*;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;
import static data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset;
import static java.lang.Math.round;

public class niko_MPC_satelliteUtils {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    /**
     * If the tracker is null, creates a new one.
     * @return The savefile specific instance of the battle tracker.
     */
    public static niko_MPC_satelliteBattleTracker getSatelliteBattleTracker() {
        niko_MPC_satelliteBattleTracker tracker = (niko_MPC_satelliteBattleTracker) Global.getSector().getMemory().get(niko_MPC_ids.satelliteBattleTrackerId);
        if (tracker == null) {
            tracker = (niko_MPC_memoryUtils.createNewSatelliteTracker());
        }
        return tracker;
    }

    //////////////////////////
    //                      //
    // ADDITION AND REMOVAL //
    //                      //
    //////////////////////////

    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, niko_MPC_satelliteHandler handler) {
        initializeSatellitesOntoEntity(entity, entity.getMarket(), handler);
    }

    /**
     * This is what should be called the FIRST TIME an entity gains satellites, or after satellites have been entirely removed.
     * @param entity The entity to add markets to.
     * @param market Will have satelliteMarketId set to this if not null.
     */
    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, MarketAPI market, niko_MPC_satelliteHandler handler) {
        if (!niko_MPC_debugUtils.doEntityHasNoSatellitesTest(entity)) { //if the test fails, something fucked up, lets abort
            return;
        }
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        if (market != null) {
            entityMemory.set(satelliteMarketId, market); // we're already protected from overwriting satellites with the above test
        }
        entityMemory.set(satelliteHandlerId, handler); //store our parameters onto the entity
        //LocationAPI containingLocation = entity.getContainingLocation();
        /*SectorEntityToken barrageTerrain = containingLocation.addTerrain(satelliteBarrageTerrainId, new niko_MPC_defenseSatelliteBarrageTerrainPlugin.barrageAreahandler(
            handler.satelliteBarrageDistance,
            "Bombardment zone",
            entity

        ));
        handler.satelliteBarrages.add(barrageTerrain);
        barrageTerrain.setCircularOrbit(entity, 0, 0, 100); */
        addSatellitesUpToMax(entity);
    }

    /**
     * Adds a new CustomCampaignEntity satellite of type id to entity and sets up an orbit around it.
     * @param entity The entity for the satellite to orbit around.
     * @param regenerateOrbit If true, repositions all satellites in orbit with the same ratio
     * of distance to eachother.
     * @param id The Id of the satellite to add.
     * @param factionId The faction id to set as the satellite's faction.
     */
    public static void addSatellite(SectorEntityToken entity, boolean regenerateOrbit, String id, String factionId) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.addSatellite(regenerateOrbit, id, factionId);
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to market through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param entity The entity to add the satellites to.
     * @param amountOfSatellitesToAdd The amount of satellites.
     */
    public static void addSatellitesToEntity(SectorEntityToken entity, int amountOfSatellitesToAdd) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        addSatellitesToEntity(entity, amountOfSatellitesToAdd, handler.getParams().satelliteId, handler.getParams().satelliteFactionId);
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to entity through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id                      The id to be assigned to the satellites.
     * @param faction                 The factionid to be given to the satellites.
     */
    public static void addSatellitesToEntity(SectorEntityToken entity, int amountOfSatellitesToAdd, String id, String faction) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.addSatellitesToEntity(amountOfSatellitesToAdd, id, faction);
    }

    // all this method should do is call addsatellites with the max shit
    public static void addSatellitesUpToMax(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.addSatellitesUpToMax();
    }

    /**
     * Not only removes all satellites from entity, but also removes all objects and entities associated
     * with the existence of the satellites. This should only be used on removal of EVERYTHING.
     * @param entity
     */
    public static void purgeSatellitesFromEntity(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        removeSatellitesFromEntity(entity);
        deleteMemoryKey(entityMemory, satelliteMarketId);

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        for (SectorEntityToken terrain : handler.satelliteBarrages) { //todo: shouldnt cause issues if there is no terrain?
            removeSatelliteBarrageTerrain(entity, terrain);
        }

        for (CampaignFleetAPI satelliteFleet : handler.satelliteFleets) {
            niko_MPC_fleetUtils.despawnSatelliteFleet(satelliteFleet);
        }

        handler.prepareForGarbageCollection();
        deleteMemoryKey(entityMemory, satelliteHandlerId);
    }

    //todo: migrate to handler
    public static int getMaxBattleSatellites(SectorEntityToken primaryEntity) {
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(primaryEntity);
        if (handler != null) return handler.getMaxBattleSatellites();
        return 3;
    }

    /**
     * Removes all satellites orbiting this entity.
     *
     * @param entity The target entity.
     */
    public static void removeSatellitesFromEntity(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfEntity(entity);
        removeSatellitesFromEntity(entity, satellitesInOrbit.size());
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from entity's orbit. Will end execution early if the list becomes empty.
     *
     * @param entity                     The entity to remove the satellite from.
     * @param amountOfSatellitesToRemove The amount of satellites to remove from entity.
     */
    public static void removeSatellitesFromEntity(SectorEntityToken entity, int amountOfSatellitesToRemove) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.removeSatellitesFromEntity(amountOfSatellitesToRemove);
    }

    /**
     * Removes a specific satellite from an entity.
     * @param entity The entity to remove the satellite from.
     * @param satellite The satellite to remove.
     * @param regenerateOrbit If true, satellite orbit will be regenerated.
     */
    public static void removeSatelliteFromEntity(SectorEntityToken entity, CustomCampaignEntityAPI satellite, boolean regenerateOrbit) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.removeSatellite(satellite, regenerateOrbit, true);
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     *
     * @param satellite The satellite to remove.
     */
    public static void removeSatellite(CustomCampaignEntityAPI satellite) {
        Misc.fadeAndExpire(satellite);
        satellite.getContainingLocation().removeEntity(satellite);
    }

    /**
     * Places all satellites in orbit around the given entity, ensuring they are all equally spaced apart from eachother.
     * @param entity The entity to regenerate satellite orbit from.
     */
    public static void regenerateOrbitSpacing(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.regenerateOrbitSpacing();
    }

    // MARKETS

    public static boolean marketsDesynced(SectorEntityToken entity) {
        return marketsDesynced(entity, entity.getMarket());
    }

    /**
     * @param entity The entity to check.
     * @param market The market to compare entity's market to.
     * @return True if entity's satellite market != market.
     */
    public static boolean marketsDesynced(SectorEntityToken entity, MarketAPI market) {
        return (memorySatellitesDesyncedWithMarket(entity.getMemoryWithoutUpdate(), market));
    }

    /**
     * @param memory The memoryAPI to check.
     * @param market The market to compare memory's market to.
     * @return True if memory's satellite market != market.
     */
    public static boolean memorySatellitesDesyncedWithMarket(MemoryAPI memory, MarketAPI market) {
        return (getMemorySatelliteMarket(memory) != market);
    }

    /**
     * Sets entity's satellitemarket to market.
     */
    public static void syncMarket(SectorEntityToken entity, MarketAPI market) {
        entity.getMemoryWithoutUpdate().set(satelliteMarketId, market);
    }

    //GETTERS AND SETTERS

    /**
     * @return Either null, or an instance of niko_MPC_satelliteHandler.
     */
    // no nullable, i use a method to nullcheck which intellij gets confused by
    public static niko_MPC_satelliteHandler getEntitySatelliteHandler(SectorEntityToken entity) {
        return (niko_MPC_satelliteHandler) entity.getMemoryWithoutUpdate().get(satelliteHandlerId);
    }

    /**
     * @return Either null, or an instance of niko_MPC_satellitehandler.
     */
    public static niko_MPC_satelliteHandler.niko_MPC_satelliteParams getEntitySatelliteParams(SectorEntityToken entity) {
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        if (handler != null) return handler.getParams();
        return null;
    }

    /**
     * Returns the "satellite market" of an entity, or the market that holds the condition. Can be null.
     * @param entity The entity to check.
     * @return The satellite market of the entity.
     */
    public static MarketAPI getEntitySatelliteMarket(SectorEntityToken entity) {
        return (getMemorySatelliteMarket(entity.getMemoryWithoutUpdate()));
    }

    public static MarketAPI getMemorySatelliteMarket(MemoryAPI memory) {
        return (MarketAPI) memory.get(satelliteMarketId);
    }

    /**
     * Gets a list of satellites in orbit around entity, using entity's satellite handler.
     * @param entity The entity of which the satellites are orbitting.
     * @return A arraylist of satellite in orbit around the entity. Can return an empty list.
     */
    public static List<CustomCampaignEntityAPI> getSatellitesInOrbitOfEntity(SectorEntityToken entity) {
        //does not call ensureSatellites because this is intended to be called on things w/o satellites
        
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        if (handler != null) {
            return handler.getSatellites();
        }
        return new ArrayList<>();
    }

    public static String getCurrentSatelliteFactionId(niko_MPC_satelliteHandler handler) {
        return getCurrentSatelliteFactionId(handler.entity);
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the entity's faction id whenever it's ran.
     * @param entity The entity to get the handler from.
     * @return A faction ID, in string form. Can return null if entity has no satellites.
     */
    @Nullable
    public static String getCurrentSatelliteFactionId(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        return handler.getCurrentSatelliteFactionId();
    }
    public static FactionAPI getCurrentSatelliteFaction(niko_MPC_satelliteHandler handler) {
        return Global.getSector().getFaction(getCurrentSatelliteFactionId(handler));
    }

    /**
    * Generates an offset with which satellites in orbit of an entity will be spaced apart by. 
    * Is based on the amount of satellites in the given list.
    * @param satellitesInOrbitOfEntity The list of satellites to use. Should be a complete list of satellites
    * in orbit of an entity.
    * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
    */
    public static float getOptimalOrbitalOffsetForSatellites(List<CustomCampaignEntityAPI> satellitesInOrbitOfEntity) {
        int numOfSatellites = satellitesInOrbitOfEntity.size();

        float optimalAngle = (360 / (float) numOfSatellites);
        // 1 satellite = offset of 360, so none. 2 satellites = offset or 180, so they are on opposite ends of the planet.
        // 3 satellites = offset of 120, meaning the satellites form a triangle around the entity. Etc.

        if (optimalAngle == 360) {
            optimalAngle = 0; //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle;
    }

    /**
     * Generates an offset with which satellites in orbit of an entity will be spaced apart by.
     * Is based on the amount of satellites in orbit of the planet.
     * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
     */
    public static float getOptimalOrbitalOffsetForSatellites(SectorEntityToken entity) {
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        if (handler != null) return handler.getOptimalOrbitalOffsetForSatellites();
        return getOptimalOrbitalOffsetForSatellites(new ArrayList<CustomCampaignEntityAPI>());
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity) {
        return getMaxPhysicalSatellitesBasedOnEntitySize(entity, 5);
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity, float radiusDivisor) {
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        if (handler != null) return handler.getMaxPhysicalSatellitesBasedOnEntitySize(radiusDivisor);
        return ((round((entity.getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number;
    }

    // GETTING ENTITIES AND CHECKING CAPABILITIES

    /**
     * @param location The location to scan.
     * @return A new set containing every entity that had defenseSatellitesApplied(entity) return true.
     */
    public static Set<SectorEntityToken> getEntitiesInLocationWithSatellites(LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = new HashSet<>();
        for (SectorEntityToken entity : location.getAllEntities()) {
            if (entity instanceof CampaignFleetAPI) {
                CampaignFleetAPI possibleSatelliteFleet = (CampaignFleetAPI) entity;
                if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(possibleSatelliteFleet)) continue; // if we dont do this, we create an infinite loop of
                // spawning fleets to check the f leets which then check those fleets which then check those fleets
                // does not account for the chance satellites have satellites themselves. if i ever do that, ill make a memkey for it or smthn
            }
            if (defenseSatellitesApplied(entity)) {
             entitiesWithSatellites.add(entity);
            }
        }
        return entitiesWithSatellites;
    }

    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellites(SectorEntityToken entity, LocationAPI location) {
        return getNearbyEntitiesWithSatellites(entity.getLocation(), location);
    }

    /**
     * First gets a list of entities in location with satellites, then does a range check using the entity's handler' satelliteInterferenceDistance variable.
     * @param coordinates The coordinates to compare the entity to.
     * @param location The location to scan.
     * @return A set containing every entity with satellites that has the coordinates within their interference distance.
     */
    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellites(Vector2f coordinates, LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = getEntitiesInLocationWithSatellites(location);
        Iterator<SectorEntityToken> iterator = entitiesWithSatellites.iterator();
        
        // a ensureSatellites check is not needed as the set only has entities with handler

        while (iterator.hasNext()) {
            SectorEntityToken entity = iterator.next();
            niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity); //we can use this here because the previously used method only returns things with handler
            if (!MathUtils.isWithinRange(entity, coordinates, handler.getSatelliteInterferenceDistance())) {
                iterator.remove(); //have to remove because we're using a full list already
            }
        }
        return entitiesWithSatellites;
    }

    public static Set<SectorEntityToken> getEntitiesWithSatellitesCapableOfFighting(Set<SectorEntityToken> entities) {
        return entities; //todo: unfinished
    }

    /**
     * @param fleet The fleet to check.
     * @return A list of entities with satellites that are willing to fight the fleet, using doEntitySatellitesWantToFight(entity, fleet).
     */
    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellitesWillingToFight(CampaignFleetAPI fleet) {
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(fleet.getLocation(), fleet.getContainingLocation());
        Iterator<SectorEntityToken> iterator = entitiesWithSatellites.iterator();

        while (iterator.hasNext()) {
            SectorEntityToken entity = iterator.next();
            if (!doEntitySatellitesWantToFight(entity, fleet)) {
                iterator.remove();
            }
        }
        return entitiesWithSatellites;
    }

    /**
     * Spawns a temporary fleet that the battle uses to check the side it would join. If the side is not NO_JOIN, adds the entity
     * to the hashmap, associated with the side it'd join.
     * @param battle The battle to check.
     * @return A hashmap of entities associated with the side of the battle they'd take.
     */
    public static HashMap<SectorEntityToken, BattleAPI.BattleSide> getNearbyEntitiesWithSatellitesWillingToJoinBattle(BattleAPI battle) {
        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToFight = new HashMap<>();
        LocationAPI containingLocation = null;
        if (battle.isPlayerInvolved()) {
            containingLocation = Global.getSector().getPlayerFleet().getContainingLocation();
        }
        else {
            for (CampaignFleetAPI fleet : battle.getBothSides()) { //have to do this, because some fleet dont HAVE a containing location
                if (fleet.getContainingLocation() != null) { //ideally, this will only iterate once or twice before finding a location
                    containingLocation = fleet.getContainingLocation();
                    break; //we found a location, no need to check everyone else
                }
            }
        }

        if (containingLocation == null) {
            if (Global.getSettings().isDevMode()) {
                niko_MPC_debugUtils.displayError("nearbyEntitiesWillingToJoinBattle null containing location");
            }
            return entitiesWillingToFight; // in truth, if there is no containing location, then there would be no entities in range anyway
        }

        Vector2f coordinates = battle.computeCenterOfMass();

        for (SectorEntityToken entity : getNearbyEntitiesWithSatellites(coordinates, containingLocation)) {
            BattleAPI.BattleSide battleSide = getSideForSatellites(entity, battle);

            if (battleSide != BattleAPI.BattleSide.NO_JOIN) { //the entity doesnt want to join if its NO_JOIN
                entitiesWillingToFight.put(entity, battleSide);
            }
        }
        return entitiesWillingToFight;
    }

    /**
    * Uses doEntitySatellitesWantToBlock/Fight and areEntitySatellitesCapableOfFBlocking/Fighting to determine
    * which fleets the satellites would want to fight when spawned.
    * @param entity The entity to get the handler from.
    * @param fleet The first fleet to check.
    * @param fleetTwo The second fleet to check.
    * @param capabilityCheck If true, runs an additional check that skips over a fleet if areEntitySatellitesCapableOfBlocking returns false.
    * @return Null if the satellites want to fight both or neither, otherwise, returns which of the two fleets they're willing to fight.
    */
    public static CampaignFleetAPI getSideForSatellitesAgainstFleets(SectorEntityToken entity, CampaignFleetAPI fleet, CampaignFleetAPI fleetTwo, boolean capabilityCheck) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);

        return handler.getSideForSatellitesAgainstFleets(fleet, fleetTwo, capabilityCheck);
    }

    /**
    * Gets all entities within range of entity and gets which fleet they'd fight using getsideforsatellitesagainstfleets. 
    * @param entity The entity to get coordinates from.
    * @param fleet The first fleet to check.
    * @param otherFleet The second fleet to check.
    * @return A new HashMap in format (entity -> fleet), where entity is the entity willing to fight, and fleet is which fleet they chose. Cannot return nulls.
    */
    public static HashMap<SectorEntityToken, CampaignFleetAPI> getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(SectorEntityToken entity, CampaignFleetAPI fleet, CampaignFleetAPI otherFleet) {
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(entity.getLocation(), entity.getContainingLocation());

        HashMap<SectorEntityToken, CampaignFleetAPI> sidesTaken = new HashMap<>();

        for (SectorEntityToken entityWithSatellite : entitiesWithSatellites) {
            CampaignFleetAPI fleetChosen = getSideForSatellitesAgainstFleets(entityWithSatellite, fleet, otherFleet, true);
            if (fleetChosen != null) {
                sidesTaken.put(entityWithSatellite, fleetChosen);
            }
        }
        return sidesTaken;
    }

    /**
     * Gets the side the dummy fleet of entity's handler would enter.
     * @param entity The entity to get the satellites from.
     * @param battle The battle to get the side for.
     * @return The battleside that entity's satellites would pick. Can return null if the entity has no satellites.
     */
    public static BattleAPI.BattleSide getSideForSatellites(SectorEntityToken entity, BattleAPI battle) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        return handler.getSideForBattle(battle);
    }

    /**
     * @param battle The battle to check.
     * @return A hashmap of nearby entities with satellites that are both willing and capable of joining battle.
     */
    public static HashMap<SectorEntityToken, BattleAPI.BattleSide> getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(BattleAPI battle) {
        return getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
    }

    /**
     * todo: expand this method to have more args
     * @param side The side to use.
     * @param entityMap The hashmap of entities to battleside.
     * @return A new arraylist of entities on the given side.
     */
    public static List<SectorEntityToken> getEntitiesOnSide(BattleAPI.BattleSide side, HashMap<SectorEntityToken, BattleAPI.BattleSide> entityMap) {
        List<SectorEntityToken> entitiesOnSide = new ArrayList<>();

        for (Map.Entry<SectorEntityToken, BattleAPI.BattleSide> entry : entityMap.entrySet()) {
            SectorEntityToken entity = entry.getKey();
            BattleAPI.BattleSide battleSide = entry.getValue();
            if (battleSide == side) {
                entitiesOnSide.add(entity);
            }
        }
        return entitiesOnSide;
    }

    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellitesWillingAndCapableToFight(CampaignFleetAPI fleet) {
        return getEntitiesWithSatellitesCapableOfFighting(getNearbyEntitiesWithSatellitesWillingToFight(fleet));
    }

    // CAPABILITY CHECKING

    public static boolean doEntitySatellitesWantToFight(niko_MPC_satelliteHandler handler, CampaignFleetAPI fleet) {
        return doEntitySatellitesWantToFight(handler.entity, fleet);
    }

    /**
     * Used for generating battles and autoresolve and such.
     * @param entity The entity to get the satellite handler from.
     * @param fleet The fleet to check.
     * @return True, if the handler' dummy fleet is hostile to the given fleet. False otherwise. Can return null
     * if entity has no satellites.
     */
    public static boolean doEntitySatellitesWantToFight(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);

        return handler.doSatellitesWantToFight(fleet);
    }

    /**
     * Used for things such as preventing the player from interacting with a market.
     * @param entity The entity to get the satellite handler from.
     * @param fleet The fleet to check.
     * @return True, if the satellite handler' faction is inhospitable or worse to fleets' faction, if the fleet has no transponder,
     * or if the satellites want to fight.
     * Can return false if handler are null.
     */
    public static boolean doEntitySatellitesWantToBlock(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);

        return handler.doSatellitesWantToBlock(fleet);
    }

    /**
     * @param entity The entity to check.
     * @return True if the entity isn't already blocking the fleet, or if entity's satellite handler' grace period is
     * less or equal to 0. False otherwise.
     */
    public static boolean areEntitySatellitesCapableOfBlocking(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        return handler.areSatellitesCapableOfBlocking(fleet);
    }

    /**
    * Unfinished.
    */
    private static boolean areEntitySatellitesCapableOfFighting(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);

        return handler.areSatellitesCapableOfFighting(fleet);
    }


    /**
    * Forces the given entity's satellite handler to spawn a full satellite fleet on the target, unless
    * it's already fighting them. Can fail if the entity has no handler.
    * @param entity The entity to get handler from.
    * @param fleet The fleet to check and engage.
    */
    public static void makeEntitySatellitesEngageFleet(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;

        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);
        handler.makeEntitySatellitesEngageFleet(fleet);
    }

    /**
    * Forces all nearby entities out of getNearbyEntitiesWithSatellitesWillingAndCapableToFight to
    * run makeEntitySatellitesEngageFleet.
    * @param fleet The fleet to force the entities to engage.
    */
    public static void makeNearbyEntitySatellitesEngageFleet(CampaignFleetAPI fleet) {
        for (SectorEntityToken entity : getNearbyEntitiesWithSatellitesWillingAndCapableToFight(fleet)) {
            makeEntitySatellitesEngageFleet(entity, fleet);
        }
    }

    //MISC

    /**
    * @param entity The entity to check.
    * @return true if getEntitySatellitehandler(entity) is not null.
    */
    public static boolean defenseSatellitesApplied(SectorEntityToken entity) {
        return getEntitySatelliteHandler(entity) != null;
    }

    @Deprecated
    public static void removeSatelliteBarrageTerrain(SectorEntityToken relatedEntity, SectorEntityToken terrain) {
        LocationAPI containingLocation = relatedEntity.getContainingLocation();

        terrain.setExpired(true);
        containingLocation.removeEntity(terrain);
    }

    /**
    * Runs handler.adjustGracePeriod(fleet, amount). Exists so that we add a new grace decrementer if none exists.
    @param fleet The fleet to adjust the grace period of.
    @param amount The amount to adjust the grace by.
    @param entity The entity to get handler from.
    */
    public static void incrementSatelliteGracePeriod(CampaignFleetAPI fleet, float amount, SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        niko_MPC_satelliteHandler handler = getEntitySatelliteHandler(entity);

        if (!entity.hasScriptOfClass(niko_MPC_gracePeriodDecrementer.class)) {
            niko_MPC_gracePeriodDecrementer decrementerScript = new niko_MPC_gracePeriodDecrementer(handler);
            entity.addScript(decrementerScript);
            handler.gracePeriodDecrementer = decrementerScript;
        }
        handler.adjustGracePeriod(fleet, amount);
    }

    public static List<SectorEntityToken> getAllDefenseSatellitePlanets() {
        List<SectorEntityToken> entitiesWithSatellites = new ArrayList<>();
        List<StarSystemAPI> systems = Global.getSector().getStarSystems();
        for (StarSystemAPI system : systems) {
            for (SectorEntityToken entity : system.getAllEntities()) {
                if (defenseSatellitesApplied(entity)) {
                    entitiesWithSatellites.add(entity);
                    continue;
                }
            }
        }
    return  entitiesWithSatellites;
    }
}
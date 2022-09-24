package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_debugUtils.logEntityData;
import static data.utilities.niko_MPC_fleetUtils.createSatelliteFleetTemplate;
import static data.utilities.niko_MPC_ids.satelliteMarketId;
import static data.utilities.niko_MPC_ids.satelliteParamsId;
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

    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, niko_MPC_satelliteParams params) {
        initializeSatellitesOntoEntity(entity, entity.getMarket(), params);
    }

    /**
     * This is what should be called the FIRST TIME an entity gains satellites, or after satellites have been entirely removed.
     * @param entity The entity to add markets to.
     * @param market Will have satelliteMarketId set to this if not null.
     */
    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, MarketAPI market, niko_MPC_satelliteParams params) {
        if (!niko_MPC_debugUtils.doEntityHasNoSatellitesTest(entity)) { //if the test fails, something fucked up, lets abort
            return;
        }
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        if (market != null) {
            entityMemory.set(satelliteMarketId, market); // we're already protected from overwriting satellites with the above test
        }
        entityMemory.set(satelliteParamsId, params); //store our parameters onto the entity
        //LocationAPI containingLocation = entity.getContainingLocation();
        /*SectorEntityToken barrageTerrain = containingLocation.addTerrain(satelliteBarrageTerrainId, new niko_MPC_defenseSatelliteBarrageTerrainPlugin.barrageAreaParams(
            params.satelliteBarrageDistance,
            "Bombardment zone",
            entity

        ));
        params.satelliteBarrages.add(barrageTerrain);
        barrageTerrain.setCircularOrbit(entity, 0, 0, 100); */
        addSatellitesUpToMax(entity);
    }

    /**
     * todo
     * @param entity
     * @param regenerateOrbit
     * @param id
     * @param factionId
     */
    public static void addSatellite(SectorEntityToken entity, boolean regenerateOrbit, String id, String factionId) {
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfEntity(entity); //first, we get the satellites

        int satelliteNumber = ((satellitesInOrbit.size()) + 1);
        String orderedId = (id + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc

        LocationAPI containingLocation = entity.getContainingLocation();
        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = containingLocation.addCustomEntity(orderedId, null, id, factionId);

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        addOrbitPointingDownWithRelativeOffset(satellite, entity, 0, params.satelliteOrbitDistance); //set up the orbit

        satellitesInOrbit.add(satellite); //now add the satellite to the params' list

        if (regenerateOrbit)
            regenerateOrbitSpacing(entity); //and set up the orbital angles
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to market through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param entity
     * @param amountOfSatellitesToAdd The amount of satellites.
     */
    public static void addSatellitesToEntity(SectorEntityToken entity, int amountOfSatellitesToAdd) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        addSatellitesToEntity(entity, amountOfSatellitesToAdd, params.satelliteId, params.satelliteFactionId);
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to market through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id                      The id to be assigned to the satellites.
     * @param faction                 The factionid to be given to the satellites.
     */
    public static void addSatellitesToEntity(SectorEntityToken entity, int amountOfSatellitesToAdd, String id, String faction) {
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(entity, false, id, faction);
        }
        regenerateOrbitSpacing(entity); //only needs to be done once, after all the satellites are added, this does not generate the error
    }

    // all this method should do is call addsatellites with the max shit
    public static void addSatellitesUpToMax(SectorEntityToken entity) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        addSatellitesToEntity(entity, params.maxPhysicalSatellites, params.satelliteId, params.satelliteFactionId);
    }

    /**
     * Not only removes all satellites from entity, but also removes all objects and entities associated
     * with the existence of the satellites. This should only be used on removal of EVERYTHING.
     * @param entity
     */
    public static void purgeSatellitesFromEntity(SectorEntityToken entity) {
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        removeSatellitesFromEntity(entity);
        deleteMemoryKey(entityMemory, satelliteMarketId);

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        for (SectorEntityToken terrain : params.satelliteBarrages) { //todo: shouldnt cause issues if there is no terrain?
            removeSatelliteBarrageTerrain(entity, terrain);
        }

        for (CampaignFleetAPI satelliteFleet : params.satelliteFleets) {
            niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);
        }

        params.prepareForGarbageCollection();
        deleteMemoryKey(entityMemory, satelliteParamsId);
    }

    public static int getMaxBattleSatellites(SectorEntityToken primaryEntity) {
        return 3;
    }

    /**
     * Removes all satellites orbiting this entity.
     *
     * @param entity The target entity.
     */
    public static void removeSatellitesFromEntity(SectorEntityToken entity) {
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
        List<CustomCampaignEntityAPI> satellites = getSatellitesInOrbitOfEntity(entity);

        Iterator<CustomCampaignEntityAPI> iterator = satellites.iterator();
        for (int i = amountOfSatellitesToRemove; ((i > 0) && (iterator.hasNext())); i--) { //fixme: this might not be stable, i dont know
            CustomCampaignEntityAPI satellite = iterator.next();
            removeSatellite(satellite); //we cant directly modify the list, hence why we use the straight removal method here
            iterator.remove(); // and run iterator.remove
        }
        regenerateOrbitSpacing(entity);
    }

    public static void removeSatelliteFromEntity(SectorEntityToken entity, CustomCampaignEntityAPI satellite) {
        removeSatelliteFromEntity(entity, satellite, true);
    }

    /**
     * Removes a specific satellite from an entity.
     * @param entity The entity to remove the satellite from.
     * @param satellite The satellite to remove.
     * @param regenerateOrbit If true, satellite orbit will be regenerated.
     */
    public static void removeSatelliteFromEntity(SectorEntityToken entity, CustomCampaignEntityAPI satellite, boolean regenerateOrbit) {
        removeSatellite(satellite);
        getSatellitesInOrbitOfEntity(entity).remove(satellite);
        if (regenerateOrbit) {
            regenerateOrbitSpacing(entity);
        }
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
        List<CustomCampaignEntityAPI> satellitesInOrbitOfEntity = getSatellitesInOrbitOfEntity(entity);

        float optimalOrbitAngleOffset = getOptimalOrbitalOffsetForSatellites(satellitesInOrbitOfEntity);
        float orbitAngle = 0;
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc, so its safe to not add a buffer to the calculation in the optimalangle method
        for (CustomCampaignEntityAPI satellite : satellitesInOrbitOfEntity) { //iterates through each orbitting satellite and offsets them
            if (orbitAngle >= 360) {
                if (Global.getSettings().isDevMode()) {
                    Global.getSector().getCampaignUI().addMessage("A satellite on " + entity + " was given a orbit offset of " + orbitAngle + "."); //debug code
                    log.debug("A satellite on " + entity + " was given a orbit offset of " + orbitAngle + ".");
                    removeSatelliteFromEntity(entity, satellite, false); //we dont want these weirdos overlapping
                }
            }
            niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
            addOrbitPointingDownWithRelativeOffset(satellite, entity, orbitAngle, params.satelliteOrbitDistance);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
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
     * @return Either null, or an instance of niko_MPC_satelliteParams.
     */
    public static niko_MPC_satelliteParams getEntitySatelliteParams(SectorEntityToken entity) {
        return (niko_MPC_satelliteParams) entity.getMemoryWithoutUpdate().get(satelliteParamsId);
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
     * Gets a list of satellites in orbit around entity, using entity's satellite params.
     * @param entity The entity of which the satellites are orbitting.
     * @return A arraylist of satellite in orbit around the entity. Can return an empty list.
     */
    public static List<CustomCampaignEntityAPI> getSatellitesInOrbitOfEntity(SectorEntityToken entity) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        if (params != null) {
            return params.getSatellites();
        }
        return new ArrayList<>();
    }

    public static String getCurrentSatelliteFactionId(niko_MPC_satelliteParams params) {
        return getCurrentSatelliteFactionId(params.entity);
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the entity's faction id whenever it's ran.
     * @param entity The entity to get the params from.
     * @return A faction ID, in string form. Can return null if entity has no satellites.
     */
    public static String getCurrentSatelliteFactionId(SectorEntityToken entity) {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return null;

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);

        if (params != null) {
            MarketAPI market = getEntitySatelliteMarket(entity);
            if (market != null) {
                if (market.isPlanetConditionMarketOnly()) {
                    params.setSatelliteId("derelict");
                } else params.setSatelliteId(market.getFactionId());
            }
            else {
                params.setSatelliteId(entity.getFaction().getId());
            }
        } else {
            if (Global.getSettings().isDevMode()) {
                niko_MPC_debugUtils.displayError("getCurrentSatelliteFactionId failure");
            }
            logEntityData(entity);
            return entity.getFaction().getId();
        }
        return params.getSatelliteFactionId();
    }

    public static FactionAPI getCurrentSatelliteFaction(niko_MPC_satelliteParams params) {
        return Global.getSector().getFaction(getCurrentSatelliteFactionId(params));
    }

    public static float getOptimalOrbitalOffsetForSatellites(SectorEntityToken entity) {
        return getOptimalOrbitalOffsetForSatellites(getSatellitesInOrbitOfEntity(entity));
    }

    public static float getOptimalOrbitalOffsetForSatellites(List<CustomCampaignEntityAPI> satelliteInOrbitOfEntity) {
        int numOfSatellites = satelliteInOrbitOfEntity.size();

        float optimalAngle = (360 / (float) numOfSatellites); //todo: explain the math

        if (optimalAngle == 360) {
            optimalAngle = 0; //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle;
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
        return ((round((entity.getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    // GETTING ENTITIES AND CHECKING CAPABILITIES

    /**
     * @param location The location to scan.
     * @return A new set containing every entity that had defenseSatellitesApplied(entity) return true.
     */
    public static Set<SectorEntityToken> getEntitiesInLocationWithSatellites(LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = new HashSet<>();
        for (SectorEntityToken entity : location.getAllEntities()) {
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
     * First gets a list of entities in location with satellites, then does a range check using the entity's params' satelliteInterferenceDistance variable.
     * @param coordinates The coordinates to compare the entity to.
     * @param location The location to scan.
     * @return A set containing every entity with satellites that has the coordinates within their interference distance.
     */
    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellites(Vector2f coordinates, LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = getEntitiesInLocationWithSatellites(location);

        for (SectorEntityToken entity : entitiesWithSatellites) {
            niko_MPC_satelliteParams params = getEntitySatelliteParams(entity); //we can use this here because the previously used method only returns things with params
            if (!MathUtils.isWithinRange(entity, coordinates, params.satelliteInterferenceDistance)) {
                entitiesWithSatellites.remove(entity); //have to remove because we're using a full list already
            }
        }
        return entitiesWithSatellites;
    }

    public static Set<SectorEntityToken> getEntitiesWithSatellitesCapableOfFighting(Set<SectorEntityToken> entities) {
        return entities; //todo: unfinished
    }

    /**
     * @param fleet The fleet to check.
     * @return A list of entities with satellites that are willing to fight the fleet.
     */
    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellitesWillingToFight(CampaignFleetAPI fleet) {
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(fleet.getLocation(), fleet.getContainingLocation());

        for (SectorEntityToken entity : entitiesWithSatellites) {
            if (!doEntitySatellitesWantToFight(entity, fleet)) {
                entitiesWithSatellites.remove(entity);
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

    public static CampaignFleetAPI getSideForSatellitesAgainstFleets(SectorEntityToken entity, CampaignFleetAPI fleet, CampaignFleetAPI fleetTwo) {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return null;

        boolean wantsToFightOne = false;
        boolean wantsToFightTwo = false;

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        if ((doEntitySatellitesWantToFight(entity, fleet)) && (areEntitySatellitesCapableOfFighting(entity, fleet))) wantsToFightOne = true;
        if ((doEntitySatellitesWantToFight(entity, fleetTwo)) && (areEntitySatellitesCapableOfFighting(entity, fleetTwo))) wantsToFightTwo = true;

        if (wantsToFightOne && wantsToFightTwo) {
            return null;
        }

        if (wantsToFightOne) {
            return fleet;
        }
        else if (wantsToFightTwo) {
            return fleetTwo;
        }
        else return null;

    }

    public static HashMap<SectorEntityToken, CampaignFleetAPI> getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(SectorEntityToken entity, CampaignFleetAPI fleet, CampaignFleetAPI otherFleet) {
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(entity.getLocation(), entity.getContainingLocation());

        HashMap<SectorEntityToken, CampaignFleetAPI> sidesTaken = new HashMap<>();

        for (SectorEntityToken entityWithSatellite : entitiesWithSatellites) {
            CampaignFleetAPI fleetChosen = getSideForSatellitesAgainstFleets(entityWithSatellite, fleet, otherFleet);
            if (fleetChosen != null) {
                sidesTaken.put(entityWithSatellite, fleetChosen);
            }
        }
        return sidesTaken;
    }

    /**
     * @param entity The entity to get the satellites from.
     * @param battle The battle to get the side for.
     * @return The battleside that entity's satellites would pick. Can return null if the entity has no satellites.
     */
    public static BattleAPI.BattleSide getSideForSatellites(SectorEntityToken entity, BattleAPI battle) {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return null;
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(params);
        //attemptToFillFleetWithVariants(50, satelliteFleet, params.weightedVariantIds); //todo: dont know if i need this
        BattleAPI.BattleSide battleSide = battle.pickSide(satelliteFleet);

        niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet); // i dont think its actually placed in the system so we can just despawn and not worry about it

        return battleSide;
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

    public static boolean doEntitySatellitesWantToFight(niko_MPC_satelliteParams params, CampaignFleetAPI fleet) {
        return doEntitySatellitesWantToFight(params.entity, fleet);
    }

    /**
     * Used for generating battles and autoresolve and such.
     * @param entity The entity to get the satellite params from.
     * @param fleet The fleet to check.
     * @return True, if the satellite params' faction is hostile to the fleet's faction. Can return false if params are null.
     */
    public static boolean doEntitySatellitesWantToFight(SectorEntityToken entity, CampaignFleetAPI fleet) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);

        if (params == null) {
            if (Global.getSettings().isDevMode()) {
                niko_MPC_debugUtils.displayError("doEntitySatellitesWantToFight params null");
            }
            log.debug(entity.getName() + "had null params in doEntitySatellitesWantToFight");
            logEntityData(entity);
            return false;
        }
        boolean marketUncolonized = false;
        MarketAPI market = entity.getMarket();
        if (market != null) {
            if (market.isPlanetConditionMarketOnly()) {
                marketUncolonized = true;
            }
        }

        CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.spawnSatelliteFleet(params, fleet.getLocation(), fleet.getContainingLocation());
        boolean wantsToFight = satelliteFleet.isHostileTo(fleet);
        niko_MPC_fleetUtils.safeDespawnFleet(satelliteFleet);

        return (wantsToFight || (marketUncolonized && !Objects.equals(fleet.getFaction().getId(), "derelict")));
    }

    /**
     * Used for things such as preventing the player from interacting with a market.
     * @param entity The entity to get the satellite params from.
     * @param fleet The fleet to check.
     * @return True, if the satellite params' faction is inhospitable or worse to fleets' faction, or if the fleet has no transponder.
     * Can return false if params are null.
     */
    public static boolean doEntitySatellitesWantToBlock(SectorEntityToken entity, CampaignFleetAPI fleet) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);

        if (params == null) {
            if (Global.getSettings().isDevMode()) {
                niko_MPC_debugUtils.displayError("doEntitySatellitesWantToBlock params null");
            }
            log.debug(entity.getName() + "had null params in doEntitySatellitesWantToBlock");
            logEntityData(entity);
            return false;
        }
        return (!fleet.isTransponderOn() ||
                getCurrentSatelliteFaction(params).isAtBest(fleet.getFaction(), RepLevel.INHOSPITABLE) ||
                doEntitySatellitesWantToFight(entity, fleet));
    }

    /**
     * @param entity The entity to check.
     * @return True if the entity isn't already blocking the fleet, or if entity's satellite params' grace period is
     * less or equal to 0.
     */
    public static boolean areEntitySatellitesCapableOfBlocking(SectorEntityToken entity, CampaignFleetAPI fleet) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        BattleAPI battle = fleet.getBattle();

        return ((battle != null && tracker.areSatellitesInvolvedInBattle(battle, params)) || params.getGracePeriod(fleet) <= 0);
    }

    private static boolean areEntitySatellitesCapableOfFighting(SectorEntityToken entity, CampaignFleetAPI fleet) {
        return true;
    }


    public static void makeEntitySatellitesEngageFleet(SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return;

        BattleAPI battleJoined = null;

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        BattleAPI battle = fleet.getBattle();
        if (battle != null) {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            if (tracker.areSatellitesInvolvedInBattle(battle, params)) return;
        }

        CampaignFleetAPI satelliteFleet = niko_MPC_fleetUtils.createNewFullSatelliteFleet(params, fleet);
        if (battle != null) {
            if (!battle.join(satelliteFleet)) {
                niko_MPC_debugUtils.displayError("makeEntitySatellitesEngageFleet battle join failure");
            }
            else {
                battleJoined = battle;
            }
        }
        else {
            BattleAPI newBattle = Global.getFactory().createBattle(satelliteFleet, fleet); //todo: this may not work
            battleJoined = newBattle;
        }
        if (battleJoined != null) {
            getSatelliteBattleTracker().associateSatellitesWithBattle(battleJoined, params, battleJoined.pickSide(satelliteFleet));
        }
    }

    public static void makeNearbyEntitySatellitesEngageFleet(CampaignFleetAPI fleet) {
        for (SectorEntityToken entity : getNearbyEntitiesWithSatellitesWillingAndCapableToFight(fleet)) {
            makeEntitySatellitesEngageFleet(entity, fleet);
        }
    }

    //MISC

    public static boolean defenseSatellitesApplied(SectorEntityToken entity) {
        return getEntitySatelliteParams(entity) != null;
    }

    public static void removeSatelliteBarrageTerrain(SectorEntityToken relatedEntity, SectorEntityToken terrain) {
        LocationAPI containingLocation = relatedEntity.getContainingLocation();

        terrain.setExpired(true);
        containingLocation.removeEntity(terrain);
    }

    public static void incrementSatelliteGracePeriod(CampaignFleetAPI fleet, float amount, SectorEntityToken entity) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        if (params == null) return;

        if (!entity.hasScriptOfClass(niko_MPC_gracePeriodDecrementer.class)) {
            niko_MPC_gracePeriodDecrementer decrementerScript = new niko_MPC_gracePeriodDecrementer(params);
            entity.addScript(decrementerScript);
            params.gracePeriodDecrementer = decrementerScript;
        }
        params.adjustGracePeriod(fleet, amount);
    }
}

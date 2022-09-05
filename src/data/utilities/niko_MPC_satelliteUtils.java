package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_fleetUtils.createSatelliteFleetTemplate;
import static data.utilities.niko_MPC_ids.*;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;
import static data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset;
import static data.utilities.niko_MPC_planetUtils.getOptimalOrbitalOffsetForSatellites;
import static data.utilities.niko_NPC_debugUtils.displayErrorToCampaign;
import static data.utilities.niko_NPC_debugUtils.logEntityData;

public class niko_MPC_satelliteUtils {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    //////////////////////////
    //                      //
    // ADDITION AND REMOVAL //
    //                      //
    //////////////////////////

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

    public static void purgeSatellitesFromEntity(SectorEntityToken entity) {
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        removeSatellitesFromEntity(entity);
        deleteMemoryKey(entityMemory, satelliteMarketId);

        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        params.prepareForGarbageCollection();
        deleteMemoryKey(entityMemory, satelliteParamsId);
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
     * Does not make a satellite with a facing of NaN work.
     **/
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

    public static List<CustomCampaignEntityAPI> getSatellitesInOrbitOfEntity(SectorEntityToken entity) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
        if (params != null) {
            return params.getSatellites();
        }
        return new ArrayList<>();
    }

    // NEW SHIT USE IT NIKKO FUCKER

    public static String getCurrentSatelliteFactionId(SectorEntityToken entity) {
        niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);

        if (params != null) {
            MarketAPI market = getEntitySatelliteMarket(entity);
            if (market != null) {
                params.setSatelliteId(market.getFactionId());
            }
        } else {
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("getCurrentSatelliteFactionId failure");
            }
            logEntityData(entity);
            return entity.getFaction().getId();
        }
        return params.getSatelliteFaction().getId();
    }


    public static boolean marketsDesynced(SectorEntityToken entity) {
        return marketsDesynced(entity, entity.getMarket());
    }

    public static boolean marketsDesynced(SectorEntityToken entity, MarketAPI market) {
        return (memorySatellitesDesyncedWithMarket(entity.getMemoryWithoutUpdate(), market));
    }

    public static boolean memorySatellitesDesyncedWithMarket(MemoryAPI memory, MarketAPI market) {
        return (getMemorySatelliteMarket(memory) != market);
    }

    public static void syncMarket(SectorEntityToken entity, MarketAPI market) {
        entity.getMemoryWithoutUpdate().set(satelliteMarketId, market);
    }

    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, niko_MPC_satelliteParams params) {
        initializeSatellitesOntoEntity(entity, entity.getMarket(), params);
    }

    /**
     * This is what should be called the FIRST TIME an entity gains satellites, or after satellites have been entirely removed.
     * @param entity The entity to add markets to.
     * @param market Will have satelliteMarketId set to this if not null.
     */
    public static void initializeSatellitesOntoEntity(SectorEntityToken entity, MarketAPI market, niko_MPC_satelliteParams params) {
        if (!doEntityHasNoSatellitesTest(entity)) { //if the test fails, something fucked up, lets abort
            return;
        }
        MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
        if (market != null) {
            entityMemory.set(satelliteMarketId, market); // we're already protected from overwriting satellites with the above test
        }
        entityMemory.set(satelliteParamsId, params); //store our parameters onto the entity
        addSatellitesUpToMax(entity);
    }

    public static Set<SectorEntityToken> getEntitiesInLocationWithSatellites(LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = new HashSet<>();
        for (SectorEntityToken entity : location.getAllEntities()) {
            if (defenseSatellitesApplied(entity)) {
             entitiesWithSatellites.add(entity);
            }
        }
        return entitiesWithSatellites;
    }

    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellites(Vector2f coordinates, LocationAPI location) {
        Set<SectorEntityToken> entitiesWithSatellites = getEntitiesInLocationWithSatellites(location);

        for (SectorEntityToken entity : entitiesWithSatellites) {
            niko_MPC_satelliteParams params = getEntitySatelliteParams(entity); //we can use this here because the previously used method only returns things with params
            if (MathUtils.isWithinRange(entity, coordinates, params.satelliteInterferenceDistance)) {
                entitiesWithSatellites.add(entity);
            }
        }
        return entitiesWithSatellites;
    }

    public static Set<SectorEntityToken> getEntitiesWithSatellitesCapableOfFighting(Set<SectorEntityToken> entities) {
        return entities; //todo: unfinished
    }

    public static Set<SectorEntityToken> getNearbyEntitiesWithSatellitesWillingToFight(CampaignFleetAPI fleet) {
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(fleet.getLocation(), fleet.getContainingLocation());

        for (SectorEntityToken entity : entitiesWithSatellites) {
            if (doEntitySatellitesWantToFight(entity, fleet)) {
                entitiesWithSatellites.add(entity);
            }
        }
        return entitiesWithSatellites;
    }

    public static HashMap<SectorEntityToken, BattleAPI.BattleSide> getNearbyEntitiesWithSatellitesWillingToJoinBattle(BattleAPI battle) {
        LocationAPI containingLocation = battle.getNonPlayerCombined().getContainingLocation();
        Vector2f coordinates = battle.computeCenterOfMass();
        Set<SectorEntityToken> entitiesWithSatellites = getNearbyEntitiesWithSatellites(coordinates, containingLocation);
        HashMap<SectorEntityToken, BattleAPI.BattleSide> entitiesWillingToFight = new HashMap<>();

        for (SectorEntityToken entity : entitiesWithSatellites) { //todo methodize
            niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
            CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(params, getCurrentSatelliteFactionId(entity), "");
            //attemptToFillFleetWithVariants(50, satelliteFleet, params.weightedVariantIds); //todo: dont know if i need this
            BattleAPI.BattleSide battleSide = battle.pickSide(satelliteFleet);

            if (battleSide != BattleAPI.BattleSide.NO_JOIN) {
                entitiesWillingToFight.put(entity, battleSide);
            }
            satelliteFleet.despawn();
        }
        return entitiesWillingToFight;
    }

    public static HashMap<SectorEntityToken, BattleAPI.BattleSide> getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(BattleAPI battle) {
        return getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle);
    }

    public static List<SectorEntityToken> getEntitiesOnSide(BattleAPI battle, BattleAPI.BattleSide side, HashMap<SectorEntityToken, BattleAPI.BattleSide> entityMap) {
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
                displayErrorToCampaign("doEntitySatellitesWantToFight params null");
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
        FactionAPI satelliteFaction = Global.getSector().getFaction(getCurrentSatelliteFactionId(entity));
        return (satelliteFaction.isHostileTo(fleet.getFaction()) || marketUncolonized);
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
                displayErrorToCampaign("doEntitySatellitesWantToBlock params null");
            }
            log.debug(entity.getName() + "had null params in doEntitySatellitesWantToBlock");
            logEntityData(entity);
            return false;
        }
        return (!fleet.isTransponderOn() ||
                params.getSatelliteFaction().isAtBest(fleet.getFaction(), RepLevel.INHOSPITABLE) ||
                doEntitySatellitesWantToFight(entity, fleet));
    }

    public static niko_MPC_satelliteParams getEntitySatelliteParams(SectorEntityToken entity) {
        return (niko_MPC_satelliteParams) entity.getMemoryWithoutUpdate().get(satelliteParamsId);
    }

    public static MarketAPI getEntitySatelliteMarket(SectorEntityToken entity) {
        return (getMemorySatelliteMarket(entity.getMemoryWithoutUpdate()));
    }

    public static MarketAPI getMemorySatelliteMarket(MemoryAPI memory) {
        return (MarketAPI) memory.get(satelliteMarketId);
    }

    public static boolean defenseSatellitesApplied(SectorEntityToken entity) {
        return getEntitySatelliteParams(entity) != null;
    }

    /**
     * Returns false if the entity has satellite params, a tracker, or if the entity has a satellite market.
     */
    public static boolean doEntityHasNoSatellitesTest(SectorEntityToken entity) {
        boolean result = true;
        if (getEntitySatelliteMarket(entity) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because " + getEntitySatelliteMarket(entity).getName() + " was still applied");
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("doEntityHasNoSatellitesTest getEntitySatelliteMarket failure");
            }
            result = false;
        }
        if (defenseSatellitesApplied(entity) || entity.getMemoryWithoutUpdate().get(satelliteParamsId) != null) {
            log.debug(entity.getName() + " failed doEntityNoSatellitesTest because defenseSatellitesApplied returned true");
            if (Global.getSettings().isDevMode()) {
                displayErrorToCampaign("doEntityHasNoSatellitesTest defenseSatellitesApplied failure");
                result = false;
            }

        }
        if (!result) {
            logEntityData(entity);
        }
        return result;
    }
}

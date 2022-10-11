package data.scripts.campaign.misc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAI;
import data.scripts.campaign.listeners.niko_MPC_satelliteFleetDespawnListener;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import data.scripts.everyFrames.niko_MPC_satelliteBattleCheckerForStation;
import data.scripts.everyFrames.niko_MPC_satelliteFleetProximityChecker;
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner;
import data.utilities.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_ids.*;
import static data.utilities.niko_MPC_satelliteUtils.isSideValid;
import static java.lang.Math.round;

public class niko_MPC_satelliteHandler {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteHandler.class);

    static {
        log.setLevel(Level.ALL);
    }


    public CampaignFleetAPI fleetForPlayerDialog;
    @Nullable
    public niko_MPC_satelliteBattleCheckerForStation entityStationBattleChecker;
    public boolean done = false;

    public void setEntity(SectorEntityToken primaryEntity) {
        this.entity = primaryEntity;
    }

    public class niko_MPC_satelliteParams {
        public String satelliteId;
        public String satelliteFactionId;
        public int maxPhysicalSatellites;
        public int maxBattleSatellites;

        public float satelliteOrbitDistance;
        public float satelliteInterferenceDistance;
        public float satelliteBarrageDistance;

        public String satelliteFleetName;

        public HashMap<String, Float> weightedVariantIds;

        public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, String satelliteFleetName,
                                        int maxPhysicalSatellites, int maxBattleSatellites, float satelliteOrbitDistance, float satelliteInterferenceDistance,
                                        float barrageDistance, HashMap<String, Float> weightedVariantIds) {

            this.satelliteId = satelliteId;
            this.satelliteFactionId = satelliteFactionId;
            this.satelliteFleetName = satelliteFleetName;
            this.maxPhysicalSatellites = maxPhysicalSatellites;
            this.maxBattleSatellites = maxBattleSatellites;
            this.satelliteOrbitDistance = satelliteOrbitDistance;
            this.satelliteInterferenceDistance = satelliteInterferenceDistance;
            this.satelliteBarrageDistance = barrageDistance;
            this.weightedVariantIds = weightedVariantIds;
        }

        public void prepareForGarbageCollection() {
            return;
        }
    }

    public niko_MPC_satelliteParams params;

    private SectorEntityToken entity;

    public List<CustomCampaignEntityAPI> orbitalSatellites;
    public List<SectorEntityToken> satelliteBarrages = new ArrayList<>();
    public HashMap<CampaignFleetAPI, Float> gracePeriods = new HashMap<>();

    @Nullable
    public niko_MPC_gracePeriodDecrementer gracePeriodDecrementer;
    @Nullable
    public niko_MPC_satelliteFleetProximityChecker satelliteFleetProximityChecker;
    public List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();
    private CampaignFleetAPI dummyFleet;

    public niko_MPC_satelliteHandler(SectorEntityToken entity, String satelliteId, String satelliteFactionId, String satelliteFleetName,
                                     int maxPhysicalSatellites, int maxBattleSatellites,
                                     float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                     HashMap<String, Float> weightedVariantIds) {

        this(entity, satelliteId, satelliteFactionId, satelliteFleetName, maxPhysicalSatellites, maxBattleSatellites,
                satelliteOrbitDistance, satelliteInterferenceDistance, barrageDistance, weightedVariantIds,
                new ArrayList<CustomCampaignEntityAPI>());
    }

    public niko_MPC_satelliteHandler(SectorEntityToken entity, String satelliteId, String satelliteFactionId, String satelliteFleetName,
                                     int maxPhysicalSatellites, int maxBattleSatellites,
                                     float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                     HashMap<String, Float> weightedVariantIds, List<CustomCampaignEntityAPI> orbitalSatellites) {

        params = new niko_MPC_satelliteParams(
                satelliteId,
                satelliteFactionId,
                satelliteFleetName,
                maxPhysicalSatellites,
                maxBattleSatellites,
                satelliteOrbitDistance,
                satelliteInterferenceDistance,
                barrageDistance,
                weightedVariantIds);

        this.entity = entity;

        this.orbitalSatellites = orbitalSatellites;

        init();
    }

    private void init() {
        if (getEntity() == null) {
            prepareForGarbageCollection();
            return;
        }

        gracePeriodDecrementer = new niko_MPC_gracePeriodDecrementer(this);
        satelliteFleetProximityChecker = new niko_MPC_satelliteFleetProximityChecker(this, getEntity());
        entityStationBattleChecker = new niko_MPC_satelliteBattleCheckerForStation(this, getEntity().getMarket());
        List<EveryFrameScript> scriptsToAdd = new ArrayList<EveryFrameScript>(Arrays.asList(gracePeriodDecrementer, satelliteFleetProximityChecker, entityStationBattleChecker));
        niko_MPC_scriptUtils.addScriptsAtValidTime(scriptsToAdd, getEntity(), true);
    }

    public List<CustomCampaignEntityAPI> getSatellites() {
        return orbitalSatellites;
    }

    private FactionAPI getSatelliteFaction() {
        return Global.getSector().getFaction(getSatelliteFactionId());
    }

    public void prepareForGarbageCollection() {
        if (done) {
            return;
        }
        done = true;

        if (satelliteFleetProximityChecker != null) {
            satelliteFleetProximityChecker.prepareForGarbageCollection();
        }
        if (gracePeriodDecrementer != null) {
            gracePeriodDecrementer.prepareForGarbageCollection();
        }
        if (entityStationBattleChecker != null) {
            entityStationBattleChecker.prepareForGarbageCollection();
        }

        satelliteFleetProximityChecker = null;
        gracePeriodDecrementer = null;
        entityStationBattleChecker = null;

        if (entity != null) {
            MemoryAPI entityMemory = entity.getMemoryWithoutUpdate();
            if (entityMemory.get(satelliteHandlerId) == this) {
                niko_MPC_memoryUtils.deleteMemoryKey(entityMemory, satelliteHandlerId);
            }
            else if (entityMemory.contains(satelliteHandlerId)) {
                niko_MPC_debugUtils.displayError("unsynced satellite handler on " + entity.getName() + " on handler GC attempt");
            }
            //todo: EXPERIMENTAL: not nulling entity
        } else {
            niko_MPC_debugUtils.displayError("entity was null on handler GC attempt", true);
        }

        if (orbitalSatellites != null) {
            orbitalSatellites.clear();
        }
        if (satelliteBarrages != null) {
            satelliteBarrages.clear();
        }
        if (gracePeriods != null) {
            gracePeriods.clear();
        }

        if (satelliteFleets != null) {
            for (CampaignFleetAPI fleet : satelliteFleets) {
                niko_MPC_fleetUtils.despawnSatelliteFleet(fleet, false);
            }
            satelliteFleets.clear();
        }

        if (getDummyFleet() != null) {
            niko_MPC_fleetUtils.despawnSatelliteFleet(getDummyFleet(), true);
            dummyFleet = null;
        }

        if (getParams() != null) {
            getParams().prepareForGarbageCollection();
        }
        else {
            niko_MPC_debugUtils.displayError("null params on handler GC attempt, entity: " + entity.getName());
        }

        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        tracker.removeHandlerFromAllBattles(this);
    }

    private CampaignFleetAPI getDummyFleet() {
        return dummyFleet;
    }
    
    /**
     * Sets the faction ID of the params to factionId, and updates the faction of all satellite entities.
     * @param factionId The factionId to set.
     */
    public void setSatelliteId(String factionId) {
        setSatelliteId(factionId, true);
    }

    /**
     * Sets the faction ID of the params to factionId. Optionally updates the faction of all satellite entities.
     * @param factionId The factionId to set.
     * @param withUpdate If true, sets the faction of all satellite entities to the new faction id.
     */
    public void setSatelliteId(String factionId, boolean withUpdate) {
        getParams().satelliteFactionId = factionId;

        if (withUpdate) {
            updateSatelliteFactions();
        }
    }
    
    /**
    * Iterates through every satellite entity we hold, and sets their
    * faction to our factionId.
    */
    public void updateSatelliteFactions() {
        for (CampaignFleetAPI fleet : satelliteFleets) {
            fleet.setFaction(getSatelliteFactionId());
        }

        for (CustomCampaignEntityAPI satellite : orbitalSatellites) {
            satellite.setFaction(getSatelliteFactionId());
        }

        if (dummyFleet != null) {
            dummyFleet.setFaction(getSatelliteFactionId());
        }  
    }

    private String getSatelliteFactionId() {
        if (getParams() == null) {
            return "derelict";
        }
        return getParams().satelliteFactionId;
    }

    public niko_MPC_satelliteParams getParams() {
        return params;
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the factionId of the params it is called on.
     * @return A faction ID, in string form.
     */
    public String getCurrentSatelliteFactionId() {
        updateFactionId();
        return getSatelliteFactionId();
    }

    public FactionAPI getCurrentSatelliteFaction() {
        return Global.getSector().getFaction(getCurrentSatelliteFactionId());
    }
    
    /**
    * Updates the factionId, by syncing it with the market, or setting it to derelict
    * if the market is uncolonized.
    * Updates the faction of all satellite entities.
    */
    public void updateFactionId() {
        MarketAPI market = niko_MPC_satelliteUtils.getEntitySatelliteMarket(entity);
        if (market != null) {
            if (market.isPlanetConditionMarketOnly()) {
                if (!Objects.equals(getSatelliteFactionId(), "derelict")) {
                    setSatelliteId("derelict"); //its relatively expensive to run this (due to iterations), so we try to minimize it
                }
            } else if (!Objects.equals(this.getSatelliteFactionId(), market.getFactionId())) {
                setSatelliteId(market.getFactionId());
            }
        }
        else if (!Objects.equals(this.getSatelliteFactionId(), entity.getFaction().getId())) {
            setSatelliteId(getEntity().getFaction().getId());
        }
    }

    public void updateFactionForSelfAndSatellites() {
        updateFactionId();
        updateSatelliteFactions();
    }


    public HashMap<CampaignFleetAPI, Float> getGracePeriods() {
        return gracePeriods;
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to get the value from.
     * @return the gracePeriod associated value to fleet. 
     */
    public float getGracePeriod(CampaignFleetAPI fleet) {
        addFleetRefToGracePeriodsIfNonePresent(fleet);
        return getGracePeriods().get(fleet);
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to adjust the grace period of.
     * @param amount The amount to adjust the grace period of fleet of.
     */
    public void adjustGracePeriod(CampaignFleetAPI fleet, float amount) {
        addFleetRefToGracePeriodsIfNonePresent(fleet); //todo: make this work with player battles
        getGracePeriods().put(fleet, Math.max(0, getGracePeriods().get(fleet) + amount));
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to check.
     */
    private void addFleetRefToGracePeriodsIfNonePresent(CampaignFleetAPI fleet) {
        if (getGracePeriods().get(fleet) == null) {
            gracePeriods.put(fleet, 0f); // we dont use a amount arg here because we only exist here to initialize a new entry
        }
    }

    public String getSatelliteFleetName() {
        return getParams().satelliteFleetName;
    }

    /**
    * Should be called whenever a new non-dummy satellite fleet is created.
    */
    public void newSatellite(CampaignFleetAPI satelliteFleet) {
        satelliteFleets.add(satelliteFleet);
    }

    public float getSatelliteInterferenceDistance() {
        return getParams().satelliteInterferenceDistance;
    }

    /**
     * Generates an offset with which satellites in orbit of our entity will be spaced apart by.
     * Is based on the amount of satellites in orbit.
     * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
     */
    public float getOptimalOrbitalOffsetForSatellites() {
        int numOfSatellites = orbitalSatellites.size();

        float optimalAngle = (360 / (float) numOfSatellites);
        // 1 satellite = offset of 360, so none. 2 satellites = offset or 180, so they are on opposite ends of the planet.
        // 3 satellites = offset of 120, meaning the satellites form a triangle around the entity. Etc.

        if (optimalAngle == 360) {
            optimalAngle = 0; //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle;
    }

    /**
     * Places all satellites in orbit around our entity, ensuring they are all equally spaced apart from eachother.
     */
    public void regenerateOrbitSpacing() {
        float optimalOrbitAngleOffset = getOptimalOrbitalOffsetForSatellites();
        float orbitAngle = 0;
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc, so its safe to not add a buffer to the calculation in the optimalangle method
        Iterator<CustomCampaignEntityAPI> iterator = getSatellites().iterator();
        while (iterator.hasNext()) {
            CustomCampaignEntityAPI satellite = iterator.next();
            if (orbitAngle >= 360) {
                niko_MPC_debugUtils.displayError("regenerateOrbitSpacing orbitAngle = " + orbitAngle);
                removeSatellite(satellite, false, false); //we dont want these weirdos overlapping
                iterator.remove();
            }
            niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset(satellite, getEntity(), orbitAngle, getParams().satelliteOrbitDistance);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     *
     * @param satellite The satellite to remove.
     */
    public void removeSatellite(CustomCampaignEntityAPI satellite, boolean regenerateOrbit, boolean removeFromList) {
        if (removeFromList) {
            getSatellites().remove(satellite);
        }
        Misc.fadeAndExpire(satellite);
        satellite.getContainingLocation().removeEntity(satellite);

        niko_MPC_memoryUtils.deleteMemoryKey(satellite.getMemoryWithoutUpdate(), satelliteHandlerIdAlt);

        if (regenerateOrbit) {
            regenerateOrbitSpacing();
        }
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from entity's orbit. Will end execution early if the list becomes empty.
     *
     * @param amountOfSatellitesToRemove The amount of satellites to remove from entity.
     */
    public void removeSatellitesFromEntity(int amountOfSatellitesToRemove) {

        Iterator<CustomCampaignEntityAPI> iterator = getSatellites().iterator();
        while (iterator.hasNext()) {
            CustomCampaignEntityAPI satellite = iterator.next();
            removeSatellite(satellite, false, false); //we cant directly modify the list, hence why we use the straight removal method here
            iterator.remove(); // and run iterator.remove
        }
        regenerateOrbitSpacing();
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to our entity through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id                      The id to be assigned to the satellites.
     * @param faction                 The factionid to be given to the satellites.
     */
    public void addSatellitesToEntity(int amountOfSatellitesToAdd, String id, String faction) {
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(false, id, faction);
        }
        regenerateOrbitSpacing(); //only needs to be done once, after all the satellites are added, this does not generate the error
    }

    /**
     * Adds a new CustomCampaignEntity satellite of type id to entity and sets up an orbit around it.
     * @param regenerateOrbit If true, repositions all satellites in orbit with the same ratio
     * of distance to eachother.
     * @param id The Id of the satellite to add.
     * @param factionId The faction id to set as the satellite's faction.
     */
    public void addSatellite(boolean regenerateOrbit, String id, String factionId) {
        int satelliteNumber = ((getSatellites().size()) + 1);
        String orderedId = (id + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc
        // i dont do this orderedid for any particular reason, i just wanted to. it causes no issues but
        // can safely be removed

        LocationAPI containingLocation = getEntity().getContainingLocation();
        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = containingLocation.addCustomEntity(orderedId, null, id, factionId);
        niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset(satellite, getEntity(), 0, params.satelliteOrbitDistance); //set up the orbit

        satellite.getMemoryWithoutUpdate().set(satelliteHandlerIdAlt, this);

        getSatellites().add(satellite); //now add the satellite to the params' list

        if (regenerateOrbit)
            regenerateOrbitSpacing(); //and set up the orbital angles
    }

    public void addSatellitesUpToMax() {
        addSatellitesToEntity(getParams().maxPhysicalSatellites, getParams().satelliteId, getParams().satelliteFactionId);
    }

    /**
     * Gets the side our dummy fleet would enter.
     * @param battle The battle to get the side for.
     * @return The battleside that entity's satellites would pick. Can return null if the entity has no satellites.
     */
    public BattleAPI.BattleSide getSideForBattle(BattleAPI battle) {
        CampaignFleetAPI dummyFleet = getDummyFleetWithUpdate();
        updateFactionId();
        updateSatelliteFactions();

        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        if (tracker.areSatellitesInvolvedInBattle(battle, this)) {
            return BattleAPI.BattleSide.NO_JOIN;
        }

        BattleAPI.BattleSide battleSide = battle.pickSide(dummyFleet);

        return battleSide;
    }

    /**
     * Used for generating battles and autoresolve and such.
     * @param fleet The fleet to check.
     * @return True, if the params' dummy fleet is hostile to the given fleet. False otherwise.
     */
    public boolean doSatellitesWantToFight(CampaignFleetAPI fleet) {

        boolean marketUncolonized = false;
        MarketAPI market = getEntity().getMarket();
        if (market != null) {
            if (market.isPlanetConditionMarketOnly()) {
                marketUncolonized = true;
            }
        }

        boolean wantsToFight = getDummyFleetWithUpdate().isHostileTo(fleet);

        // uncolonized markets are derelict and hostile to everyone
        return (wantsToFight || (marketUncolonized && !Objects.equals(fleet.getFaction().getId(), "derelict")));
    }

    /**
     * Unfinished.
     */
    public boolean areSatellitesCapableOfFighting(CampaignFleetAPI fleet) {
        return areSatellitesCapableOfBlocking(fleet);
    }

    /**
     * Used for things such as preventing the player from interacting with a market.
     * @param fleet The fleet to check.
     * @return True, if the satellite params' faction is inhospitable or worse to fleets' faction, if the fleet has no transponder,
     * or if the satellites want to fight.
     */
    public boolean doSatellitesWantToBlock(@NotNull CampaignFleetAPI fleet) {
        return (!fleet.isTransponderOn() ||
                getCurrentSatelliteFaction().isAtBest(fleet.getFaction(), RepLevel.INHOSPITABLE) ||
                doSatellitesWantToFight(fleet));
    }

    /**
     * @return True if the entity isn't already blocking the fleet, or if entity's satellite params' grace period is
     * less or equal to 0. False otherwise.
     */
    public boolean areSatellitesCapableOfBlocking(@NotNull CampaignFleetAPI fleet) {
        BattleAPI battle = fleet.getBattle();
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

        if (battle != null && tracker.areSatellitesInvolvedInBattle(battle, this)) {
            return false;
        }
        return (getGracePeriod(fleet) <= 0);
    }

    /**
     * Uses doEntitySatellitesWantToBlock/Fight and areEntitySatellitesCapableOfFBlocking/Fighting to determine
     * which fleets the satellites would want to fight when spawned.
     * @param fleet The first fleet to check.
     * @param fleetTwo The second fleet to check.
     * @param capabilityCheck If true, runs an additional check that skips over a fleet if areEntitySatellitesCapableOfBlocking returns false.
     * @return Null if the satellites want to fight both or neither, otherwise, returns which of the two fleets they're willing to fight.
     */
    public CampaignFleetAPI getSideForSatellitesAgainstFleets(@Nullable CampaignFleetAPI fleet, @Nullable CampaignFleetAPI fleetTwo, boolean capabilityCheck) {

        boolean wantsToFightOne = false;
        boolean wantsToFightTwo = false;

        if (fleet == fleetTwo) {
            niko_MPC_debugUtils.displayError("getSideForSatellitesAgainstFleets same fleet, fleet: " + fleet);
            if (fleet != null) {
                if ((doSatellitesWantToFight(fleet)) && (areSatellitesCapableOfFighting(fleet))) {
                    return fleet;
                }
            }
            return null;
        }

        if (fleet != null && (doSatellitesWantToFight(fleet)) && (areSatellitesCapableOfFighting(fleet))) wantsToFightOne = true;
        if (fleetTwo != null && (doSatellitesWantToFight(fleetTwo)) && (areSatellitesCapableOfFighting(fleetTwo))) wantsToFightTwo = true;

        if (wantsToFightOne && wantsToFightTwo) {
            return null;
        }

        if (wantsToFightOne) {
            if (!capabilityCheck || areSatellitesCapableOfBlocking(fleet)) {
                return fleet;
            }
        }
        else if (wantsToFightTwo) {
            if (!capabilityCheck || areSatellitesCapableOfBlocking(fleetTwo)) {
                return fleetTwo;
            }
        }
        return null;
    }

    /**
     * Forces us to spawn a full satellite fleet on the target, unless
     * we're already fighting them or they have grace.
     * @param fleet The fleet to check and engage.
     */
    public void makeEntitySatellitesEngageFleet(@NotNull CampaignFleetAPI fleet) {
        if (!shouldAndCanEngageFleet(fleet) || !niko_MPC_fleetUtils.isFleetValidEngagementTarget(fleet)) {
            return;
        }

        BattleAPI battleJoined = null;
        BattleAPI battle = fleet.getBattle();
        CampaignFleetAPI satelliteFleet = createNewFullSatelliteFleet(fleet.getLocation(), fleet.getContainingLocation(), true, false);
        if (battle != null) {
            if (!battle.join(satelliteFleet)) {
                niko_MPC_debugUtils.displayError("makeEntitySatellitesEngageFleet battle join failure, fleet: " + fleet + ", battle: " + battle);
            }
            else {
                battleJoined = battle;
            }
        }
        else { //no battle? fine, i'll MAKE MY OWN
            satelliteFleet.clearAssignments(); // just in case the hold assignment all satellite fleets get is fucking with a few things
            satelliteFleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, fleet, 999999999, null); // again, sanity
            fleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, satelliteFleet, 1, null);

            satelliteFleet.setCircularOrbit(entity, VectorUtils.getAngle(fleet.getLocation(), entity.getLocation()),
                    Misc.getDistance(satelliteFleet, entity), 999999999);
            //todo: remove the above, its a stopgap so that fleets dont drift away, and it sucks ass

            BattleAPI newBattle = Global.getFactory().createBattle(satelliteFleet, fleet); // force the satellite to engage the enemy

            // removing the createBattle doesnt fix the god damn issue where fleets drift

            battleJoined = newBattle;
        }
        if (battleJoined != null) {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            tracker.associateSatellitesWithBattle(battleJoined, this, battleJoined.pickSide(satelliteFleet));
        }
    }

    public boolean shouldAndCanEngageFleet(@NotNull CampaignFleetAPI fleet) {
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        BattleAPI battle = fleet.getBattle();

        if (battle != null) {
            if (tracker.areSatellitesInvolvedInBattle(battle, this)) return false;
            if (!isSideValid(getSideForBattle(battle))) return false;
        }

        if (!doSatellitesWantToFight(fleet)) return false;
        if (!areSatellitesCapableOfFighting(fleet)) return false;

        return true;
    }

    /**
     * Creates an empty fleet with absolutely nothing in it, except for the memflags satellite fleets must have.
     * @return A new satellite fleet.
     */
    public CampaignFleetAPI createSatelliteFleetTemplate() {

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(getCurrentSatelliteFactionId(), getSatelliteFleetName(), true);
       // fleet.setFaction(getCurrentSatelliteFactionId());
        setTemplateMemoryKeys(fleet);

        fleet.setAI(new niko_MPC_satelliteFleetAI((CampaignFleet) fleet));
        fleet.addEventListener(new niko_MPC_satelliteFleetDespawnListener());

        PersonAPI aiCaptain = new AICoreOfficerPluginImpl().createPerson(Commodities.GAMMA_CORE, "derelict", null);
        fleet.setCommander(aiCaptain);

        return fleet;
    }

    public void cleanUpSatelliteFleetBeforeDeletion(@NotNull CampaignFleetAPI satelliteFleet) {

        BattleAPI battle = satelliteFleet.getBattle();
        if (battle != null) {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            if (tracker.areSatellitesInvolvedInBattle(battle, this)) {
                tracker.removeHandlerFromBattle(battle, this);
            }
        }

        if (fleetForPlayerDialog == satelliteFleet) {
            fleetForPlayerDialog = null;
        }

        getSatelliteFleets().remove(satelliteFleet);
        niko_MPC_memoryUtils.deleteMemoryKey(satelliteFleet.getMemoryWithoutUpdate(), satelliteHandlerId);
    }

    public List<CampaignFleetAPI> getSatelliteFleets() {
        return satelliteFleets;
    }

    public CampaignFleetAPI spawnSatelliteFleet(@NotNull Vector2f coordinates, @NotNull LocationAPI location, boolean temporary, boolean dummy) {
        CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate();

        location.addEntity(satelliteFleet);
        satelliteFleet.setLocation(coordinates.x, coordinates.y);
        if (temporary) {
            niko_MPC_temporarySatelliteFleetDespawner script = new niko_MPC_temporarySatelliteFleetDespawner(satelliteFleet, this);
            satelliteFleet.addScript(script);
            satelliteFleet.getMemoryWithoutUpdate().set(niko_MPC_ids.temporaryFleetDespawnerId, script);
        }

        satelliteFleet.addAssignment(FleetAssignment.HOLD, location.createToken(coordinates), 99999999f);

        if (dummy) {
            newDummySatellite(satelliteFleet);
        }
        else {
            newSatellite(satelliteFleet);
        }

        return satelliteFleet;
    }

    public CampaignFleetAPI createNewFullSatelliteFleet(Vector2f coordinates, LocationAPI location, boolean temporary, boolean dummy) {
        CampaignFleetAPI satelliteFleet = spawnSatelliteFleet(coordinates, location, temporary, dummy);
        nameFleetMembers(niko_MPC_fleetUtils.attemptToFillFleetWithVariants(getMaxBattleSatellites(), satelliteFleet, getWeightedVariantIds(), true));

        return satelliteFleet;
    }

    public void nameFleetMembers(@NotNull List<FleetMemberAPI> fleetMembers) {
        for (FleetMemberAPI fleetMember : fleetMembers) {
            String name = Global.getSector().getFaction(Factions.DERELICT).pickRandomShipName();
            if (name == null) {
                niko_MPC_debugUtils.displayError("deploySatellite null name, fleetMember: " + fleetMember);
                name = "this name is an error, please report this to niko";
            }
            fleetMember.setShipName(name);
        }
    }

    public CampaignFleetAPI createNewFullDummySatelliteFleet(Vector2f coordinates, LocationAPI location) {
        return createNewFullSatelliteFleet(coordinates, location, false, true);
    }

    public CampaignFleetAPI createDummyFleet(@NotNull SectorEntityToken entity) {
        CampaignFleetAPI satelliteFleet = createNewFullDummySatelliteFleet(new Vector2f(99999999, 99999999), entity.getContainingLocation());

        satelliteFleet.setDoNotAdvanceAI(true);

        return satelliteFleet;
    }

    private void setTemplateMemoryKeys(@NotNull CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();

        fleetMemory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

        fleetMemory.set(isSatelliteFleetId, true);
        fleetMemory.set(satelliteHandlerId, this);
    }

    public SectorEntityToken getEntity() {
        if (entity == null) {
            niko_MPC_debugUtils.displayError("entity somehow null on handler getEntity()", true, false);
        }
        return entity;
    }

    public int getMaxPhysicalSatellites() {
        return getParams().maxPhysicalSatellites;
    }

    public int getMaxPhysicalSatellitesBasedOnEntitySize(float radiusDivisor) {
        if (getEntity() == null) return 0;
        return ((round((getEntity().getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    public int getMaxBattleSatellites() {
        return getParams().maxBattleSatellites;
    }

    public HashMap<String, Float> getWeightedVariantIds() {
        return getParams().weightedVariantIds;
    }

    public float getSatelliteOrbitDistance() {
        return getParams().satelliteOrbitDistance;
    }

    /*public FactionAPI getFakeSatelliteFaction() {
        return Global.getSector().getFaction(getParams().fakeSatelliteFactionId);
    }*/

    /**
     * Instantiates a new dummy fleet is none is present, but ONLY if getSatelliteFaction() doesn't return null.
     * @return the dummyFleet used for things such as targetting and conditional attack logic. Can return a standard
     * satellite fleet if getSatelliteFaction() == null.
     */
    public CampaignFleetAPI getDummyFleetWithUpdate() {
        FactionAPI faction = getSatelliteFaction();
        if (dummyFleet == null) {
            if (faction != null) { // a strange hack i have to do, since this method is called before factions /exist/?
                createDummyFleet(getEntity());
            } else {
               return spawnSatelliteFleet(getEntity().getLocation(), getEntity().getContainingLocation(), true, false);
            }
        }
        dummyFleet.setFaction(getCurrentSatelliteFactionId()); // update da faction
        return dummyFleet;
    }

    /**
     * Instantiates a new dummy fleet is none is present, but ONLY if getSatelliteFaction() doesn't return null.
     * @param fleet The fleet to check.
     * @return dummy.isHostileTo(fleet).
     */
    public boolean dummyFleetWantsToFight(CampaignFleetAPI fleet) {
        CampaignFleetAPI dummy = getDummyFleetWithUpdate();
        updateFactionId();

        return dummy.isHostileTo(fleet);
    }

    public void newDummySatellite(CampaignFleetAPI satelliteFleet) {
        dummyFleet = satelliteFleet;
    }
}

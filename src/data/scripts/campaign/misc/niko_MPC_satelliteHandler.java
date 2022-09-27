package data.scripts.campaign.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_fleetsApproachingSatellitesChecker;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset;
import static java.lang.Math.round;

public class niko_MPC_satelliteHandler {

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

        public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, String satelliteFleetName, int maxPhysicalSatellites,
                                        int maxBattleSatellites, float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                        HashMap<String, Float> weightedVariantIds) {

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

    public SectorEntityToken entity;

    public List<CustomCampaignEntityAPI> orbitalSatellites;
    public List<SectorEntityToken> satelliteBarrages = new ArrayList<>();
    public HashMap<CampaignFleetAPI, Float> gracePeriods = new HashMap<>();

    public niko_MPC_fleetsApproachingSatellitesChecker approachingFleetChecker;

    public niko_MPC_gracePeriodDecrementer gracePeriodDecrementer;
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

        this.orbitalSatellites = orbitalSatellites; //todo: i dont like that im assigning this in a constructor

        init();
    }

    private void init() {
        //approachingFleetChecker = new niko_MPC_fleetsApproachingSatellitesChecker(this, entity);
        //entity.addScript(approachingFleetChecker);

        gracePeriodDecrementer = new niko_MPC_gracePeriodDecrementer(this);
        entity.addScript(gracePeriodDecrementer);
    }

    public List<CustomCampaignEntityAPI> getSatellites() {
        return orbitalSatellites;
    }

    private FactionAPI getSatelliteFaction() {
        return Global.getSector().getFaction(getSatelliteFactionId());
    }

    public void prepareForGarbageCollection() {
       //approachingFleetChecker.prepareForGarbageCollection();
        gracePeriodDecrementer.prepareForGarbageCollection();

        entity = null;
        orbitalSatellites = null;
        satelliteBarrages = null;
        gracePeriods = null;

        for (CampaignFleetAPI fleet : satelliteFleets) {
            niko_MPC_fleetUtils.safeDespawnFleet(fleet, false);
        }
        satelliteFleets = null;

        if (getDummyFleet() != null) {
            niko_MPC_fleetUtils.safeDespawnFleet(getDummyFleet(), true);
            dummyFleet = null;
        }

        getParams().prepareForGarbageCollection();
        params = null;

        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
        tracker.removeParamsFromAllBattles(this);
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
    
    /**
    * Updates the factionId, by syncing it with the market, or setting it to derelict
    * if the market is uncolonized.
    * Updates the faction of all satellite entities.
    */
    public void updateFactionId() { 
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return;
        
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
            setSatelliteId(entity.getFaction().getId());
        }
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
        addFleetRefToGracePeriodsIfNonePresent(fleet);
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
            addOrbitPointingDownWithRelativeOffset(satellite, entity, orbitAngle, getParams().satelliteOrbitDistance);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     *
     * @param satellite The satellite to remove.
     */
    public void removeSatellite(CustomCampaignEntityAPI satellite, boolean regenerateOrbit, boolean removeFromList) {
        getSatellites().remove(satellite);
        Misc.fadeAndExpire(satellite);
        satellite.getContainingLocation().removeEntity(satellite);

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
        addOrbitPointingDownWithRelativeOffset(satellite, getEntity(), 0, params.satelliteOrbitDistance); //set up the orbit

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
        BattleAPI.BattleSide battleSide = battle.pickSide(dummyFleet);

        return battleSide;
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    public int getMaxPhysicalSatellites() {
        return getParams().maxPhysicalSatellites;
    }

    public int getMaxPhysicalSatellitesBasedOnEntitySize(float radiusDivisor) {
        return ((round((entity.getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    public int getMaxBattleSatellites() {
        return getParams().maxBattleSatellites;
    }

    /**
     * Instantiates a new dummy fleet is none is present, but ONLY if getSatelliteFaction() doesn't return null.
     * @return the dummyFleet used for things such as targetting and conditional attack logic. Can return a standard
     * satellite fleet if getSatelliteFaction() == null.
     */
    public CampaignFleetAPI getDummyFleetWithUpdate() {
        FactionAPI faction = getSatelliteFaction();
        if (dummyFleet == null) {
            if (faction != null) { // a strange hack i have to do, since this method is called before factions /exist/?
                niko_MPC_fleetUtils.createDummyFleet(this, entity);
            } else {
               return niko_MPC_fleetUtils.spawnSatelliteFleet(this, entity.getLocation(), entity.getContainingLocation());
            }
        }
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
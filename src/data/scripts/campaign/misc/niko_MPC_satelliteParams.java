package data.scripts.campaign.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.everyFrames.niko_MPC_fleetsApproachingSatellitesChecker;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static data.utilities.niko_MPC_debugUtils.logEntityData;

public class niko_MPC_satelliteParams {

    public SectorEntityToken entity;
    public String satelliteId;
    public String satelliteFactionId;
    public int maxPhysicalSatellites;
    public int maxBattleSatellites;

    public float satelliteOrbitDistance;
    public float satelliteInterferenceDistance;
    public float satelliteBarrageDistance;
    public HashMap<String, Float> weightedVariantIds;

    public List<CustomCampaignEntityAPI> orbitalSatellites;
    public List<SectorEntityToken> satelliteBarrages = new ArrayList<>();
    public HashMap<CampaignFleetAPI, Float> gracePeriods = new HashMap<>();

    public niko_MPC_fleetsApproachingSatellitesChecker approachingFleetChecker;

    public niko_MPC_gracePeriodDecrementer gracePeriodDecrementer;
    public List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();
    private String satelliteFleetName;
    private CampaignFleetAPI dummyFleet;

    public niko_MPC_satelliteParams(SectorEntityToken entity, String satelliteId, String satelliteFactionId, String satelliteFleetName,
                                    int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                    HashMap<String, Float> weightedVariantIds) {

        this(entity, satelliteId, satelliteFactionId, satelliteFleetName, maxPhysicalSatellites, maxBattleSatellites,
                satelliteOrbitDistance, satelliteInterferenceDistance, barrageDistance, weightedVariantIds,
                new ArrayList<CustomCampaignEntityAPI>());
    }

    public niko_MPC_satelliteParams(SectorEntityToken entity, String satelliteId, String satelliteFactionId, String satelliteFleetName,
                                    int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                    HashMap<String, Float> weightedVariantIds, List<CustomCampaignEntityAPI> orbitalSatellites) {
        this.entity = entity;
        this.satelliteId = satelliteId;
        this.satelliteFactionId = satelliteFactionId;
        this.satelliteFleetName = satelliteFleetName;
        this.maxPhysicalSatellites = maxPhysicalSatellites;
        this.maxBattleSatellites = maxBattleSatellites;
        this.satelliteOrbitDistance = satelliteOrbitDistance;
        this.satelliteInterferenceDistance = satelliteInterferenceDistance;
        this.satelliteBarrageDistance = barrageDistance;
        this.weightedVariantIds = weightedVariantIds;

        this.orbitalSatellites = orbitalSatellites;

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
        return Global.getSector().getFaction(satelliteFactionId);
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
        setSatelliteId(factionId, true)
    }

    /**
     * Sets the faction ID of the params to factionId. Optionally updates the faction of all satellite entities.
     * @param factionId The factionId to set.
     * @param withUpdate If true, sets the faction of all satellite entities to the new faction id.
     */
    public void setSatelliteId(String factionId, boolean withUpdate) {
        satelliteFactionId = factionId;

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
        return satelliteFactionId;
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the factionId of the params it is called on.
     * @return A faction ID, in string form. Can return null if entity has no satellites.
     */
    public String getCurrentSatelliteFactionId() {
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return null;
        
        updateFactionId();
        return getSatelliteFactionId();
    }
    
    /**
    * Updates the factionId, by syncing it with the market, or setting it to derelict
    * if the market is uncolonized.
    * Updates the faction of all satellite entities.
    */
    public void updateFactionId() { 
        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(entity)) return;
        
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
        return satelliteFleetName;
    }

    /**
    * Should be called whenever a new non-dummy satellite fleet is created.
    */
    public void newSatellite(CampaignFleetAPI satelliteFleet) {
        satelliteFleets.add(satelliteFleet);
    }

    public float getSatelliteInterferenceDistance() {
        return satelliteInterferenceDistance;
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

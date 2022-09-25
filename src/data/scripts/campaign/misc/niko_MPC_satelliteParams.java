package data.scripts.campaign.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import data.scripts.everyFrames.niko_MPC_fleetsApproachingSatellitesChecker;
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    public FactionAPI getSatelliteFaction() {
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

    public void setSatelliteId(String factionId) {
        satelliteFactionId = factionId;

        for (CampaignFleetAPI fleet : satelliteFleets) {
            fleet.setFaction(satelliteFactionId);
        }

        for (CustomCampaignEntityAPI satellite : orbitalSatellites) {
            satellite.setFaction(satelliteFactionId);
        }

        if (dummyFleet != null) {
            dummyFleet.setFaction(satelliteFactionId);
        }
    }

    public String getSatelliteFactionId() {
        return satelliteFactionId;
    }

    public HashMap<CampaignFleetAPI, Float> getGracePeriods() {
        return gracePeriods;
    }

    public float getGracePeriod(CampaignFleetAPI fleet) {
        addFleetRefToGracePeriodsIfNonePresent(fleet);
        return getGracePeriods().get(fleet);
    }

    public void adjustGracePeriod(CampaignFleetAPI fleet, float amount) {
        addFleetRefToGracePeriodsIfNonePresent(fleet);
        getGracePeriods().put(fleet, Math.max(0, getGracePeriods().get(fleet) + amount));
    }

    private void addFleetRefToGracePeriodsIfNonePresent(CampaignFleetAPI fleet) {
        if (getGracePeriods().get(fleet) == null) {
            gracePeriods.put(fleet, 0f); // we dont use a amount arg here because we only exist here to initialize a new entry
        }
    }

    public String getSatelliteFleetName() {
        return satelliteFleetName;
    }

    public void newSatellite(CampaignFleetAPI satelliteFleet) {
        satelliteFleets.add(satelliteFleet);
    }

    public float getSatelliteInterferenceDistance() {
        return satelliteInterferenceDistance;
    }


    public CampaignFleetAPI getDummyFleetWithUpdate() {
        FactionAPI faction = getSatelliteFaction();
        if (dummyFleet == null) {
            if (faction != null) { // a strange hack i have to do, since this method is called before factions /exist/?
                dummyFleet = niko_MPC_fleetUtils.createDummyFleet(this, entity);
            } else {
               return niko_MPC_fleetUtils.spawnSatelliteFleet(this, entity.getLocation(), entity.getContainingLocation());
            }
        }
        return dummyFleet;
    }

    public boolean dummyFleetWantsToFight(CampaignFleetAPI fleet) {
        CampaignFleetAPI dummy = getDummyFleetWithUpdate();
        dummy.setFaction(niko_MPC_satelliteUtils.getCurrentSatelliteFactionId(this));

        return dummy.isHostileTo(fleet);
    }
}

package data.scripts.campaign.misc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class niko_MPC_satelliteParams {

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
    public float gracePeriod = 0f;

    public EveryFrameScript gracePeriodDecrementer;

    public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                    HashMap<String, Float> weightedVariantIds) {
        this(satelliteId, satelliteFactionId, maxPhysicalSatellites, maxBattleSatellites, satelliteOrbitDistance, satelliteInterferenceDistance, barrageDistance, weightedVariantIds, new ArrayList<CustomCampaignEntityAPI>());
    }

    public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance, float barrageDistance,
                                    HashMap<String, Float> weightedVariantIds, List<CustomCampaignEntityAPI> orbitalSatellites) {
        this.satelliteId = satelliteId;
        this.satelliteFactionId = satelliteFactionId;
        this.maxPhysicalSatellites = maxPhysicalSatellites;
        this.maxBattleSatellites = maxBattleSatellites;
        this.satelliteOrbitDistance = satelliteOrbitDistance;
        this.satelliteInterferenceDistance = satelliteInterferenceDistance;
        this.satelliteBarrageDistance = barrageDistance;
        this.weightedVariantIds = weightedVariantIds;

        this.orbitalSatellites = orbitalSatellites;
    }

    public List<CustomCampaignEntityAPI> getSatellites() {
        return orbitalSatellites;
    }

    public FactionAPI getSatelliteFaction() {
        return Global.getSector().getFaction(satelliteFactionId);
    }

    public void prepareForGarbageCollection() {
        orbitalSatellites = null;
        satelliteBarrages = null;
        gracePeriod = 0;
    }

    public void setSatelliteId(String factionId) {
        satelliteFactionId = factionId;
    }

    public String getSatelliteFactionId() {
        return satelliteFactionId;
    }

    public float getGracePeriod() {
        return gracePeriod;
    }

    public void adjustGracePeriod(float amount) {
        gracePeriod += amount;
    }
}

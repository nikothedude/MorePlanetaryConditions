package data.scripts.campaign.misc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;

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
    public HashMap<String, Float> weightedVariantIds;

    public List<CustomCampaignEntityAPI> orbitalSatellites;

    public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance,
                                    HashMap<String, Float> weightedVariantIds) {
        this(satelliteId, satelliteFactionId, maxPhysicalSatellites, maxBattleSatellites, satelliteOrbitDistance, satelliteInterferenceDistance, weightedVariantIds, new ArrayList<CustomCampaignEntityAPI>());
    }

    public niko_MPC_satelliteParams(String satelliteId, String satelliteFactionId, int maxPhysicalSatellites, int maxBattleSatellites,
                                    float satelliteOrbitDistance, float satelliteInterferenceDistance,
                                    HashMap<String, Float> weightedVariantIds, List<CustomCampaignEntityAPI> orbitalSatellites) {
        this.satelliteId = satelliteId;
        this.satelliteFactionId = satelliteFactionId;
        this.maxPhysicalSatellites = maxPhysicalSatellites;
        this.maxBattleSatellites = maxBattleSatellites;
        this.satelliteOrbitDistance = satelliteOrbitDistance;
        this.satelliteInterferenceDistance = satelliteInterferenceDistance;
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
    }

    public void setSatelliteId(String factionId) {
        satelliteFactionId = factionId;
    }

    public String getSatelliteFactionId() {
        return satelliteFactionId;
    }
}

package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_satelliteUtils;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.util.List;

public class niko_MPC_fleetsApproachingSatellitesChecker implements EveryFrameScriptWithCleanup {

    public SectorEntityToken entity;
    public boolean done = false;
    public niko_MPC_satelliteParams satelliteParams;

    public niko_MPC_fleetsApproachingSatellitesChecker(niko_MPC_satelliteParams satelliteParams, SectorEntityToken entity) {
        this.satelliteParams = satelliteParams;
        this.entity = entity;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        List<CampaignFleetAPI> hostileFleetsWithinInterferenceDistance = CampaignUtils.getNearbyHostileFleets(getEntity(), getSatelliteParams().getSatelliteInterferenceDistance());
        for (CampaignFleetAPI fleet : hostileFleetsWithinInterferenceDistance) {
            if (niko_MPC_satelliteUtils.areEntitySatellitesCapableOfBlocking(entity, fleet)) {
                if (fleet.getInteractionTarget() == entity) {
                    niko_MPC_satelliteUtils.makeEntitySatellitesEngageFleet(entity, fleet);
                }
            }
        }
    }

    public SectorEntityToken getEntity() {
        return entity;
    }

    public niko_MPC_satelliteParams getSatelliteParams() {
        return satelliteParams;
    }

    public void prepareForGarbageCollection() {
        satelliteParams.approachingFleetChecker = null;

        satelliteParams = null;

        done = true;
    }
}

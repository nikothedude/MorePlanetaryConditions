package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.utilities.niko_MPC_satelliteUtils;

public class niko_MPC_fleetReachedEntityChecker implements EveryFrameScriptWithCleanup {

    public boolean done = false;
    public CampaignFleetAPI fleet;

    public niko_MPC_fleetReachedEntityChecker(CampaignFleetAPI fleet) {
        this.fleet = fleet;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
    }

    private void prepareForGarbageCollection() {
        if (fleet != null) {
            fleet.removeScript(this);
            fleet = null;
        }
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
        if (fleet.getBattle() != null) {
            niko_MPC_satelliteUtils.makeNearbyEntitySatellitesEngageFleet(fleet);
        }
        prepareForGarbageCollection();
    }
}

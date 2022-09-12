package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_fleetUtils;

public class niko_MPC_temporarySatelliteFleetDespawner implements EveryFrameScriptWithCleanup {

    public CampaignFleetAPI fleet;
    public niko_MPC_satelliteParams params;
    public int grace = 1;

    public boolean done = false;

    public niko_MPC_temporarySatelliteFleetDespawner(CampaignFleetAPI fleet, niko_MPC_satelliteParams params) {
        this.fleet = fleet;
        this.params = params;
    }

    @Override
    public void cleanup() {
        prepareForGarbageCollection();
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
        if (grace != 0) {
            grace -= 1;
            return;
        }

        if (fleet.getBattle() == null) {
            getRidOfFleet();
            prepareForGarbageCollection();
            return;
        }
        else {
            if (!params.getInfluencedBattles().contains(fleet.getBattle())) {
                params.influencedBattles.add(fleet.getBattle());
            }
        }
    }

    private void prepareForGarbageCollection() {

        if (fleet != null) {
            fleet.removeScript(this);
            fleet = null;
        }

        done = true;
    }

    private void getRidOfFleet() {
        niko_MPC_fleetUtils.safeDespawnFleet(fleet);
    }
}

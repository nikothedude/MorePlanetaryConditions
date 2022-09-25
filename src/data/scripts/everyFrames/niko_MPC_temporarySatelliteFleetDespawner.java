package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

public class niko_MPC_temporarySatelliteFleetDespawner implements EveryFrameScriptWithCleanup {

    public CampaignFleetAPI fleet;
    public niko_MPC_satelliteParams params;
    public int grace = 1;

    public boolean done = false;

    public double advanceTimeSinceStart = 0;

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
        advanceTimeSinceStart += amount;

        if (fleet.getBattle() == null) {
            getRidOfFleet();
            prepareForGarbageCollection();
            return;
        }
        else {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            BattleAPI battle = fleet.getBattle();
            if (!tracker.areSatellitesInvolvedInBattle(battle, params)) { //params is supposed to hold a ref to the battles of all fleets
                tracker.associateSatellitesWithBattle(battle, params, battle.pickSide(fleet));
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
        boolean vanish = (advanceTimeSinceStart < 90); //arbitrary number
        niko_MPC_fleetUtils.safeDespawnFleet(fleet, vanish);
    }
}

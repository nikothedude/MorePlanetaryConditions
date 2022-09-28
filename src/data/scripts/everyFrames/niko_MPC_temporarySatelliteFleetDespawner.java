package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.*;

public class niko_MPC_temporarySatelliteFleetDespawner implements EveryFrameScriptWithCleanup {

    public CampaignFleetAPI fleet;
    public niko_MPC_satelliteHandler params;
    public int grace = 1;

    public boolean done = false;

    public double advanceTimeSinceStart = 0;

    public niko_MPC_temporarySatelliteFleetDespawner(CampaignFleetAPI fleet, niko_MPC_satelliteHandler params) {
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
            getRidOfFleet(); //this despawns the fleet, which calls the listener, which calls prepareforgc. its weird
            return;
        }
        else {
            niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();
            BattleAPI battle = fleet.getBattle();
            if (!tracker.areSatellitesInvolvedInBattle(battle, params)) { // sanity
                tracker.associateSatellitesWithBattle(battle, params, battle.pickSide(fleet));
            }
        }
    }

    public void prepareForGarbageCollection() {

        if (fleet != null) {
            niko_MPC_memoryUtils.deleteMemoryKey(fleet.getMemoryWithoutUpdate(), niko_MPC_ids.temporaryFleetDespawnerId);
            fleet.removeScript(this);
            fleet = null;
        }
        params = null;
        done = true;
    }


    private void getRidOfFleet() {
        boolean vanish = (advanceTimeSinceStart < 1); //arbitrary number
        niko_MPC_fleetUtils.despawnSatelliteFleet(fleet, vanish);
    }
}

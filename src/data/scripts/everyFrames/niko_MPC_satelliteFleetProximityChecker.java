package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.niko_MPC_fleetUtils;
import org.lazywizard.lazylib.campaign.CampaignUtils;

import java.util.List;

public class niko_MPC_satelliteFleetProximityChecker implements EveryFrameScriptWithCleanup {

    public SectorEntityToken entity;
    public boolean done = false;
    public niko_MPC_satelliteHandler satelliteHandler;
    private float deltaTime = 0f;

    public niko_MPC_satelliteFleetProximityChecker(niko_MPC_satelliteHandler handler, SectorEntityToken entity) {
        this.satelliteHandler = handler;
        this.entity = entity;
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
        //CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        //if (playerFleet.getContainingLocation() != entity.getContainingLocation()) return;

        // using deltatime because i want a bit more performance
        // its not like we need to run EVERY frame, only enough
        deltaTime += amount;
        float thresholdForAdvancement = 0.2f;
        if (deltaTime < thresholdForAdvancement) {
            return;
        } else {
            deltaTime = 0;
        }

        List<CampaignFleetAPI> fleetsWithinInterferenceDistance = CampaignUtils.getNearbyFleets(entity, satelliteHandler.getSatelliteInterferenceDistance());
        for (CampaignFleetAPI fleet : fleetsWithinInterferenceDistance) {
            if (fleet == null) continue; //literally 0 idea how this can be null but okay starsector
            if (fleet == Global.getSector().getPlayerFleet()) continue;
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) continue;
            BattleAPI battle = (fleet.getBattle());
            if (battle == null) continue;

            if (satelliteHandler.areSatellitesCapableOfBlocking(fleet)) {
                if (satelliteHandler.getSideForBattle(battle) != BattleAPI.BattleSide.NO_JOIN) {
                    satelliteHandler.makeEntitySatellitesEngageFleet(fleet);
                }
            }
        }
    }
    public void prepareForGarbageCollection () {
        entity = null;

        if (satelliteHandler != null) {
            satelliteHandler.satelliteFleetProximityChecker = null;
            satelliteHandler = null;
        }

        done = true;
    }
}

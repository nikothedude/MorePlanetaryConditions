package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScriptWithCleanup;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore;
import data.utilities.niko_MPC_fleetUtils;

public class niko_MPC_satelliteBattleCheckerForStation implements EveryFrameScriptWithCleanup {

    public MarketAPI market;
    public boolean done = false;
    public niko_MPC_satelliteHandlerCore satelliteHandler;
    private float deltaTime = 0f;

    public niko_MPC_satelliteBattleCheckerForStation(niko_MPC_satelliteHandlerCore handler, MarketAPI market) {
        this.satelliteHandler = handler;
        this.market = market;

        init();
    }

    private void init() {
        if (market == null) {
            prepareForGarbageCollection();
        }
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
        // using deltatime because i want a bit more performance
        // its not like we need to run EVERY frame, only enough
        deltaTime += amount;
        float thresholdForAdvancement = 0.2f;
        if (deltaTime < thresholdForAdvancement) {
            return;
        } else {
            deltaTime = 0;
        }

        for (SectorEntityToken possibleStation : market.getConnectedEntities()) {
            if (possibleStation == null) {
                continue;
            }
            CampaignFleetAPI stationFleet = (CampaignFleetAPI) possibleStation.getMemoryWithoutUpdate().get(MemFlags.STATION_FLEET);
            if (stationFleet != null) { //if its null, its defeated
                BattleAPI battle = stationFleet.getBattle();
                if (battle != null) {
                    for (CampaignFleetAPI otherFleet : battle.getOtherSideFor(stationFleet)) {
                        if (niko_MPC_fleetUtils.isFleetValidEngagementTarget(otherFleet)) {
                            satelliteHandler.makeEntitySatellitesEngageFleet(otherFleet);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void prepareForGarbageCollection() {
        market = null;

        if (satelliteHandler != null) {
            satelliteHandler.entityStationBattleChecker = null;
            satelliteHandler = null;
        }

        done = true;
    }
}

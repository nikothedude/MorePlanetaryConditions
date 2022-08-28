package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.ArrayList;

import static data.utilities.niko_MPC_planetUtils.getSatellitesInOrbitOfMarket;
import static data.utilities.niko_MPC_satelliteUtils.removeSatellitesFromMarket;

public class niko_MPC_satelliteTrackerScript implements EveryFrameScript {
    private boolean done = false;
    public MarketAPI holder;

    public ArrayList<MarketAPI> marketsWithSatellites = new ArrayList<>();

    public niko_MPC_satelliteTrackerScript(MarketAPI holder) {
        this.holder = holder;
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
        if (holder == null) { //theres a chance our market gets deleted without our knowledge
            done = true;
            Global.getSector().removeScript(this);
            return;
        }

        if (!(holder.hasCondition("niko_MPC_antiAsteroidSatellites"))) {
            removeSelfAndSatellites();
        }
    }

    public void removeSelfAndSatellites() {
        removeSatellitesFromMarket(holder, getSatellitesInOrbitOfMarket(holder));
        Global.getSector().removeScript(this);
    }

    public MarketAPI getHolder() {
        return holder;
    }

}

package data.scripts.everyFrames;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.utilities.niko_MPC_satelliteUtils;
import jdk.jfr.Experimental;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.*;

import static data.utilities.niko_MPC_planetUtils.getSatellitesInOrbitOfMarket;
import static data.utilities.niko_MPC_satelliteUtils.removeSatellitesFromMarket;

public class niko_MPC_satelliteTrackerScript implements EveryFrameScript {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteTrackerScript.class);

    static {
        log.setLevel(Level.ALL);
    }

    /**
     * Every single market that supposedly has satellites in orbit. Is iterated through every advance() call.
     */
    public HashSet<MarketAPI> marketsWithSatellites;

    public niko_MPC_satelliteTrackerScript() {
        this(new HashSet<MarketAPI>()); //default value is an empty hashset
    }

    public niko_MPC_satelliteTrackerScript(HashSet<MarketAPI> marketsWithSatellites) {
        this.marketsWithSatellites = marketsWithSatellites;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        marketsWithSatellites.removeAll(Collections.singleton(null)); //sanity, todo: come back to this, see if a better solution can be found, and if its even a problem
        Set<MarketAPI> marketsWithSatellitesCopy = new HashSet<>(getMarketsWithSatellites()); //required to avoid concurrentmodificationexception
        for (MarketAPI market : marketsWithSatellitesCopy) { //check each market that supposedly has the condition

            log.debug(market.getId() + " iterated");

            if (!(market.hasCondition("niko_MPC_antiAsteroidSatellites"))) { //if it doesnt have the condition, //fixme markets without this condition have it in their conditions var
                removeSatellitesFromMarketAndRemoveFromList(market); //remove it and it's satellites, and stop checking it until the condition is reapplied

            //todo: handle satellite status
            }
        }
    }

    public void removeSatellitesFromMarketAndRemoveFromList(MarketAPI market) {
        removeSatellitesFromMarket(market, getSatellitesInOrbitOfMarket(market));
        getMarketsWithSatellites().remove(market);
    }

    @Getter
    public HashSet<MarketAPI> getMarketsWithSatellites() {
        return marketsWithSatellites;
    }


}

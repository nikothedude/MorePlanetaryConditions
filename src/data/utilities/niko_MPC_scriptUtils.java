package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;

import java.util.List;

import static data.utilities.niko_MPC_ids.satelliteTrackerId;
import static data.utilities.niko_MPC_satelliteUtils.getSatellitesInOrbitOfMarket;

public class niko_MPC_scriptUtils {

    public static void addScriptIfScriptIsUnique(SectorEntityToken entity, EveryFrameScript script) { //todo: maybe make an alternate type of script that has an "id" var
        if (!(entity.hasScriptOfClass(script.getClass()))) { //todo: might be able to implement this better by passing a class instead
            entity.addScript(script);
        }
    }

    /**
     * Adds a new niko_MPC_satelliteTrackerScript to market, but ONLY if the market does not already have a tracker instance.
     * By default, passes the market's satellite memory list to the script.
     *
     * @param market The market to add the script to.
     */
    public static void addSatelliteTrackerIfNoneIsPresent(MarketAPI market, SectorEntityToken entity,
                                                          int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        addSatelliteTrackerIfNoneIsPresent(market, entity, getSatellitesInOrbitOfMarket(market), maxPhysicalSatellites, satelliteId, satelliteFactionId);
    }

    /**
     * Adds a new niko_MPC_satelliteTrackerScript to market, but ONLY if the market does not already have a tracker instance.
     * @param market The market to add the script to.
     * @param satellites The satellites list to pass to the script.
     */
    public static void addSatelliteTrackerIfNoneIsPresent(MarketAPI market, SectorEntityToken entity, List<CustomCampaignEntityAPI> satellites,
                                                             int maxPhysicalSatellites, String satelliteId, String satelliteFactionId) {
        if (!(marketHasSatelliteTracker(market))) { // if the script isn't already present,
            niko_MPC_satelliteTrackerScript script = (new niko_MPC_satelliteTrackerScript(market, entity, satellites, maxPhysicalSatellites, satelliteId, satelliteFactionId)); //make a new one,
            //...then we should add a new tracker for satellites
            addNewSatelliteTracker(market, script); //todo: methodize
            entity.addScript(script); ///fixme < this is causing an error.......?
        }
    }

    public static void addNewSatelliteTracker(MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        niko_MPC_satelliteTrackerScript oldScript = getInstanceOfSatelliteTracker(market);
        assert(oldScript == null); //we should NEVER be replacing an existing script, since the framework isnt set up for that

        setInstanceOfSatelliteTracker(market, script);
    }

    public static boolean marketHasSatelliteTracker(MarketAPI market) {
        return (!(getInstanceOfSatelliteTracker(market) == null));
    }

    @Getter
    public static niko_MPC_satelliteTrackerScript getInstanceOfSatelliteTracker(MarketAPI market) {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        return (niko_MPC_satelliteTrackerScript) marketMemory.get(satelliteTrackerId);
    }

    /**
     * Performs market.getMemoryWithoutUpdate().set(satelliteTrackerId, script). Raw setter, does not have any checks.
     * @param market The market that will hold the script.
     * @param script The script to be applied.
     */
    @Setter
    public static void setInstanceOfSatelliteTracker(MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        marketMemory.set(satelliteTrackerId, script);
    }

}

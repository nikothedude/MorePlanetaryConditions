package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.HashMap;
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
     * Also checks to see if the entity the market is held by has a satellite script. This is useful for when a market
     * changes on a planet-we don't want to add two scripts. The script itself handles migrating between markets.
     * By default, passes the market's satellite memory list to the script.
     *
     * @param market The market to add the script to.
     */
    public static void addSatelliteTrackerIfNoneIsPresent(MarketAPI market, SectorEntityToken entity,
                                                          int maxPhysicalSatellites, String satelliteId, String satelliteFactionId, HashMap<String, Float> variantIds) {
        addSatelliteTrackerIfNoneIsPresent(market, entity, getSatellitesInOrbitOfMarket(market), maxPhysicalSatellites, satelliteId, satelliteFactionId, variantIds);
    }

    /**
     * Adds a new niko_MPC_satelliteTrackerScript to market, but ONLY if the market does not already have a tracker instance.
     * Also checks to see if the entity the market is held by has a satellite script. This is useful for when a market
     * changes on a planet-we don't want to add two scripts. The script itself handles migrating between markets.
     * @param market The market to add the script to.
     * @param satellites The satellites list to pass to the script.
     */
    public static void addSatelliteTrackerIfNoneIsPresent(MarketAPI market, SectorEntityToken entity, List<CustomCampaignEntityAPI> satellites,
                                                             int maxPhysicalSatellites, String satelliteId, String satelliteFactionId, HashMap<String, Float> variantIds) {

        if (!(marketHasSatelliteTracker(market)) && (!entity.hasScriptOfClass(niko_MPC_satelliteTrackerScript.class))) { // if the script isn't already present,
            niko_MPC_satelliteTrackerScript script = (new niko_MPC_satelliteTrackerScript(market, entity, satellites, maxPhysicalSatellites, satelliteId, satelliteFactionId, variantIds)); //make a new one,
            //...then we should add a new tracker for satellites
            addNewSatelliteTracker(market, script); //todo: methodize
            script.updateSatelliteStatus(true); //todo: maybe put this in a better plcae
            entity.addScript(script);
        }
    }

    /**
     * If the market doesnt already have a script, add the given one.
     * @param market The market to add to.
     * @param script The script to add.
     */
    public static void addNewSatelliteTracker(MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        niko_MPC_satelliteTrackerScript oldScript = getInstanceOfSatelliteTracker(market);
        assert(oldScript == null); //we should NEVER be replacing an existing script, since the framework isnt set up for that

        setInstanceOfSatelliteTracker(market, script);
    }

    /**
     * @param market The market to check.
     * @return true if the market has an instance of a satellite tracker.
     */
    public static boolean marketHasSatelliteTracker(MarketAPI market) {
        return (!(getInstanceOfSatelliteTracker(market) == null));
    }

    /**
     * @param market The market to get the satellite tracker from.
     * @return an instance of niko_MPC_satelliteTrackerScript, but can return null if the market has no script.
     */
    public static niko_MPC_satelliteTrackerScript getInstanceOfSatelliteTracker(MarketAPI market) {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        return (niko_MPC_satelliteTrackerScript) marketMemory.get(satelliteTrackerId);
    }

    /**
     * Performs market.getMemoryWithoutUpdate().set(satelliteTrackerId, script). Raw setter, does not have any checks.
     * @param market The market that will hold the script.
     * @param script The script to be applied.
     */
    public static void setInstanceOfSatelliteTracker(MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        marketMemory.set(satelliteTrackerId, script);
    }


}

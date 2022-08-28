package data.utilities;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.HashSet;

import static data.utilities.niko_MPC_planetUtils.getMarketsWithSatellites;
import static data.utilities.niko_MPC_satelliteUtils.getInstanceOfSatelliteTracker;
import static data.utilities.niko_MPC_satelliteUtils.setInstanceOfSatelliteTracker;

public class niko_MPC_scriptUtils {

    public static void addScriptIfScriptIsUnique(SectorEntityToken entity, EveryFrameScript script) { //todo: maybe make an alternate type of script that has an "id" var
        if (!(entity.hasScriptOfClass(script.getClass()))) { //todo: might be able to implement this better by passing a class instead
            entity.addScript(script);
        }
    }

    public static void addNewSatelliteTracker(niko_MPC_satelliteTrackerScript script) {
        niko_MPC_satelliteTrackerScript oldScript = getInstanceOfSatelliteTracker();
        //if ((oldScript != null)) {} unneeded
        assert(oldScript == null); //we should NEVER be replacing an existing script, since the framework isnt set up for that

        setInstanceOfSatelliteTracker(script);
    }

    public static void addSatelliteTrackerIfNoneIsPresent() {
        if ((getInstanceOfSatelliteTracker()) == null) { // if the script isn't already present,
            HashSet<MarketAPI> marketsWithSatellites = new HashSet<>();
            niko_MPC_satelliteTrackerScript script = (new niko_MPC_satelliteTrackerScript(marketsWithSatellites));
            //...then we should add a new tracker for satellites
            addNewSatelliteTracker(script); //todo: methodize
            SectorAPI sector = Global.getSector();
            sector.addScript(script);
        }
    }

    /**
     * Destroys the current marketsWithSatellites list then creates a brand new one.
     * Should be mostly unnecessary for anything except gameload.
     */
    public static void updateSatelliteTrackerMarkets() {
        HashSet<MarketAPI> marketsWithSatellites = getInstanceOfSatelliteTracker().getMarketsWithSatellites();
        marketsWithSatellites.clear(); //first we purge the current list of markets
        marketsWithSatellites.addAll(getMarketsWithSatellites()); //then we regenerate it
    }

    public static void addSatelliteTrackerIfNoneIsPresentAndUpdate() {
        addSatelliteTrackerIfNoneIsPresent();
        updateSatelliteTrackerMarkets();
    }

}

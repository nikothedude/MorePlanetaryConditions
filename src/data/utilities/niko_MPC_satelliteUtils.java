package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import data.scripts.util.MagicCampaign;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset;
import static data.utilities.niko_MPC_planetUtils.getOptimalOrbitalOffsetForSatellites;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_satelliteUtils {

    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void handleSatelliteStatusOfMarket(MarketAPI market) { //placeholder name
        return; //todo: placeholder

    }

    //////////////////////////
    //                      //
    // ADDITION AND REMOVAL //
    //                      //
    //////////////////////////
    /**
     * Adds a new satellite to the market, orbitting around the primaryentity of the market.
     * @param market The market to add the satellite to.
     * @param id The id to be assigned to the satellite.
     * @param faction The factionId to be given to the satellite.
     */
    public static void addSatellite(MarketAPI market, String id, String faction) {
        addSatellite(market, true, id, faction);
    }

    /**
     * Adds a new satellite to the market, orbitting around the primaryentity of the market.
     * @param market The market to add the satellite to.
     * @param regenerateOrbit If true, regenerateOrbitSpacing(market) will be ran.
     * @param id The id to be assigned to the satellite.
     * @param faction The factionId to be given to the satellite.
     */
    public static void addSatellite(MarketAPI market, boolean regenerateOrbit, String id, String faction) {
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfMarket(market); //first, we get the satellites

        int satelliteNumber = ((satellitesInOrbit.size()) + 1);
        String orderedId = (id + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc

        StarSystemAPI system = market.getStarSystem();
        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = system.addCustomEntity(orderedId, null, id, faction);
        addOrbitPointingDownWithRelativeOffset(satellite, market.getPrimaryEntity()); //set up the orbit

        satellitesInOrbit.add(satellite);

        if (regenerateOrbit)
            regenerateOrbitSpacing(market);
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to market through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     * @param market Market to add satellites to.
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id The id to be assigned to the satellites.
     * @param faction The factionid to be given to the satellites.
     */
    public static void addSatellitesToMarket(MarketAPI market, int amountOfSatellitesToAdd, String id, String faction) {
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(market, false, id, faction);
        }
        regenerateOrbitSpacing(market); //only needs to be done once, after all the satellites are added, this does not generate the error
    }

    /**
     * Removes all satellites orbiting this market.
     * @param market The target market.
     */
    public static void removeSatellitesFromMarket(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfMarket(market);
        removeSatellitesFromMarket(market, satellitesInOrbit.size());
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from market's orbit. Will end execution early if the list becomes empty.
     * @param market The market to remove the satellite from.
     * @param amountOfSatellitesToRemove The amount of satellites to remove from market.
     */
    public static void removeSatellitesFromMarket(MarketAPI market, int amountOfSatellitesToRemove) {
        List<CustomCampaignEntityAPI> satellites = getSatellitesInOrbitOfMarket(market);

        Iterator<CustomCampaignEntityAPI> iterator = satellites.iterator();
        for(int i = amountOfSatellitesToRemove; ((i > 0) && (iterator.hasNext())); i--) { //fixme: this might not be stable, i dont know
            CustomCampaignEntityAPI satellite = iterator.next();
            removeSatellite(satellite); //we cant directly modify the list, hence why we use the straight removal method here
            iterator.remove(); // and run iterator.remove
        }
        regenerateOrbitSpacing(market);
    }

    /**
     * Runs removeSatellite(satellite), before removing the satellite from the market's satellite orbiting list.
     * @param market The market to remove from.
     * @param satellite The satellite to remove.
     */
    public static void removeSatelliteFromMarket(MarketAPI market, CustomCampaignEntityAPI satellite) {
        removeSatelliteFromMarket(market, satellite, true);
    }

    /**
     * Runs removeSatellite(satellite), before removing the satellite from the market's satellite orbiting list.
     * @param market The market to remove from.
     * @param satellite The satellite to remove.
     * @param regenerateOrbit If true, regenerateOrbitSpacing(market) is called.
     */
    public static void removeSatelliteFromMarket(MarketAPI market, CustomCampaignEntityAPI satellite, boolean regenerateOrbit) {
        removeSatellite(satellite);
        getSatellitesInOrbitOfMarket(market).remove(satellite);
        if (regenerateOrbit) {
            regenerateOrbitSpacing(market);
        }
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     * @param satellite The satellite to remove.
     */
    public static void removeSatellite(CustomCampaignEntityAPI satellite) {
        Misc.fadeAndExpire(satellite);
        satellite.getContainingLocation().removeEntity(satellite);
    }

    /**
     * Does not make a satellite with a facing of NaN work.
     * @param market
     */
    public static void regenerateOrbitSpacing(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbitOfMarket = getSatellitesInOrbitOfMarket(market);

        float optimalOrbitAngleOffset = getOptimalOrbitalOffsetForSatellites(satellitesInOrbitOfMarket); //fixme: does not generate the error
        float orbitAngle = 0;
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc, so its safe to not add a buffer to the calculation in the optimalangle method
        for (CustomCampaignEntityAPI satellite : satellitesInOrbitOfMarket) { //iterates through each orbitting satellite and offsets them
            if (orbitAngle >= 360) {
                if (Global.getSettings().isDevMode()) {
                    Global.getSector().getCampaignUI().addMessage("A satellite on " + market + " was given a orbit offset of " + orbitAngle + "."); //debug code
                log.debug("A satellite on " + market + " was given a orbit offset of " + orbitAngle + ".");
                removeSatelliteFromMarket(market, satellite, false); //we dont want these weirdos overlapping
                }
            }
            addOrbitPointingDownWithRelativeOffset(satellite, market.getPrimaryEntity(), orbitAngle);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
    }
    /**
     * Gets the market's satellite tracker and accesses its satellite list.
     * @param market The market we are searching for satellites.
     * @return Returns a ArrayList, containing all satellite instances orbiting market. Returns an empty arraylist if the satellite tracker is null.
     */
    public static List<CustomCampaignEntityAPI> getSatellitesInOrbitOfMarket(MarketAPI market) {
        niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
        if (script != null) {
            return script.getSatellites();
        }
        return new ArrayList<>();
    }
}

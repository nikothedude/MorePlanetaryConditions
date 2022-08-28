package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static data.utilities.niko_MPC_planetUtils.getOptimalOrbitalAngleForSatellites;
import static data.utilities.niko_MPC_planetUtils.getSatellitesInOrbitOfMarket;

public class niko_MPC_satelliteUtils {

    /**
     * Holds a reference to the single satelliteTrackerScript present in campaign. Only here for purposes of accessing it.
     */
    public static niko_MPC_satelliteTrackerScript satelliteTracker;

    /**
     * A list of all possible satellite condition Ids. PLEASE UPDATE THIS IF YOU ADD A NEW ONE
     */
    public final static List<String> satelliteConditionIds = new ArrayList<>(Arrays.asList("niko_MPC_antiAsteroidSatellites"));

    private static final Logger log = Global.getLogger(niko_MPC_satelliteUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void handleSatelliteStatusOfMarket(MarketAPI market) { //placeholder name
        return; //todo: placeholder

    }

    public static void addSatellitesToMarket(MarketAPI market, int amountOfSatellitesToAdd, String id, String name, String faction) {
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(market, false, id, name, faction);
        }
        regenerateOrbitSpacing(market); //only needs to be done once, after all the satellites are added
    }

    public static void addSatellite(MarketAPI market, String id, String name, String faction) {
        addSatellite(market, true, id, name, faction);
    }

    public static void addSatellite(MarketAPI market, boolean regenerateOrbit, String id, String name, String faction) {
        StarSystemAPI system = market.getStarSystem();

        List<CustomCampaignEntityAPI> satellitesInOrbit; //var instantiated here to save horizontal space
        satellitesInOrbit = getSatellitesInOrbitOfMarket(market);

        int satelliteNumber = ((satellitesInOrbit.size()) + 1); //the number of the satellite we are adding, used for tracking it

        String orderedid = (id + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc

        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = system.addCustomEntity(orderedid, name, id, faction);
        addOrbitAroundSectorEntity(satellite, market.getPrimaryEntity()); //then add the satellite to the planet we are orbiting

        satellitesInOrbit.add(satellite); //returns the same number as appendSatelliteNumberToId

        if (regenerateOrbit)
            regenerateOrbitSpacing(market);
    }

    public static void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity) {// todo: why am i using this when i initialize a satellite? why not just use the offsets instantly instead of regenning?
        addOrbitAroundSectorEntity(satellite, entity, (entity.getCircularOrbitAngle()));
    }

    public static void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity, float orbitAngle) {
        float orbitRadius = (entity.getRadius()); //todo: placeholder math
        float orbitDays = (entity.getCircularOrbitPeriod()); //my understanding is that this method returns how many days it takes for this object to do a complete orbit

        satellite.setCircularOrbitPointingDown(entity, orbitAngle, orbitRadius, orbitDays);
        //todo: pointingdown will require the sprite to be tuned for the cannons and guns and shit to face away from the planet

    }

    /**
     * Removes all satellites orbiting this market.
     * @param market The target market.
     */
    public static void removeSatellitesFromMarket(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfMarket(market);
        removeSatellitesFromMarket(market, satellitesInOrbit);
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from market's orbit, taking satellites from listToUse.
     * @param market The target market.
     * @param listToUse The list from which satellites will be taken.
     */
    public static void removeSatellitesFromMarket(MarketAPI market, List<CustomCampaignEntityAPI> listToUse) {

        List<CustomCampaignEntityAPI> listToUseCopy = new ArrayList<>(listToUse);
        // we make a copy and manipualte the values in this copy to avoid a concurrentmodificationexception
        // the result is hopefully the same, and it seems it is, from testing, although fixme: when satellites are removed, the arraylist still has a size of 1?

        for (CustomCampaignEntityAPI satellite : listToUseCopy) {
            removeSatelliteFromMarket(market, satellite, false);
        }

        regenerateOrbitSpacing(market);
        if (listToUse == market.getMemoryWithoutUpdate().get("$niko_MPC_defenseSatellitesInOrbit")) { //todo: sloppy code, methodize it, also, this should check size of the list instead
            market.getMemoryWithoutUpdate().set("$niko_MPC_defenseSatellitesInOrbit", null); //we need this for reapplying fixme: throws a error. fix it
            market.getMemoryWithoutUpdate().unset("$niko_MPC_defenseSatellitesInOrbit");
        }
    }

    public static void removeSatelliteFromMarket(MarketAPI market, CustomCampaignEntityAPI satellite) {
        removeSatelliteFromMarket(market, satellite, true);
    }

    public static void removeSatelliteFromMarket(MarketAPI market, CustomCampaignEntityAPI satellite, boolean regenerateOrbit) {
        removeSatellite(satellite);
        getSatellitesInOrbitOfMarket(market).remove(satellite);
        if (regenerateOrbit) {
            regenerateOrbitSpacing(market);
        }
    }

    public static void removeSatellite(CustomCampaignEntityAPI satellite) {
        Misc.fadeAndExpire(satellite);
        satellite.getContainingLocation().removeEntity(satellite);
    }

    public static void regenerateOrbitSpacing(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbitOfMarket = getSatellitesInOrbitOfMarket(market);

        float optimalOrbitAngleOffset = getOptimalOrbitalAngleForSatellites(satellitesInOrbitOfMarket);
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
            addOrbitAroundSectorEntity(satellite, market.getPrimaryEntity(), orbitAngle);
            orbitAngle += optimalOrbitAngleOffset; //no matter what, this should end up less than 360 when the final iteration runs
        }
    }
}

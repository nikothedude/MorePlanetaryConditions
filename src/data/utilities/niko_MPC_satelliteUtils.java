package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.lang.Float;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static data.utilities.niko_MPC_generalUtils.deleteMemoryKey;
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

    public static void addSatellitesToMarket(MarketAPI market, int amountOfSatellitesToAdd, String id, String faction) {
        for (int i = 1; i <= amountOfSatellitesToAdd; i++) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(market, false, id, faction);
        }
        regenerateOrbitSpacing(market); //only needs to be done once, after all the satellites are added, this does not generate the error
    }

    public static void addSatellite(MarketAPI market, String id, String faction) {
        addSatellite(market, true, id, faction);
    }

    public static void addSatellite(MarketAPI market, boolean regenerateOrbit, String id, String faction) {
        StarSystemAPI system = market.getStarSystem();

        List<CustomCampaignEntityAPI> satellitesInOrbit; //var instantiated here to save horizontal space
        satellitesInOrbit = getSatellitesInOrbitOfMarket(market);

        int satelliteNumber = ((satellitesInOrbit.size()) + 1); //this does not cause the error

        String orderedId = (id + (" " + satelliteNumber)); // the 1st satellite becomes "id 1", etc

        // instantiate the satellite in the system
        CustomCampaignEntityAPI satellite = system.addCustomEntity(orderedId, null, id, faction); //fixme: < causes the error, its not the null
        addOrbitAroundSectorEntity(satellite, market.getPrimaryEntity()); //DOESNT CAUSE THE ERROR?

        satellitesInOrbit.add(satellite); //returns the same number as appendSatelliteNumberToId

        if (regenerateOrbit)
            regenerateOrbitSpacing(market);
    }

    public static void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity) {// todo: why am i using this when i initialize a satellite? why not just use the offsets instantly instead of regenning?
        addOrbitAroundSectorEntity(satellite, entity, (entity.getCircularOrbitAngle()));
    }

    public static void addOrbitAroundSectorEntity(CustomCampaignEntityAPI satellite, SectorEntityToken entity, float orbitAngle) {
        float orbitRadius = (entity.getRadius()); //todo: placeholder math
        float orbitDays = (orbitRadius/5); //todo: placeholder
        //DO NOT IGNORE THIS COMMENT
        //entity.getCircularOrbitPeriod() will return 0 if the entity does not orbit! THIS WILL CAUSE A JSONEXCEPTION ON SAVE! DO NOT! ENTER 0!

        if (orbitDays <= 0) {
            orbitDays = 1; //we cannot allow a zero or less number, or else saving will fail

            Global.getSector().getCampaignUI().addMessage(
                    "A orbit was created with an orbitdays of <=0. Please inform the mod author and provide them a copy of your starsector.log."
            );

            log.debug("niko_MPC_ERROR: " + satellite + ", orbitting" + entity.getName() + " in the " + entity.getStarSystem().getName() + "system was created with a " + orbitDays + "orbitDays.");
        }

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

        List<CustomCampaignEntityAPI> listToUseCopy = new ArrayList<>(listToUse); //todo: use iterator
        // we make a copy and manipualte the values in this copy to avoid a concurrentmodificationexception
        // the result is hopefully the same, and it seems it is, from testing, although fixme: when satellites are removed, the arraylist still has a size of 1?

        for (CustomCampaignEntityAPI satellite : listToUseCopy) {
            removeSatelliteFromMarket(market, satellite, false);
        }
        regenerateOrbitSpacing(market);
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

    // does not cause the fucking error
    public static void removeSatellite(CustomCampaignEntityAPI satellite) {
       Misc.fadeAndExpire(satellite); //setting to 0 doesnt help
       satellite.getContainingLocation().removeEntity(satellite); //does not cause the error
    }

    /**
     * Does not make a satellite with a facing of NaN work.
     * @param market
     */
    public static void regenerateOrbitSpacing(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbitOfMarket = getSatellitesInOrbitOfMarket(market);

        float optimalOrbitAngleOffset = getOptimalOrbitalAngleForSatellites(satellitesInOrbitOfMarket); //fixme: does not generate the error
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

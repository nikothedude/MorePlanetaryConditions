package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static data.utilities.niko_MPC_generalUtils.instantiateMemoryKeyIfInvalid;
import static data.utilities.niko_MPC_satelliteUtils.satelliteConditionIds;
import static java.lang.Math.round;

public class niko_MPC_planetUtils {

    public static int getNumSatellitesInOrbitOfMarket(MarketAPI market) {
        int numSatellites;

        List<CustomCampaignEntityAPI> satellitesInOrbit = getSatellitesInOrbitOfMarket(market);
        numSatellites = satellitesInOrbit.size();

        return numSatellites;
    }

    /**
     * @param market The market we are searching for satellites.
     * @return Returns a ArrayList, containing all satellite instances orbiting market.
     */
    public static List<CustomCampaignEntityAPI> getSatellitesInOrbitOfMarket(MarketAPI market) {
        List<CustomCampaignEntityAPI> satellitesInOrbit;

        MemoryAPI marketMemory = (market.getMemoryWithoutUpdate());
        instantiateMemoryKeyIfInvalid(marketMemory, "$niko_MPC_defenseSatellitesInOrbit", new ArrayList<CustomCampaignEntityAPI>());

        satellitesInOrbit = (List<CustomCampaignEntityAPI>) (marketMemory.get("$niko_MPC_defenseSatellitesInOrbit"));

        return satellitesInOrbit;
    }

    /**
     * Finds the optimal orbiting offset in degrees between orbiting satellites using exponential decay.
     * Ex. At 1 satellite, the optimal offset is 360 (reduced to 0). At 2, the optimal is 180, so the satellites are 180 degrees apart.
     * At 3, it's 90, so they're 90 degrees, 4, 45, etc.
     *
     * @param satellitesInOrbitOfMarket The satellites themselves.
     * @return The optimal angle offset in degrees between the satellites. Returns float.
     */
    public static float getOptimalOrbitalAngleForSatellites(List<CustomCampaignEntityAPI> satellitesInOrbitOfMarket) {
        int numOfSatellites = satellitesInOrbitOfMarket.size();

        float optimalAngle = (360 / (float) numOfSatellites); //todo: explain the math

        if (optimalAngle == 360) {
            optimalAngle = 0; //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle;
    }

    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity) {
        return getMaxPhysicalSatellitesBasedOnEntitySize(entity, 5);
    }

    public static int getMaxPhysicalSatellitesBasedOnEntitySize(SectorEntityToken entity, float radiusDivisor) {
        return ((round((entity.getRadius()) / radiusDivisor))); // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    public static List<MarketAPI> getMarketsWithSatellites() {
        List<MarketAPI> allMarkets = Global.getSector().getEconomy().getMarketsCopy(); //get all markets
        List<MarketAPI> marketsWithSatellites = new ArrayList<>(); //instantiate an empty list

        for (MarketAPI market : allMarkets) { //iterate through all markets
            List<MarketConditionAPI> marketConditions = market.getConditions(); //get their conditions
            for (MarketConditionAPI marketCondition : marketConditions) { // now, iterate through the conditions
                if (satelliteConditionIds.contains(marketCondition.getId())) { // if the global list of satellite ids contains an id that matches the iterated condition...
                    marketsWithSatellites.add(market); // ...we have satellites! add it!
                }
            }
        }
        return marketsWithSatellites; //here you go! a neat list of markets with satellites. i hope so badly this doesnt break
    }
}

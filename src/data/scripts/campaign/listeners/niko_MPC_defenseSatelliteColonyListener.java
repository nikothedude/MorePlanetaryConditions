package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_defenseSatelliteColonyListener implements ColonyInteractionListener {
    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {
        niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
        if (script != null) {
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

            if (script.satellitesWantToBlockFleet(playerFleet)) {
            }
        }
    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {

    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {

    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {

    }
}

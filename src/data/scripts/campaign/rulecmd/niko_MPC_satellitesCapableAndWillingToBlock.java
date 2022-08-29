package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignPlanet;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_satellitesCapableAndWillingToBlock extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        if (market != null) {
            niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
            if (script != null) {
                CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                return (script.satellitesWantToBlockFleet(playerFleet) && (script.satellitesCapableOfBlockingFleet(playerFleet)));
            }
        }
        return false;
    }
}

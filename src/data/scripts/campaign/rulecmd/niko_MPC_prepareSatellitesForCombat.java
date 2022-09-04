package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_campaignResumedDeleteScript;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_fleetUtils.createNewSatelliteFleet;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_prepareSatellitesForCombat extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        MarketAPI market = dialog.getInteractionTarget().getMarket();
        if (market != null) {
            niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
            if (script == null) return false;
            LocationAPI playerContainingLocation = Global.getSector().getPlayerFleet().getContainingLocation();
            Vector2f playerCoordinates = Global.getSector().getPlayerFleet().getLocation();

            CampaignFleetAPI satelliteFleet = createNewSatelliteFleet(script, playerContainingLocation, playerCoordinates.x, playerCoordinates.y);
            satelliteFleet.addScript(new niko_MPC_campaignResumedDeleteScript(satelliteFleet));
            return true;
        }
        return false;
    }
}

package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import data.scripts.util.MagicCampaign;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.campaign.CampaignUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_dialogUtils.createSatelliteFleetFocus;
import static data.utilities.niko_MPC_fleetUtils.generateTemporarySatelliteFleet;
import static data.utilities.niko_MPC_fleetUtils.spawnTemporarySatelliteFleetsOnFleet;
import static data.utilities.niko_MPC_ids.satelliteConditionIds;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_spawnTemporarySatelliteFleetsOnPlayer extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        boolean setFocus = false;
        if (params.size() > 0) {
            setFocus = params.get(0).getBoolean(memoryMap);
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<CampaignFleetAPI> satelliteFleets = spawnTemporarySatelliteFleetsOnFleet(playerFleet);

        if (satelliteFleets.size() <= 0) {
            return false;
        }
        if (setFocus) {
            createSatelliteFleetFocus(satelliteFleets.get(0), satelliteFleets, dialog, memoryMap);
        }

    return true;
    }
}

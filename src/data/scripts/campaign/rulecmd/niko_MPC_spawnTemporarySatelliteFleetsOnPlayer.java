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
import static data.utilities.niko_MPC_ids.satelliteConditionIds;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_spawnTemporarySatelliteFleetsOnPlayer extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        boolean result = false;
        boolean setFocus = false;
        if (params.size() > 0) {
            setFocus = params.get(0).getBoolean(memoryMap);
        }

        CampaignFleetAPI focusTarget = null;
        List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();

        List<MarketAPI> marketsInSystem = new ArrayList<>();
        SectorEntityToken entity = dialog.getInteractionTarget();
        List<SectorEntityToken> entitiesInSystem = entity.getContainingLocation().getAllEntities(); //todo: maybe switch to planetapi for optimization
        for (SectorEntityToken entityToken : entitiesInSystem) { // could also add these things to a global list or something i dunno
            if (entityToken.getMarket() != null) {
                marketsInSystem.add(entityToken.getMarket());
            }
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        LocationAPI playerLocation = playerFleet.getContainingLocation();
        Vector2f coordinates = playerFleet.getLocation();

        for (MarketAPI market : marketsInSystem) {
            niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
            if (script == null) continue;
            if (MathUtils.isWithinRange(market.getPrimaryEntity(), playerFleet, script.getSatelliteOrbitRadius())) {
                CampaignFleetAPI satelliteFleet = generateTemporarySatelliteFleet(market, playerLocation, coordinates);
                if (setFocus) {
                    focusTarget = satelliteFleet;
                }
                satelliteFleets.add(satelliteFleet);
                result = true;
            }
        }

        if (focusTarget != null) {
            createSatelliteFleetFocus(focusTarget, satelliteFleets, dialog, memoryMap);
        }
        return result;
    }
}

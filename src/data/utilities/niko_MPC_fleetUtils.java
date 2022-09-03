package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.plugins.DModAdderPlugin;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.everyFrames.niko_MPC_campaignResumedDeleteScript;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import data.scripts.util.MagicCampaign;
import org.lazywizard.lazylib.LazyLib;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.fs.starfarer.api.impl.campaign.ids.MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER;
import static data.utilities.niko_MPC_listenerUtils.addCleanupListenerToFleet;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_fleetUtils {

    public static CampaignFleetAPI createSatelliteFleetTemplate(niko_MPC_satelliteTrackerScript script) {
        String factionId = script.getSatelliteFactionId();
        String fleetName = script.getSatelliteFleetName();
        String fleetType = script.getSatelliteFleetType();

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(factionId, fleetName, true);
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(MemFlags.MEMORY_KEY_FLEET_TYPE, fleetType);
        fleetMemory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

        MarketAPI market = script.getMarket();

        if (market != null && !market.getId().equals("fake")) {
            fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_SOURCE_MARKET, market.getId());
        }

        return fleet;
    }

    public static List<FleetMemberAPI> generateNewSatellites(CampaignFleetAPI fleetTemplate, int numShipsToGen, HashMap<String, Float> variantWeights) {
        List<FleetMemberAPI> satellitesToAdd = new ArrayList<>();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

        for (Map.Entry<String, Float> entry : variantWeights.entrySet()) {
            picker.add(entry.getKey(), entry.getValue());
        }

        for (int i = numShipsToGen; i > 0; i--) {
            String pickedVariantId = picker.pick();
            FleetMemberAPI satellite = Global.getFactory().createFleetMember(FleetMemberType.SHIP, pickedVariantId);
            if(satellite!=null) satellitesToAdd.add(satellite);
        }
        return satellitesToAdd;
    }

    public static void attemptToFillFleetWithVariants(int budget, CampaignFleetAPI fleet, HashMap<String, Float> variants) {
        if (budget == 0) {
            return;
        }

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        List<FleetMemberAPI> satellitesToAdd = new ArrayList<>();

        for (Map.Entry<String, Float> entry : variants.entrySet()) {
            picker.add(entry.getKey(), entry.getValue());
        }

        while ((budget > 0) && (!(picker.isEmpty()))) {
            String pickedVariantId = picker.pick();

            ShipVariantAPI variant = Global.getSettings().getVariant(pickedVariantId);
            int variantFp = variant.getHullSpec().getFleetPoints();

            if (variantFp <= budget) {
                FleetMemberAPI satellite = Global.getFactory().createFleetMember(FleetMemberType.SHIP, pickedVariantId);
                if(satellite != null) {
                    satellitesToAdd.add(satellite);
                budget -= variantFp;
                }
                else {
                    Global.getSector().getCampaignUI().addMessage("bad thing happnened in fleetutils uh oh also if youre reading this tell niko to improve their logging");
                }
            }
            else picker.remove(pickedVariantId);
        }
        for (FleetMemberAPI satellite : satellitesToAdd) {
            fleet.getFleetData().addFleetMember(satellite);
        }
    }

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y) {
        return createNewSatelliteFleet(script, location, x, y, 50, script.getSatelliteVariantWeightedIds());
    }

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y, int budget, HashMap<String, Float> variants) {
        CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(script);
        attemptToFillFleetWithVariants(budget, satelliteFleet, variants);

        satelliteFleet.setContainingLocation(location); //todo: I HOPE THIS WORKS SO BAD
        satelliteFleet.setLocation(x, y);

        satelliteFleet.setDoNotAdvanceAI(true);

        return satelliteFleet;
    }

    public static CampaignFleetAPI generateTemporarySatelliteFleet(MarketAPI market, LocationAPI location, Vector2f coordinates) {
        if (market != null) {
            niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
            if (script == null) return null;
            LocationAPI playerContainingLocation = Global.getSector().getPlayerFleet().getContainingLocation();
            Vector2f playerCoordinates = Global.getSector().getPlayerFleet().getLocation();

            CampaignFleetAPI satelliteFleet = createNewSatelliteFleet(script, location, coordinates.x, coordinates.y);
            addCleanupListenerToFleet(script, satelliteFleet);
            satelliteFleet.addScript(new niko_MPC_campaignResumedDeleteScript(satelliteFleet));
            return satelliteFleet;
        }
        return null;
    }
}

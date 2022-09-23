package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAI;
import data.scripts.campaign.listeners.niko_MPC_satelliteFleetDespawnListener;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_ids.*;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;

public class niko_MPC_fleetUtils {

    /**
     * Creates an empty fleet with absolutely nothing in it, except for the memflags satellite fleets must have.
     * @return A new satellite fleet.
     */
    public static CampaignFleetAPI createSatelliteFleetTemplate(niko_MPC_satelliteParams params) {

        String factionId = niko_MPC_satelliteUtils.getCurrentSatelliteFactionId(params);
        String fleetName = params.getSatelliteFleetName();

        final CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(factionId, fleetName, true);
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

        fleetMemory.set(isSatelliteFleetId, true);
        fleetMemory.set(satelliteParamsId, params);

        fleet.setAI(new niko_MPC_satelliteFleetAI((CampaignFleet) fleet));

        fleet.addEventListener(new niko_MPC_satelliteFleetDespawnListener());

        return fleet;
    }

    /**
     * Fills fleet with the given variants up until budget isn't high enough to generate any more variants.
     * Uses a weighted picking system to determine what ships to add.
     *
     * @param budget   The amount of FP to be added to the fleet. Hard cap.
     * @param fleet    The fleet to fill.
     * @param variants The variants, in variantId -> weight format, to be picked.
     */
    public static void attemptToFillFleetWithVariants(int budget, CampaignFleetAPI fleet, HashMap<String, Float> variants) {
        attemptToFillFleetWithVariants(budget, fleet, variants, false);
    }

    /**
     * Fills fleet with the given variants up until budget isn't high enough to generate any more variants.
     * Uses a weighted picking system to determine what ships to add.
     * @param budget The amount of FP to be added to the fleet. Hard cap.
     * @param fleet The fleet to fill.
     * @param variants The variants, in variantId -> weight format, to be picked.
     */
    public static void attemptToFillFleetWithVariants(int budget, CampaignFleetAPI fleet, HashMap<String, Float> variants, boolean altBudgetMode){
        if (budget <= 0) {
            return;
        }

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        List<FleetMemberAPI> shipsToAdd = new ArrayList<>();

        for (Map.Entry<String, Float> entry : variants.entrySet()) { //add the contents of the variants to the picker
            picker.add(entry.getKey(), entry.getValue());
        }

        // explanation of the conditions here: since when a ship is successfully added we subtract its FP from budget,
        // we need to always check to see if budget is empty so we can stop
        // and since we also remove any variants that don't have enough fp to be made, we need to check to make sure
        // the picker still hsa things to pick
        while ((budget > 0) && (!(picker.isEmpty()))) {
            String pickedVariantId = picker.pick();
            ShipVariantAPI variant = Global.getSettings().getVariant(pickedVariantId);
            int variantFp = variant.getHullSpec().getFleetPoints();
            if (altBudgetMode) variantFp = 1; //turns this into a method that adds x amount of ships

            if (variantFp <= budget) { // is only true if we can afford making this ship
                FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, pickedVariantId);
                if(ship != null) {
                    shipsToAdd.add(ship);
                    budget -= variantFp;
                }
                else {
                    String error = "attemptToFillFleetWithVariants created null ship";
                    niko_MPC_debugUtils.displayError(error, true); //THIS SHOULD NEVER HAPPEN. EVER.
                }
            } else {
                picker.remove(pickedVariantId);
                continue; //continue for clarity
            }
        }
        for (FleetMemberAPI ship : shipsToAdd) {
            fleet.getFleetData().addFleetMember(ship);
        }
    }

    public static void safeDespawnFleet(CampaignFleetAPI fleet) {
        fleet.setLocation(9999999, 9999999);
        fleet.despawn();
    }

    public static CampaignFleetAPI spawnSatelliteFleet(niko_MPC_satelliteParams params, Vector2f coordinates, LocationAPI location) {
        CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(params);

        location.addEntity(satelliteFleet);
        satelliteFleet.setLocation(coordinates.x, coordinates.y);
        satelliteFleet.addScript(new niko_MPC_temporarySatelliteFleetDespawner(satelliteFleet, params));

        satelliteFleet.addAssignment(FleetAssignment.HOLD, location.createToken(coordinates), 99999999f);

        params.newSatellite(satelliteFleet);

        return satelliteFleet;
    }

    public static CampaignFleetAPI createNewFullSatelliteFleet(niko_MPC_satelliteParams params, SectorEntityToken entity) {
        return createNewFullSatelliteFleet(params, entity.getLocation(), entity.getContainingLocation());
    }

    public static CampaignFleetAPI createNewFullSatelliteFleet(niko_MPC_satelliteParams params, Vector2f coordinates, LocationAPI location) {
        CampaignFleetAPI satelliteFleet = spawnSatelliteFleet(params, coordinates, location);

        attemptToFillFleetWithVariants(params.maxBattleSatellites, satelliteFleet, params.weightedVariantIds, true);

        return satelliteFleet;
    }

    public static niko_MPC_satelliteParams getSatelliteFleetParams(CampaignFleetAPI fleet) {
        return (niko_MPC_satelliteParams) fleet.getMemoryWithoutUpdate().get(satelliteParamsId);
    }

    public static boolean fleetIsSatelliteFleet(CampaignFleetAPI fleet) {
        return fleet.getMemoryWithoutUpdate().is(isSatelliteFleetId, true);
    }

    /*
    public static List<CampaignFleetAPI> spawnTemporarySatelliteFleetsOnFleet(CampaignFleetAPI fleet) {
        List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();
        LocationAPI containingLocation = fleet.getContainingLocation();

        // we cant use getMarketsInLocation because it doesnt return planetary markets, sadly
        // have to use a copy here, since no matter what, the base list will have a item added to it if we add a fleet
        List<SectorEntityToken> allEntitiesInLocation = new ArrayList<>(containingLocation.getAllEntities());

        for (SectorEntityToken entity : allEntitiesInLocation) {
            MarketAPI market = entity.getMarket();
            if (market != null) {
                niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
                if (script != null) {
                    if (MathUtils.isWithinRange(entity, fleet, script.getSatelliteSpawnRadius())) {
                        CampaignFleetAPI satelliteFleet = generateTemporarySatelliteFleet(market, containingLocation, fleet.getLocation());
                        satelliteFleets.add(satelliteFleet);
                        }
                }
            }
        }
        return satelliteFleets;

     */
}

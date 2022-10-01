package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.utilities.niko_MPC_ids.*;

public class niko_MPC_fleetUtils {

    /**
     * Creates an empty fleet with absolutely nothing in it, except for the memflags satellite fleets must have.
     * @return A new satellite fleet.
     */
    public static CampaignFleetAPI createSatelliteFleetTemplate(@NotNull niko_MPC_satelliteHandler handler) {
        return handler.createSatelliteFleetTemplate();
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
     * @return a list of the newly created fleetmembers.
     */
    @NotNull
    public static List<FleetMemberAPI> attemptToFillFleetWithVariants(int budget, CampaignFleetAPI fleet, HashMap<String, Float> variants, boolean altBudgetMode){
        List<FleetMemberAPI> newFleetMembers = new ArrayList<>();
        if (budget <= 0) {
            return newFleetMembers;
        }

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();

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

                    newFleetMembers.add(ship);
                    ship.getRepairTracker().setCR(0.7f); //the ships spawn with 50 cr, fo rsome reaosn, so i have to do this
                }
                else {
                    String error = "attemptToFillFleetWithVariants created null ship, fleet: " + fleet;
                    niko_MPC_debugUtils.displayError(error); //THIS SHOULD NEVER HAPPEN. EVER.
                }
                budget -= variantFp;
            } else {
                picker.remove(pickedVariantId);
                continue; //continue for clarity
            }
        }
        for (FleetMemberAPI ship : newFleetMembers) {
            fleet.getFleetData().addFleetMember(ship);
        }
        return newFleetMembers;
    }

    public static void despawnSatelliteFleet(CampaignFleetAPI fleet) {
        despawnSatelliteFleet(fleet, false);
    }

    public static void despawnSatelliteFleet(CampaignFleetAPI fleet, boolean vanish) {
        genericPreDeleteSatelliteFleetCleanup(fleet);

        if (vanish) {
            fleet.setLocation(9999999, 9999999);
        }
        fleet.despawn(); //will ALWAYS call the despawn listener
    }

    public static void genericPreDeleteSatelliteFleetCleanup(@NotNull CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        niko_MPC_temporarySatelliteFleetDespawner script = (niko_MPC_temporarySatelliteFleetDespawner) fleetMemory.get(niko_MPC_ids.temporaryFleetDespawnerId);
        if (script != null) {
            script.prepareForGarbageCollection();
        }

        niko_MPC_satelliteHandler handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(fleet);
        if (handler != null) {
            handler.cleanUpSatelliteFleetBeforeDeletion(fleet);
        }
    }

    public static CampaignFleetAPI spawnSatelliteFleet(niko_MPC_satelliteHandler handler, Vector2f coordinates, LocationAPI location) {
        return spawnSatelliteFleet(handler, coordinates, location, true, false);
    }

    public static CampaignFleetAPI spawnSatelliteFleet(@NotNull niko_MPC_satelliteHandler handler, Vector2f coordinates, LocationAPI location, boolean temporary, boolean dummy) {
        return handler.spawnSatelliteFleet(coordinates, location, temporary, dummy);
    }

    @Nullable
    public static CampaignFleetAPI getHandlerDialogFleet(@NotNull niko_MPC_satelliteHandler handler, SectorEntityToken entity) {
        if (handler.fleetForPlayerDialog == null) {
            handler.fleetForPlayerDialog = createNewFullSatelliteFleet(handler, entity);
        }
        return handler.fleetForPlayerDialog;
    }

    public static CampaignFleetAPI createNewFullSatelliteFleet(niko_MPC_satelliteHandler handler, SectorEntityToken entity) {
        return createNewFullSatelliteFleet(handler, entity, true);
    }

    public static CampaignFleetAPI createNewFullSatelliteFleet(niko_MPC_satelliteHandler handler, @NotNull SectorEntityToken entity, boolean temporary) {
        return createNewFullSatelliteFleet(handler, entity.getLocation(), entity.getContainingLocation(), temporary, false);
    }

    public static CampaignFleetAPI createNewFullSatelliteFleet(@NotNull niko_MPC_satelliteHandler handler, Vector2f coordinates, LocationAPI location, boolean temporary, boolean dummy) {
        return handler.createNewFullSatelliteFleet(coordinates, location, temporary, dummy);
    }

    public static boolean fleetIsSatelliteFleet(@NotNull CampaignFleetAPI fleet) {
        return fleet.getMemoryWithoutUpdate().is(isSatelliteFleetId, true);
    }

    public static CampaignFleetAPI createDummyFleet(@NotNull niko_MPC_satelliteHandler handler, SectorEntityToken entity) {
        return handler.createDummyFleet(entity);
    }

    public static boolean isFleetValidEngagementTarget(@Nullable CampaignFleetAPI fleet) {
        if (fleet == null) return false;
        if (fleet == Global.getSector().getPlayerFleet()) return false;
        if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) return false;

        return true;
    }

    /*public static boolean satelliteFleetIsHostileTo(CampaignFleetAPI satelliteFleet, CampaignFleetAPI fleet) {
        niko_MPC_satellitehandler handler = niko_MPC_satelliteUtils.getEntitySatellitehandler(satelliteFleet);

        String originalFactionId = satelliteFleet.getFaction().getId();
        satelliteFleet.setFaction(niko_MPC_ids.satelliteFactionId);
        boolean result = ((satelliteFleet.isHostileTo(fleet)) || (handler.getSatelliteFaction().isHostileTo(fleet.getFaction())));
        satelliteFleet.setFaction(originalFactionId);
    }*/

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

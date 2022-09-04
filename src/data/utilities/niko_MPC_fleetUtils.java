package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.campaign.ai.ModularFleetAI;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import data.scripts.everyFrames.niko_MPC_campaignResumedDeleteScript;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_ids.isSatelliteFleetId;
import static data.utilities.niko_MPC_ids.satelliteTrackerId;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;
import static data.utilities.niko_MPC_scriptUtils.getInstanceOfSatelliteTracker;

public class niko_MPC_fleetUtils {

    public static CampaignFleetAPI createSatelliteFleetTemplate(niko_MPC_satelliteTrackerScript script) {
        String factionId = script.getSatelliteFactionId();
        String fleetName = script.getSatelliteFleetName();
        String fleetType = script.getSatelliteFleetType();

        final CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(factionId, fleetName, true);
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        fleetMemory.set(MemFlags.MEMORY_KEY_FLEET_TYPE, fleetType);
        fleetMemory.set(MemFlags.FLEET_FIGHT_TO_THE_LAST, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleetMemory.set(MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER, true);

        fleetMemory.set(isSatelliteFleetId, true);
        fleetMemory.set(satelliteTrackerId, script);

        MutableFleetStatsAPI stats = fleet.getStats();

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

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y, boolean addAssignment) {
        return createNewSatelliteFleet(script, location, x, y, 200, script.getSatelliteVariantWeightedIds(), addAssignment);
    }

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y) {
        return createNewSatelliteFleet(script, location, x, y, 200, script.getSatelliteVariantWeightedIds(), true);
    }

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y, int budget, HashMap<String, Float> variants) {
        return createNewSatelliteFleet(script, location, x, y, budget, variants, true);
    }

    public static CampaignFleetAPI createNewSatelliteFleet(niko_MPC_satelliteTrackerScript script, LocationAPI location, float x, float y, int budget, HashMap<String, Float> variants,
                                                           boolean addAssignment) {
        final CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(script);
        attemptToFillFleetWithVariants(budget, satelliteFleet, variants);

        satelliteFleet.setContainingLocation(location); //todo: I HOPE THIS WORKS SO BAD
        location.addEntity(satelliteFleet);
        satelliteFleet.setLocation(x, y);
        if (satelliteFleet.getAI() == null) {
            satelliteFleet.setAI(new ModularFleetAI((CampaignFleet) satelliteFleet));
        }

        if (addAssignment) {
            satelliteFleet.getAI().addAssignment(FleetAssignment.HOLD, satelliteFleet.getContainingLocation().createToken(x, y), 500f, null);
        }

        script.getSatelliteFleets().add(satelliteFleet);
        return satelliteFleet;
    }

    public static CampaignFleetAPI generateTemporarySatelliteFleet(MarketAPI market, LocationAPI location, Vector2f coordinates) {
        if (market != null) {
            niko_MPC_satelliteTrackerScript script = getInstanceOfSatelliteTracker(market);
            if (script == null) return null;

            CampaignFleetAPI satelliteFleet = createNewSatelliteFleet(script, location, coordinates.x, coordinates.y);
            satelliteFleet.addScript(new niko_MPC_campaignResumedDeleteScript(satelliteFleet));
            return satelliteFleet;
        }
        return null;
    }

    public static void safeDespawnFleet(CampaignFleetAPI fleet) {
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        if (fleetMemory.contains(isSatelliteFleetId)) {
            niko_MPC_satelliteTrackerScript script = (niko_MPC_satelliteTrackerScript) fleetMemory.get(satelliteTrackerId);
            if (script != null) {
                script.getSatelliteFleets().remove(fleet);
                deleteMemoryKey(fleet.getMemoryWithoutUpdate(), satelliteTrackerId);
            } else if (!fleet.isExpired()) {
                Global.getSector().getCampaignUI().addMessage("Satellite fleet has no script in despawnSatelliteFleet, and is not expired");
            }
        }
        fleet.despawn();
    }

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
    }
}

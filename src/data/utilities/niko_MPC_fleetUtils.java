package data.utilities;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.loading.AbilitySpecAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;
import data.scripts.util.MagicCampaign;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.LazyLib;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class niko_MPC_fleetUtils {

    private static final Logger log = Global.getLogger(niko_MPC_fleetUtils.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static CampaignFleetAPI createSatelliteFleetTemplate(niko_MPC_satelliteTrackerScript script) {
        return createSatelliteFleetTemplate(script.getSatelliteFactionId(), script.getSatelliteFleetName(), script.getSatelliteFleetType(), script.getMarket(), script);
    }

    public static CampaignFleetAPI createSatelliteFleetTemplate(String factionId, String fleetName, String fleetType, MarketAPI market) {
        return createSatelliteFleetTemplate(factionId, fleetName, fleetType, market,null);
    }

    public static CampaignFleetAPI createSatelliteFleetTemplate(String factionId, String fleetName, String fleetType, MarketAPI market, niko_MPC_satelliteTrackerScript script) {
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(factionId, fleetName, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, fleetType);

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
        attemptToFillFleetWithVariants(budget, fleet, variants, null);
    }

    public static void attemptToFillFleetWithVariants(int budget, CampaignFleetAPI fleet, HashMap<String, Float> variants, niko_MPC_satelliteTrackerScript script) {
        if (doFleetFillBudgetCheck(budget, fleet, script)) {
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
                    if (doFleetFillBudgetCheck(budget, fleet, script, true)) break;
                }
            }
            else picker.remove(pickedVariantId);
        }
        for (FleetMemberAPI satellite : satellitesToAdd) {
            fleet.getFleetData().addFleetMember(satellite);
        }
    }

    public static CampaignFleetAPI addInternalSatelliteFleetToMarket(niko_MPC_satelliteTrackerScript script, MarketAPI market) {
        MemoryAPI marketMemory = market.getMemoryWithoutUpdate();
        CampaignFleetAPI satelliteFleet = createSatelliteFleetTemplate(script);

        int numShips = 3f;//todo: VERY ARBITRARY
    }

    public static boolean doFleetFillBudgetCheck(int budget, CampaignFleetAPI fleet, niko_MPC_satelliteTrackerScript script) {
        return doFleetFillBudgetCheck(budget, fleet, script, false);
    }

    public static boolean doFleetFillBudgetCheck(int budget, CampaignFleetAPI fleet, niko_MPC_satelliteTrackerScript script, boolean duringLoop) {
        if (budget == 0) {
            return true;
        }
        if (budget > 0) {
            if (duringLoop) {
                Global.getSector().getCampaignUI().addMessage("attemptToFillFleetWithVariants somehow ended a loop with a budget of " + budget + ", please inform the more" +
                        "planetary conditions mod author, and provide a copy of your logs.");
                if (script != null) {
                    log.debug("attemptToFillWithVariants ended a loop with a budget of " + budget + ", called by" + script.getMarket().getName() + " 's asteroid tracker.");
                }
                else {
                    log.debug(("attemptToFillWithVariants ended a loop with a budget of " + budget + ", called by an unknown caller."));
                }
            }
            else {
                Global.getSector().getCampaignUI().addMessage("attemptToFillFleetWithVariants was called with a budget of " + budget + ", please inform the more" +
                        "planetary conditions mod author, and provide a copy of your logs.");
                if (script != null) {
                    log.debug(script.getMarket().getName() + "'s satellite tracker passed a budget of " + budget + "to attemptToFillFleetWithVariants.");
                } else {
                    log.debug("An unknown caller passed a budget of " + budget + "to attemptToFillFleetWithVariants.");
                }
            }
            return true;
        }
    return false;
    }

}

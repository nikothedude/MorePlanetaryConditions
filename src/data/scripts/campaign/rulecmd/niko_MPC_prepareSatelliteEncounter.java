package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore;
import data.utilities.niko_MPC_dialogUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.*;

public class niko_MPC_prepareSatelliteEncounter extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        SectorEntityToken entity = dialog.getInteractionTarget();

        entity = niko_MPC_dialogUtils.digForSatellitesInEntity(entity);

        niko_MPC_satelliteHandlerCore handler = niko_MPC_satelliteUtils.getSatelliteHandler(entity);
        if (handler == null) return false;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Set<SectorEntityToken> entitiesWillingToFight = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToFight(playerFleet);
        entitiesWillingToFight.add(entity);

        List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();

        CampaignFleetAPI focusedSatellite = null;
        for (SectorEntityToken satelliteEntity : entitiesWillingToFight) {
            niko_MPC_satelliteHandlerCore satelliteHandler = niko_MPC_satelliteUtils.getSatelliteHandler(satelliteEntity);
            CampaignFleetAPI dialogFleet = niko_MPC_fleetUtils.getHandlerDialogFleet(satelliteHandler, playerFleet);
            if (dialogFleet != null) {
                focusedSatellite = dialogFleet;
                satelliteFleets.add(dialogFleet);
            }
        }

        if (focusedSatellite == null) return false;
        boolean isFightingFriendly = false;

        for (CampaignFleetAPI satelliteFleet : satelliteFleets) {
            if (satelliteFleet.getFaction().isPlayerFaction()) {
                isFightingFriendly = true;
                satelliteFleet.setFaction("derelict"); //hack-the game doesnt let you fight your own faction, ever
                // ^ possible issue, if this fleet is engaged in combat and is a player fleet it might fuck some shit up
            }
            MemoryAPI fleetMemory = satelliteFleet.getMemoryWithoutUpdate();
            boolean stillSet = Misc.setFlagWithReason(fleetMemory, MemFlags.MEMORY_KEY_MAKE_HOSTILE, niko_MPC_ids.satelliteFleetHostileReason, true, 999999999);
            if (!stillSet) {
                if (satelliteFleet.getAI() instanceof ModularFleetAIAPI) {
                    ModularFleetAIAPI mAI = (ModularFleetAIAPI) satelliteFleet.getAI();
                    mAI.getTacticalModule().forceTargetReEval();
                }
            }
            Misc.setFlagWithReason(fleetMemory, MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, niko_MPC_ids.satelliteFleetHostileReason, true, 999999999);
        }
            niko_MPC_dialogUtils.createSatelliteFleetFocus(focusedSatellite, satelliteFleets, dialog, entity, memoryMap, isFightingFriendly);

        return true;
    }
}

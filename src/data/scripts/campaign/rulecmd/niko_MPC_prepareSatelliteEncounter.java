package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.MakeOtherFleetHostile;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_dialogUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_satelliteUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class niko_MPC_prepareSatelliteEncounter extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        SectorEntityToken entity = dialog.getInteractionTarget();

        niko_MPC_satelliteParams satelliteParams = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
        if (satelliteParams == null) return false;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Set<SectorEntityToken> entitiesWillingToFight = niko_MPC_satelliteUtils.getNearbyEntitiesWithSatellitesWillingToFight(playerFleet);
        entitiesWillingToFight.add(entity);

        List<CampaignFleetAPI> satelliteFleets = new ArrayList<>();

        for (SectorEntityToken satelliteEntity : entitiesWillingToFight) {
            niko_MPC_satelliteParams iteratedSatelliteParams = niko_MPC_satelliteUtils.getEntitySatelliteParams(satelliteEntity);
            satelliteFleets.add(niko_MPC_fleetUtils.createNewFullSatelliteFleet(iteratedSatelliteParams, playerFleet));
        }

        if (satelliteFleets.size() == 0) return false;

        CampaignFleetAPI focusedSatellite = satelliteFleets.get(0);

        for (CampaignFleetAPI satelliteFleet : satelliteFleets) {
            MemoryAPI fleetMemory = satelliteFleet.getMemoryWithoutUpdate();
            boolean stillSet = Misc.setFlagWithReason(fleetMemory, MemFlags.MEMORY_KEY_MAKE_HOSTILE, niko_MPC_ids.satelliteFleetHostileReason, true, 999999999);
            if (!stillSet) {
                if (satelliteFleet.getAI() instanceof ModularFleetAIAPI) {
                    ModularFleetAIAPI mAI = (ModularFleetAIAPI) satelliteFleet.getAI();
                    mAI.getTacticalModule().forceTargetReEval();
                }
            }
            Misc.setFlagWithReason(fleetMemory, MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, niko_MPC_ids.satelliteFleetHostileReason, true, 999999999);

            if (Objects.equals(satelliteFleet.getFaction().getId(), "player")) {
                satelliteFleet.setFaction("derelict"); //hack-the game doesnt let you fight your own faction, ever
            }
        }

        niko_MPC_dialogUtils.createSatelliteFleetFocus(focusedSatellite, satelliteFleets, dialog, memoryMap);

        return true;
    }
}

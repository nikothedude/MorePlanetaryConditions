package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_dialogUtils;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_satelliteUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        niko_MPC_dialogUtils.createSatelliteFleetFocus(focusedSatellite, dialog, memoryMap);

        return true;
    }
}

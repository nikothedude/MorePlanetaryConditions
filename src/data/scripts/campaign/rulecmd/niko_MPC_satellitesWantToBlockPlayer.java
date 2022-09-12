package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.List;
import java.util.Map;

public class niko_MPC_satellitesWantToBlockPlayer extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        SectorEntityToken entity = dialog.getInteractionTarget();

        niko_MPC_satelliteParams satelliteParams = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
        if (satelliteParams == null) return false;

        return niko_MPC_satelliteUtils.doEntitySatellitesWantToBlock(entity, Global.getSector().getPlayerFleet());
    }
}

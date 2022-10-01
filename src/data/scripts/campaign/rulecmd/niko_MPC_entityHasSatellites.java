package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.utilities.niko_MPC_dialogUtils;
import data.utilities.niko_MPC_satelliteUtils;

import java.util.List;
import java.util.Map;

public class niko_MPC_entityHasSatellites extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        SectorEntityToken entity = dialog.getInteractionTarget();
        entity = niko_MPC_dialogUtils.digForSatellitesInEntity(entity);

        return niko_MPC_satelliteUtils.getEntitySatelliteHandler(entity) != null;
    }
}

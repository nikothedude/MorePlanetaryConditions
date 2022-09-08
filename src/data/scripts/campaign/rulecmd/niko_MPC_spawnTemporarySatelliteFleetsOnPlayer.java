package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class niko_MPC_spawnTemporarySatelliteFleetsOnPlayer extends BaseCommandPlugin {
    @Deprecated
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        boolean setFocus = false;
        if (params.size() > 0) {
            setFocus = params.get(0).getBoolean(memoryMap);
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
       // List<CampaignFleetAPI> satelliteFleets = spawnTemporarySatelliteFleetsOnFleet(playerFleet);

      //  if (satelliteFleets.size() <= 0) {
       //     return false;
     //   }
      //  if (setFocus) {
       //     createSatelliteFleetFocus(satelliteFleets.get(0), dialog, memoryMap);
       // }

    return true;
    }
}

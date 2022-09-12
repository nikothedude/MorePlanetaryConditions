package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_satelliteUtils;

import static data.utilities.niko_MPC_ids.satelliteParamsId;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;

public class niko_MPC_satelliteFleetDespawnListener extends BaseFleetEventListener {

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        super.reportFleetDespawnedToListener(fleet, reason, param);

        if (!niko_MPC_debugUtils.ensureEntityHasSatellites(fleet)) return;
        niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(fleet);

        BattleAPI battle = fleet.getBattle();
        if (battle != null) {
            params.influencedBattles.remove(battle);
        }
        params.satelliteFleets.remove(fleet);
        deleteMemoryKey(fleet.getMemoryWithoutUpdate(), satelliteParamsId);
        fleet.removeEventListener(this);

        Global.getSector().getListenerManager().removeListener(this);
    }
}

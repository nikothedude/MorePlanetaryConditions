package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_satelliteUtils;

import static data.utilities.niko_MPC_ids.satelliteParamsId;
import static data.utilities.niko_MPC_memoryUtils.deleteMemoryKey;

public class niko_MPC_satelliteFleetDespawnListener extends BaseFleetEventListener {

    /**
     * This listener is attached to every satellite fleet that spawns.
     * <p>
     * Gets the params that spawned the fleet and does the following:
     * </p>
     * @param fleet
     * @param reason
     * @param param
     */
    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, CampaignEventListener.FleetDespawnReason reason, Object param) {
        super.reportFleetDespawnedToListener(fleet, reason, param);

        if (!niko_MPC_debugUtils.assertEntityHasSatellites(fleet)) return; //sanity
        niko_MPC_satelliteHandler params = niko_MPC_satelliteUtils.getEntitySatelliteHandler(fleet);

        BattleAPI battle = fleet.getBattle();

        params.satelliteFleets.remove(fleet);
        deleteMemoryKey(fleet.getMemoryWithoutUpdate(), satelliteParamsId);
        fleet.removeEventListener(this);

        Global.getSector().getListenerManager().removeListener(this);
    }
}

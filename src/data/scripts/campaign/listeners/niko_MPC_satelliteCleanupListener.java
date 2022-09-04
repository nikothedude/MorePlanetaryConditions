package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

import static data.utilities.niko_MPC_fleetUtils.safeDespawnFleet;
import static data.utilities.niko_MPC_ids.isSatelliteFleetId;
import static data.utilities.niko_MPC_ids.satelliteTrackerId;

public class niko_MPC_satelliteCleanupListener extends BaseCampaignEventListener {

    public niko_MPC_satelliteCleanupListener(boolean permaRegister) {
        super(permaRegister);
    }

    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        for (CampaignFleetAPI fleet : battle.getSnapshotBothSides()) {
            if (fleet.getMemoryWithoutUpdate().contains(isSatelliteFleetId)) {
                MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
                niko_MPC_satelliteTrackerScript script = (niko_MPC_satelliteTrackerScript) fleetMemory.get(satelliteTrackerId);
                if (script == null) {
                    Global.getSector().getCampaignUI().addMessage("Satellite fleet in" + fleet.getContainingLocation() + "had no script in cleanup listener");
                    return;
                }
                safeDespawnFleet(fleet);
            }
        }
    }

}

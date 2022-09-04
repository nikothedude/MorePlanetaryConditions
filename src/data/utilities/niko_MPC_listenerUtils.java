package data.utilities;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.campaign.listeners.niko_MPC_satelliteBattleCleanupListener;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

public class niko_MPC_listenerUtils {

    public static niko_MPC_satelliteBattleCleanupListener addCleanupListenerToFleet(niko_MPC_satelliteTrackerScript script, CampaignFleetAPI fleet) {
        niko_MPC_satelliteBattleCleanupListener cleanupListener = (new niko_MPC_satelliteBattleCleanupListener(script));
        fleet.addEventListener(cleanupListener);
        script.cleanupListenersWithFleet.put(cleanupListener, fleet);
        return cleanupListener;
    }


}

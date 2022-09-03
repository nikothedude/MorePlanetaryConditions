package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener;
import com.fs.starfarer.campaign.fleet.Battle;
import data.scripts.everyFrames.niko_MPC_satelliteTrackerScript;

public class niko_MPC_satelliteBattleCleanupListener extends BaseFleetEventListener {
    niko_MPC_satelliteTrackerScript script;

    public niko_MPC_satelliteBattleCleanupListener(niko_MPC_satelliteTrackerScript script) {
        this.script = script;
    }

    public void reportBattleOccurred(CampaignFleetAPI fleet,
                                     CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle.isDone()) {
            fleet.despawn(); // you exist soley to fight
            script.doneInfluencingBattle(battle);
        }
    }

    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet,
                                               CampaignEventListener.FleetDespawnReason reason, Object param) {
        if (reason == CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE) {
            BattleAPI battle = (BattleAPI) param;
            script.doneInfluencingBattle(battle);
        }
        script.removeCleanupListener(this);
        fleet.removeEventListener(this); //youre useless. die
    }

}

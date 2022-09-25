package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.*;

public class niko_MPC_satelliteEventListener extends BaseCampaignEventListener {
    public niko_MPC_satelliteEventListener(boolean permaRegister) {
        super(permaRegister);
    }

    /**
     * Using the global satellite battle tracker, iterates through a list of all satellite params that are influencing
     * the battle. If the stored side of the satellite params is not the same as the primary winner, we can assume
     * that we lost an offensive battle, and we give all the fleets on the enemy's side a grace period.
     *
     * @param primaryWinner The "primary" fleet of the side that won. This is NOT the combined fleet.
     * @param battle The battle to check.
     */
    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        super.reportBattleFinished(primaryWinner, battle);
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

        for (niko_MPC_satelliteParams params : tracker.getSatellitesInfluencingBattle(battle)) {
            BattleAPI.BattleSide battleSide = tracker.getSideOfSatellitesForBattle(battle, params);
            if (battleSide != battle.pickSide(primaryWinner)) { // if our picked side on the battle does not have the winner,
                for (CampaignFleetAPI hostileFleet : battle.getSnapshotSideFor(primaryWinner)) { // we can assume that
                    float graceIncrement = niko_MPC_ids.satelliteVictoryGraceIncrement; // we lost the final engagement,
                    params.adjustGracePeriod(hostileFleet, graceIncrement); // to have "beat the satellites", giving
                    // them a period of grace
                }
            }
        }
        tracker.removeBattle(battle);
    }
}

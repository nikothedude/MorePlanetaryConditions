package data.scripts.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

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

    @Override //if this fails, we can add a script on fleet usage of jump point, which is a method in here
    public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {
        super.reportFleetReachedEntity(fleet, entity);
        if (fleet == null) return;

        FleetAssignmentDataAPI assignment = fleet.getCurrentAssignment();

        niko_MPC_satelliteParams params = niko_MPC_satelliteUtils.getEntitySatelliteParams(entity);
        if (params == null) {
            SectorEntityToken trueTarget = assignment.getTarget();
            params = niko_MPC_satelliteUtils.getEntitySatelliteParams(trueTarget);

            if (params == null) {
                SectorEntityToken orbitTarget = trueTarget.getOrbitFocus();
                params = niko_MPC_satelliteUtils.getEntitySatelliteParams(orbitTarget);
            }
        }

        if (params != null) {
            SectorEntityToken paramEntity = params.entity;
            if (fleet == Global.getSector().getPlayerFleet()) return;
            if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(fleet)) return;
            if (niko_MPC_satelliteUtils.areEntitySatellitesCapableOfBlocking(paramEntity, fleet) && niko_MPC_satelliteUtils.doEntitySatellitesWantToBlock(paramEntity, fleet)) {
                if (niko_MPC_satelliteUtils.doEntitySatellitesWantToFight(paramEntity, fleet)) {
                    if ((fleet.getInteractionTarget() == paramEntity) || //this is inconsistant, not everything (notably raids) triggers this
                            (assignment.getTarget() == paramEntity) ||
                            (assignment.getTarget().getOrbitFocus() == paramEntity)) { //raids DO however have the planet as an orbit focus

                        niko_MPC_satelliteUtils.makeEntitySatellitesEngageFleet(paramEntity, fleet);
                    }
                }
            }
        }
    }
}

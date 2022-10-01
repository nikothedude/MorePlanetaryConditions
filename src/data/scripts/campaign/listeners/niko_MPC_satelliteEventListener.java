package data.scripts.campaign.listeners;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import data.scripts.campaign.misc.niko_MPC_satelliteHandler;
import data.utilities.niko_MPC_fleetUtils;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_satelliteBattleTracker;
import data.utilities.niko_MPC_satelliteUtils;

public class niko_MPC_satelliteEventListener extends BaseCampaignEventListener {
    public niko_MPC_satelliteEventListener(boolean permaRegister) {
        super(permaRegister);
    }

    /**
     * Using the global satellite battle tracker, iterates through a list of all satellite handler that are influencing
     * the battle. If the stored side of the satellite handler is not the same as the primary winner, we can assume
     * that we lost an offensive battle, and we give all the fleets on the enemy's side a grace period.
     *
     * @param primaryWinner The "primary" fleet of the side that won. This is NOT the combined fleet.
     * @param battle The battle to check.
     */
    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) { //fixme: doesnt fire on player battle end
        super.reportBattleFinished(primaryWinner, battle);
        niko_MPC_satelliteBattleTracker tracker = niko_MPC_satelliteUtils.getSatelliteBattleTracker();

        for (niko_MPC_satelliteHandler handler : tracker.getSatellitesInfluencingBattle(battle)) {
            BattleAPI.BattleSide battleSide = tracker.getSideOfSatellitesForBattle(battle, handler);
            if (battleSide != battle.pickSide(primaryWinner)) { // if our picked side on the battle does not have the winner,
                for (CampaignFleetAPI hostileFleet : battle.getSnapshotSideFor(primaryWinner)) { // we can assume that
                    float graceIncrement = niko_MPC_ids.satelliteVictoryGraceIncrement; // we lost the final engagement,
                    handler.adjustGracePeriod(hostileFleet, graceIncrement); // to have "beat the satellites", giving
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

        niko_MPC_satelliteHandler handler = null;
        if (entity != null) handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(entity);

        if (handler == null) {
            SectorEntityToken trueTarget = null;
            if (assignment != null) trueTarget = assignment.getTarget();
            if (trueTarget != null) handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(trueTarget);

            if (handler == null) {
                SectorEntityToken orbitTarget = null;
                if (trueTarget != null) orbitTarget = trueTarget.getOrbitFocus();
                if (orbitTarget != null) handler = niko_MPC_satelliteUtils.getEntitySatelliteHandler(orbitTarget);
            }
        }

        if (handler != null) {
            if (!niko_MPC_fleetUtils.isFleetValidEngagementTarget(fleet)) return;
            SectorEntityToken paramEntity = handler.entity;
            if ((fleet.getInteractionTarget() == paramEntity) || //this is inconsistant, not everything (notably raids) triggers this
                    (assignment.getTarget() == paramEntity) ||
                    (assignment.getTarget().getOrbitFocus() == paramEntity)) { //raids DO however have the planet as an orbit focus

                niko_MPC_satelliteUtils.makeEntitySatellitesEngageFleet(paramEntity, fleet);
            }
        }
    }
}

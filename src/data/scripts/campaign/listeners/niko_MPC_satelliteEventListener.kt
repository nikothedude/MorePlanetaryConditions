package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.isFleetValidEngagementTarget
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker
import data.utilities.niko_MPC_satelliteUtils.getHandlerForCondition
import data.utilities.niko_MPC_satelliteUtils.makeEntitySatellitesEngageFleet

class niko_MPC_satelliteEventListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {
    /**
     * Using the global satellite battle tracker, iterates through a list of all satellite handler that are influencing
     * the battle. If the stored side of the satellite handler is not the same as the primary winner, we can assume
     * that we lost an offensive battle, and we give all the fleets on the enemy's side a grace period.
     *
     * @param primaryWinner The "primary" fleet of the side that won. This is NOT the combined fleet.
     * @param battle The battle to check.
     */
    override fun reportBattleFinished(
        primaryWinner: CampaignFleetAPI,
        battle: BattleAPI
    ) { //fixme: doesnt fire on player battle end
        super.reportBattleFinished(primaryWinner, battle)
        val tracker = getSatelliteBattleTracker()
        for (handler in tracker.getSatellitesInfluencingBattle(battle)) {
            val battleSide = tracker.getSideOfSatellitesForBattle(battle, handler)
            if (battleSide != battle.pickSide(primaryWinner)) { // if our picked side on the battle does not have the winner,
                for (hostileFleet in battle.getSnapshotSideFor(primaryWinner)) { // we can assume that
                    val graceIncrement = niko_MPC_ids.satelliteVictoryGraceIncrement // we lost the final engagement,
                    handler.adjustGracePeriod(hostileFleet!!, graceIncrement) // to have "beat the satellites", giving
                    // them a period of grace
                }
            }
        }
        tracker.removeBattle(battle)
    }

    //if this fails, we can add a script on fleet usage of jump point, which is a method in here
    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI, entity: SectorEntityToken) {
        super.reportFleetReachedEntity(fleet, entity)
        if (fleet == null) return
        val assignment = fleet.currentAssignment
        var handler: niko_MPC_satelliteHandlerCore? = null
        if (entity != null) handler = entity.getHandlerForCondition()
        if (handler == null) {
            var trueTarget: SectorEntityToken? = null
            if (assignment != null) trueTarget = assignment.target
            if (trueTarget != null) handler = trueTarget.getHandlerForCondition()
            if (handler == null) {
                var orbitTarget: SectorEntityToken? = null
                if (trueTarget != null) orbitTarget = trueTarget.orbitFocus
                if (orbitTarget != null) handler = orbitTarget.getHandlerForCondition()
            }
        }
        if (handler != null) {
            if (!isFleetValidEngagementTarget(fleet)) return
            val handlerEntity = handler.getEntity()
            if (fleet.interactionTarget === handlerEntity || assignment!!.target === handlerEntity || assignment!!.target.orbitFocus === handlerEntity) { //raids DO however have the planet as an orbit focus
                makeEntitySatellitesEngageFleet(handlerEntity, fleet)
            }
        }
    }
}
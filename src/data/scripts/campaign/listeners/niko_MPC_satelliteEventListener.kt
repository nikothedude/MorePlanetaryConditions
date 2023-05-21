package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

class niko_MPC_satelliteEventListener(permaRegister: Boolean) : BaseCampaignEventListener(permaRegister) {
    /**
     * Using the global satellite battle tracker, iterates through a list of all satellite handler that are influencing
     * the battle. If the stored side of the satellite handler is not the same as the primary winner, we can assume
     * that we lost an offensive battle, and we give all the fleets on the enemy's side a grace period.
     *
     * @param primaryWinner The "primary" fleet of the side that won. This is NOT the combined fleet.
     * @param battle The battle to check.
     */
    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI, battle: BattleAPI) { //fixme: doesnt fire on player battle end
        super.reportBattleFinished(primaryWinner, battle)

        val tracker = getSatelliteBattleTracker() ?: return
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

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        super.reportBattleOccurred(primaryWinner, battle)
    }

    //if this fails, we can add a script on fleet usage of jump point, which is a method in here
    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        super.reportFleetReachedEntity(fleet, entity)
        if (fleet == null) return niko_MPC_debugUtils.log.info("null fleet during reportfleetreached entity, entity: $entity")
        val assignment = fleet.currentAssignment
        var trueTarget: SectorEntityToken? = entity
        var handlers: MutableSet<niko_MPC_satelliteHandlerCore>? = trueTarget?.getSatelliteHandlers()
        if (trueTarget?.hasSatellites() != true) {
            if (assignment != null) trueTarget = assignment.target
            if (trueTarget != null) handlers = trueTarget.getSatelliteHandlers()
            if (trueTarget?.hasSatellites() != true) {
                var orbitTarget: SectorEntityToken? = null
                if (trueTarget != null) orbitTarget = trueTarget.orbitFocus
                if (orbitTarget != null) handlers = orbitTarget.getSatelliteHandlers()
            }
        }
        if (handlers?.isNotEmpty() == true) {
            for (handler: niko_MPC_satelliteHandlerCore in handlers) {
                var handlerEntity: HasMemory = handler.getPrimaryHolder() ?: continue
                if (handlerEntity is MarketAPI && handlerEntity.primaryEntity != null) handlerEntity = handlerEntity.primaryEntity
                //^ if no entity, get market. if no market, get entity. if both, get entity.
                if (fleet.interactionTarget === handlerEntity || assignment!!.target === handlerEntity || assignment!!.target.orbitFocus === handlerEntity) { //raids DO however have the planet as an orbit focus
                    handler.tryToEngageFleet(fleet)
                }
            }
        }
    }
}

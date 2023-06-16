package data.scripts.everyFrames

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_fleetUtils.setTemporaryFleetDespawner
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker

class niko_MPC_temporarySatelliteFleetDespawner(
    var fleet: CampaignFleetAPI,
    var handler: niko_MPC_satelliteHandlerCore?
) : niko_MPC_baseNikoScript() {

    var advanceTimeSinceStart = 0.0f
    var graceRuns = 0

    init {
        fleet.setTemporaryFleetDespawner(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun getPrimaryLocation(): LocationAPI? {
        return fleet.containingLocation
    }

    override fun advance(amount: Float) {
        advanceTimeSinceStart += amount
        if (handler == null) {
            niko_MPC_debugUtils.displayError("Null handler during $this advance")
            delete()
            return
        }
        if (fleet.battle == null && graceRuns <= 0) {
            getRidOfFleet() //this despawns the fleet, which calls the listener, which calls prepareforgc. its weird
            return
        } else {
            val tracker = getSatelliteBattleTracker() ?: return //this cant ever happen in campaign
            val battle = fleet.battle
            if (battle != null && !tracker.areSatellitesInvolvedInBattle(battle, handler!!)) { // sanity
                tracker.associateSatellitesWithBattle(battle, handler!!, battle.pickSide(fleet))
            }
        }
        //graceRuns--;
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        fleet.setTemporaryFleetDespawner(null)
        getRidOfFleet()
        fleet.removeScript(this)

        return true
    }

    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    //arbitrary number
    private fun getRidOfFleet() {
        if (!fleet.isDespawning) {
            val vanish = advanceTimeSinceStart < 1 //arbitrary number
            fleet.satelliteFleetDespawn(vanish)
        }
    }
}

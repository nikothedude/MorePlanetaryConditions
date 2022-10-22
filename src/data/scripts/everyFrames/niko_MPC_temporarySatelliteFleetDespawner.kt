package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.sun.org.apache.xpath.internal.operations.Bool
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.despawnSatelliteFleet
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_memoryUtils.deleteMemoryKey
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker

class niko_MPC_temporarySatelliteFleetDespawner(
    var fleet: CampaignFleetAPI,
    var handler: niko_MPC_satelliteHandlerCore?
) : niko_MPC_baseNikoScript() {

    var advanceTimeSinceStart = 0.0f
    var graceRuns = 0

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
            val tracker = getSatelliteBattleTracker()
            val battle = fleet.battle
            if (battle != null && !tracker.areSatellitesInvolvedInBattle(battle, handler!!)) { // sanity
                tracker.associateSatellitesWithBattle(battle, handler!!, battle.pickSide(fleet))
            }
        }
        //graceRuns--;
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        getRidOfFleet()
        fleet.removeScript(this)
        fleet.setTemporaryFleetDespawner(null)

        return true
    }

    override fun start() {
        fleet.addScript(this)
    }

    override fun stop() {
        fleet.removeScript(this)
    }

    //arbitrary number
    private fun getRidOfFleet() {
        val vanish = advanceTimeSinceStart < 1 //arbitrary number
        fleet.satelliteFleetDespawn(vanish)
    }
}
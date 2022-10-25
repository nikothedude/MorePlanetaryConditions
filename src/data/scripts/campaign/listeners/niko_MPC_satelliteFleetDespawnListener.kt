package data.scripts.campaign.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_fleetUtils.getTemporaryFleetDespawner
import data.utilities.niko_MPC_fleetUtils.isDummyFleet
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet

class niko_MPC_satelliteFleetDespawnListener : BaseFleetEventListener() {
    /** Here, and nowhere else, should we handle post-deletion GC prep. NOWHERE ELSE GODDAMN IT. */
    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        super.reportFleetDespawnedToListener(fleet, reason, param)
        if (fleet == null) return
        if (!fleet.isSatelliteFleet()) {
            niko_MPC_debugUtils.logDataOf(fleet)
            return niko_MPC_debugUtils.displayError("$this had a satellite fleet despawn listener despite failing isSatelliteFleet()")
        }
        val handler = fleet.getSatelliteEntityHandler() ?: return niko_MPC_debugUtils.displayError("$fleet had" +
                "null handler despite having despawned and passing !fleet.isSatelliteFleet()")

        niko_MPC_debugUtils.log.info("$fleet, ${fleet.name} is satellite fleet and hit despawnlistener")
        fleet.postDeletionGCPrep()
        fleet.removeEventListener(this)
        Global.getSector().listenerManager.removeListener(this)
    }
}

private fun CampaignFleetAPI.postDeletionGCPrep() {
    if (!this.isSatelliteFleet()) {
        niko_MPC_debugUtils.logDataOf(this)
        return niko_MPC_debugUtils.displayError("satellite fleet post gc called on non-satellite fleet, $this")
    }
    this.orbit = null
    val despawnerScript: niko_MPC_temporarySatelliteFleetDespawner? = getTemporaryFleetDespawner()
    if (despawnerScript == null) {
        if (!isDummyFleet()) niko_MPC_debugUtils.displayError("$this was a non-dummy satellite fleet that despawned with no temporary fleet despawner")
    } else despawnerScript.delete()
    val handler = getSatelliteEntityHandler() ?: return niko_MPC_debugUtils.displayError(
        "$this had no handler" +
                " during satellite fleet gc prep"
    )
    handler.doSatelliteFleetPostDeletionGCPrep(this)
}

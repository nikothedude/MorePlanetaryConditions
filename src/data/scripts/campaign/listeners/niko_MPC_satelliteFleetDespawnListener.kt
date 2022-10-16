package data.scripts.campaign.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import data.utilities.niko_MPC_fleetUtils.genericPreDeleteSatelliteFleetCleanup

class niko_MPC_satelliteFleetDespawnListener : BaseFleetEventListener() {
    /**
     * This listener is attached to every satellite fleet that spawns.
     * Serves to remove scripts and listeners and memorykeys, as well as a few others.
     */
    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI, reason: FleetDespawnReason, param: Any) {
        super.reportFleetDespawnedToListener(fleet, reason, param)
        genericPreDeleteSatelliteFleetCleanup(fleet)
        fleet.removeEventListener(this)
        Global.getSector().listenerManager.removeListener(this)
    }
}
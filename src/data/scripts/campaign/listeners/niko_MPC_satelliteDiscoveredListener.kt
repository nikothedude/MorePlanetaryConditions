package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener
import data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteHandlerAlternate
import data.utilities.niko_MPC_satelliteUtils.isCustomEntitySatellite

class niko_MPC_satelliteDiscoveredListener : DiscoverEntityListener {
    override fun reportEntityDiscovered(entity: SectorEntityToken) {
        if (isCustomEntitySatellite(entity)) return
        val handler = entity.getCosmeticSatelliteHandler() ?: return
        for (satellite in handler.getOrbitalSatellites()) {
            satellite.sensorProfile = 9999999999f
        }
    }
}
package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_satelliteUtils.isCosmeticSatellite

class niko_MPC_satelliteDiscoveredListener : DiscoverEntityListener {
    override fun reportEntityDiscovered(entity: SectorEntityToken) {
        if (entity !is CustomCampaignEntityAPI || !entity.isCosmeticSatellite()) {
            displayError("$entity not custom campaign entity/cosmetic satellite during $this reportentitydiscovered")
            return
        }
        val handler = entity.getSatelliteEntityHandler()
        if (handler == null) {
            displayError("$this had no handler despite being a cosmetic satellite")
            return
        }
        for (satellite in handler.cosmeticSatellites) {
            satellite.sensorProfile = 9999999f
        }
    }
}
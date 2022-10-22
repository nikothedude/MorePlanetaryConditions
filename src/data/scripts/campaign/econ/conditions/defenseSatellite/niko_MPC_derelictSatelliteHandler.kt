package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI

class niko_MPC_derelictSatelliteHandler private constructor(
    entity: SectorEntityToken?,
    market: MarketAPI?,
    condition: niko_MPC_antiAsteroidSatellitesBase?
): niko_MPC_satelliteHandlerCore(entity, market, condition) {

    companion object derelictHandlerFactory {
        fun createNewHandlerInstance(entity: SectorEntityToken?, market: MarketAPI?, condition: niko_MPC_antiAsteroidSatellitesBase?): niko_MPC_derelictSatelliteHandler {
            val handler = niko_MPC_derelictSatelliteHandler(entity, market, condition)
            handler.postConstructInit()
            return handler
        }
    }
}
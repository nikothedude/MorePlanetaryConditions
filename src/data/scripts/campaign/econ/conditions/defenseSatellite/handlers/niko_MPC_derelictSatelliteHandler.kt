package data.scripts.campaign.econ.conditions.defenseSatellite.handlers

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAI
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import org.lazywizard.lazylib.MathUtils

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

    override val satelliteConstructionFactionId: String = "derelictSatelliteBuilder"
    override val cosmeticSatelliteId: String = "niko_MPC_derelict_anti_asteroid_satellite"
    override val satelliteFleetName: String = "Domain-era anti-asteroid satellites"
    override val maximumSatelliteFleetFp: Float = 130f
    override var satelliteOrbitDistance: Float = 15.0f
        set(value) { field = value }

    override fun assignCommanderToSatelliteFleet(satelliteFleet: CampaignFleetAPI): PersonAPI? {
        return AICoreOfficerPluginImpl().createPerson(Commodities.GAMMA_CORE, currentSatelliteFactionId, MathUtils.getRandom())
    }

    override fun createSatelliteFleetAI(satelliteFleet: CampaignFleetAPI): CampaignFleetAIAPI {
        return niko_MPC_satelliteFleetAI(satelliteFleet as CampaignFleet)
    }
}
package data.scripts.campaign.econ.conditions.defenseSatellite.handlers

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.campaign.ai.ModularFleetAI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAI
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAITacticalModule
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

class niko_MPC_derelictSatelliteHandler private constructor(
    entity: SectorEntityToken
): niko_MPC_satelliteHandlerCore(entity) {

    companion object derelictHandlerFactory {
        fun createNewHandlerInstance(entity: SectorEntityToken): niko_MPC_derelictSatelliteHandler {
            val handler = niko_MPC_derelictSatelliteHandler(entity)
            handler.postConstructInit()
            return handler
        }
    }

    override val conditionId: String = "niko_MPC_antiAsteroidSatellites_derelict"

    override val satelliteConstructionFactionId: String = "derelictSatelliteBuilder"
    override val cosmeticSatelliteId: String = "niko_MPC_derelict_anti_asteroid_satellite"
    override val satelliteFleetName: String = "Domain-era anti-asteroid satellites"
    override val maximumSatelliteFleetFp: Float = 130f
        get() {
            return ((field+niko_MPC_settings.SATELLITE_FLEET_FP_BONUS_INCREMENT)*niko_MPC_settings.SATELLITE_FLEET_FP_BONUS_MULT).toFloat()
        }
    override var satelliteOrbitDistance: Float = 15.0f

    override fun assignCommanderToSatelliteFleet(satelliteFleet: CampaignFleetAPI): PersonAPI? {
        return AICoreOfficerPluginImpl().createPerson(Commodities.GAMMA_CORE, currentSatelliteFactionId, MathUtils.getRandom())
    }

    override fun createSatelliteFleetAI(satelliteFleet: CampaignFleetAPI): CampaignFleetAIAPI {
        val ai = ModularFleetAI(satelliteFleet as CampaignFleet)
        ai.tacticalModule = niko_MPC_satelliteFleetAITacticalModule(satelliteFleet, ai)
        return ai
    }
}
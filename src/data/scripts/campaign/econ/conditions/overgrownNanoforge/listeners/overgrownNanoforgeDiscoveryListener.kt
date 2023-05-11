package data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.listeners.SurveyPlanetListener
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge

class overgrownNanoforgeDiscoveryListener(): SurveyPlanetListener {

    override fun reportPlayerSurveyedPlanet(planet: PlanetAPI?) {
        if (planet == null) return

        val surveyedMarket = planet.market ?: return
        val nanoforge = surveyedMarket.getOvergrownNanoforge() ?: return

        val handler = nanoforge.getHandlerWithUpdate()
        handler.discovered = true
    }

}
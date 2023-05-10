package data.scripts.campaign.econ.conditions.overgrownNanoforge.listeners

class overgrownNanoforgeDiscoveryListener(): SuveryPlanetListener {

    override fun reportPlayerSurveyedPlanet(planet: PlanetAPI?) {
        if (planet == null) return

        val surveyedMarket = planet.market ?: return
        val nanoforge = surveyedMarket.getOvergrownNanoforge() ?: return

        val handler = nanoforge.getHandlerWithUpdate()
        handler.discovered = true
    }

}
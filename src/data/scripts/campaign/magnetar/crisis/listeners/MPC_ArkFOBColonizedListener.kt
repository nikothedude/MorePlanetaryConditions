package data.scripts.campaign.magnetar.crisis.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener
import data.scripts.campaign.magnetar.crisis.MPC_fractalCoreFactor
import data.utilities.niko_MPC_ids

class MPC_ArkFOBColonizedListener: PlayerColonizationListener {
    override fun reportPlayerColonizedPlanet(planet: PlanetAPI?) {
        if (planet?.id != niko_MPC_ids.MPC_FOB_ID) return
        planet.customDescriptionId = "MPC_IAIICFOBReclaimed"
    }

    override fun reportPlayerAbandonedColony(colony: MarketAPI?) {
        if (colony?.id != MPC_fractalCoreFactor.FOB_MARKET_ID) return
        colony.primaryEntity?.customDescriptionId = "MPC_IAIICFOBDecivved"
    }
}
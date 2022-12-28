package data.scripts.everyFrames

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_antiAsteroidSatellitesBase
import data.scripts.campaign.econ.conditions.overgrownNanoforge.niko_MPC_overgrownNanoforge
import data.utilities.niko_MPC_industryIds

class niko_MPC_overgrownNanoforgeRemovalScript(
    entity: SectorEntityToken, conditionId: String, condition: niko_MPC_overgrownNanoforge? = null):
    niko_MPC_conditionRemovalScript(entity, conditionId, condition) {

    override fun shouldDelete(market: MarketAPI): Boolean {
        val superResult = super.shouldDelete(market)
        return (superResult || !market.hasIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId))
    }

    override fun deleteItem() {
        condition?.delete()
    }
}
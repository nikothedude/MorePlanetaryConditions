package data.scripts.campaign.econ

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.misc.niko_MPC_satelliteHandler
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_satelliteUtils

abstract class niko_MPC_antiAsteroidSatellitesBase : niko_MPC_industryAddingCondition() {

    init {
        industryIds.add(niko_MPC_industryIds.luddicPathSuppressorStructureId)
    }

    override fun apply(id: String) {
        super.apply(id)

        val primaryEntity: SectorEntityToken? = market.primaryEntity
        if (primaryEntity != null) {
            val satelliteHandler : niko_MPC_satelliteHandler? = niko_MPC_satelliteUtils.getEntitySatelliteHandler(getMarket())
        }
    }

    protected fun getMarket(): MarketAPI {
        return market
    }

}
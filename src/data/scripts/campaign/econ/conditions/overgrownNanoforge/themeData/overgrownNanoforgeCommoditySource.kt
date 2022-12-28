package data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeDemandData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeSupplyData
import data.scripts.campaign.econ.industries.overgrownJunk.niko_MPC_baseOvergrownNanoforgeIndustry
import data.scripts.campaign.econ.industries.overgrownJunk.niko_MPC_overgrownNanoforgeIndustry
import data.utilities.niko_MPC_miscUtils.getOvergrownNanoforgeCommodities
import data.utilities.niko_MPC_miscUtils.getOvergrownNanoforgeCondition
import data.utilities.niko_MPC_overgrownNanoforgeCommodityDataStore
import data.utilities.overgrownNanoforgeCommoditySetupData

abstract class overgrownNanoforgeCommoditySource(
    var industry: niko_MPC_overgrownNanoforgeIndustry
) {

    val supplyData: MutableMap<String, overgrownNanoforgeSupplyData> = HashMap()

    //todo: read.
    /** Each source should be cumulative as well as it's demand
     * Each source should specify the commodities and effect provided as well as the defecit effects
     * Sources should be able to choose if demand is shared between certain types of supply or specific to its own
     * Ex. Extraction (mining for ore, organics, etc) all sharing a heavy machinery demand and using the highest value
     * */

    abstract val id: String

    abstract fun getSupplyOf(
        commodityId: String,
        market: MarketAPI? = null,
        industry: niko_MPC_baseOvergrownNanoforgeIndustry? = null,
    ): overgrownNanoforgeSupplyData?

    fun getSupply(): HashMap<String, overgrownNanoforgeSupplyData> {
        val supply = HashMap<String, overgrownNanoforgeSupplyData>()
        for (commodityId in niko_MPC_overgrownNanoforgeCommodityDataStore.keys) {
            val supplyData = getSupplyOf(commodityId, getOurMarket(), industry) ?: continue
            supply[commodityId] = supplyData
        }
        return supply
    }

    abstract fun getDemandFor(commodityId: String): overgrownNanoforgeDemandData

    open fun getExtraDemand(): HashMap<String, Float> = HashMap()

    open fun doExtraEffects() {
        return
    }

    fun applyToData(commodityData: overgrownNanoforgeCommodityData? = getOurCommodityData()) {
        if (commodityData == null) return
        commodityData.applySource(this)
    }

    private fun getOurCommodityData(): overgrownNanoforgeCommodityData? {
        val market = getOurMarket()
        return market.getOvergrownNanoforgeCondition()?.commodityData
    }

    private fun getOurMarket(): MarketAPI {
        return industry.market
    }


}

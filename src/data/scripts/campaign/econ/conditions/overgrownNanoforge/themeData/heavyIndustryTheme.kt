package data.scripts.campaign.econ.conditions.overgrownNanoforge.themeData

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeDemandData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeSourceIds.OVERGROWN_NANOFORGE_HEAVY_INDUSTRY_SOURCE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeSupplyData
import data.scripts.campaign.econ.industries.overgrownJunk.niko_MPC_baseOvergrownNanoforgeIndustry

class heavyIndustryTheme(): overgrownNanoforgeTheme() {
    override val id = OVERGROWN_NANOFORGE_HEAVY_INDUSTRY_SOURCE

    fun getSuppliesOutput(marketSize: Int): Int {
        return (8 + marketSize / 3)
    }
    fun getSuppliesDemand(marketSize: Int): overgrownNanoforgeDemandData {
        return overgrownNanoforgeDemandData(id!!, overgrownNanoforgeCategories.HEAVY_INDUSTRY_THEME, Commodities.SUPPLIES,)
    }

    fun getHeavyMachineryOutput(marketSize: Int): Int {
        return (8 + marketSize / 3)
    }

    fun getShipOutput(marketSize: Int): Int {
        return (8 + marketSize / 3)
    }
    override fun getSupplyOf(
        commodityId: String,
        market: MarketAPI?,
        industry: niko_MPC_baseOvergrownNanoforgeIndustry?
    ): overgrownNanoforgeSupplyData? {
        val marketSize = market?.size ?: return null
        when (commodityId) {
            Commodities.SUPPLIES -> (return overgrownNanoforgeSupplyData(
                id!!, overgrownNanoforgeCategories.HEAVY_INDUSTRY_THEME, commodityId, getSuppliesOutput(marketSize), getDemandFor(commodityId)))
        }
        return null
    }

    override fun getSupply(market: MarketAPI?, overgrownNanoforgeIndustry: niko_MPC_baseOvergrownNanoforgeIndustry?): HashMap<String, Int> {
        val marketSize: Int = market?.size ?: 0
        return hashMapOf(
            Pair(Commodities.SUPPLIES, getSuppliesOutput(marketSize)),
            Pair(Commodities.HEAVY_MACHINERY, getHeavyMachineryOutput(marketSize)),
            Pair(Commodities.SHIPS, getShipOutput(marketSize))
        )
    }

    override fun applyToData(commodityData: overgrownNanoforgeCommodityData) {
        TODO("Not yet implemented")
    }
}
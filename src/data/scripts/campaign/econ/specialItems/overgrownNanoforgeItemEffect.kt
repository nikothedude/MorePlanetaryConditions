package data.scripts.campaign.econ.specialItems

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.utilities.niko_MPC_marketUtils.isPrimaryHeavyIndustry
import lunalib.lunaExtensions.getMarketsCopy

class overgrownNanoforgeItemEffect(id: String?, supplyIncrease: Int, demandIncrease: Int) : BoostIndustryInstallableItemEffect(
    id,
    supplyIncrease,
    demandIncrease) {
    var industry: Industry? = null

    override fun apply(industry: Industry?) {
        if (industry == null) return
        this.industry = industry

        increaseSupplyAndDemand()
        super.apply(industry)

        val shipOutput = industry.getSupply(Commodities.SHIPS)
        if (shipOutput != null && shipOutput.quantity.modifiedInt > 0) {
            modifyShipProduction(industry)
        }

        industry.upkeep.unmodifyFlat(id)
        industry.upkeep.modifyMultAlways(id, getUpkeepMult(), getDesc())
        industry.upkeep.modifyFlatAlways(id, getUpkeepIncrement(), getDesc())
    }

    private fun increaseSupplyAndDemand() {
        if (industry == null) return
        val ourIndustry = industry!!
        val desc = getDesc()

        for (supply in ourIndustry.allSupply) {
            val commodity = supply.commodityId
            if (!supplyIsValidToAlter(commodity)) continue

            supply.quantity.unmodifyFlat(id)
            supply.quantity.modifyMultAlways(id, getSupplyMult(), desc)
            supply.quantity.modifyFlatAlways(id, getSupplyIncrement(), desc)

        }

        for (demand in ourIndustry.allDemand) {
            demand.quantity.unmodifyFlat(id)
            demand.quantity.modifyMultAlways(id, getDemandMult(), desc)
            demand.quantity.modifyFlatAlways(id, getDemandIncrement(), desc)
        }
    }

    private fun resetSupplyAndDemand() {
        if (industry == null) return
        val ourIndustry = industry!!

        for (supply in ourIndustry.allSupply) {
            supply.quantity.unmodify(id)
        }

        for (demand in ourIndustry.allDemand) {
            demand.quantity.unmodify(id)
        }
    }

    private fun getSupplyMult(): Float {
        return 2f
    }

    private fun getSupplyIncrement(): Float {
        return 1f
    }

    private fun getDemandMult(): Float {
        return 1.5f
    }

    private fun getDemandIncrement(): Float {
        return 1f
    }

    companion object {
        fun supplyIsValidToAlter(commodity: String): Boolean {
            if (commodity == Commodities.CREW ||
                commodity == Commodities.MARINES ||
                commodity == Commodities.ORGANS) return false
            return true
        }
    }

    private fun getUpkeepMult(): Float {
        return 2.5f
    }

    private fun getUpkeepIncrement(): Float {
        return 5000f
    }

    private fun modifyShipProduction(industry: Industry) {
        val market = industry.market ?: return
        val dynamicStats = market.stats.dynamic
        val desc = getDesc()
        dynamicStats.getStat(Stats.CUSTOM_PRODUCTION_MOD).modifyFlat(id, 0f) //just be safe
        dynamicStats.getStat(Stats.CUSTOM_PRODUCTION_MOD).modifyMultAlways(id, getShipProductionMult(), desc)
        dynamicStats.getStat(Stats.CUSTOM_PRODUCTION_MOD).modifyFlatAlways(id, getShipProductionIncrement(market), desc)

        dynamicStats.getStat(Stats.PRODUCTION_QUALITY_MOD).modifyMultAlways(id, getProductionQualityMult(), desc)

        if (industry.isPrimaryHeavyIndustry()) {
            val faction = market.faction ?: return
            for (factionMarket in faction.getMarketsCopy()) {
                factionMarket.stats.dynamic.getStat(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(id, getShipSizeMult(), desc)
            }
        }
    }

    private fun getProductionQualityMult(): Float {
        return 0.25f
    }

    private fun getShipProductionMult(): Float {
        return 5f
    }

    private fun getShipSizeMult(): Float {
        return 4f
    }

    private fun getShipProductionIncrement(market: MarketAPI): Float {
        val marketSize = market.size
        return 1000f*marketSize
    }

    override fun unapply(industry: Industry?) {
        super.unapply(industry)
        if (industry == null) return

        resetSupplyAndDemand()

        industry.upkeep.unmodify(id)

        val market = industry.market ?: return
        val dynamicStats = market.stats.dynamic

        dynamicStats.getStat(Stats.CUSTOM_PRODUCTION_MOD).unmodify(id)
        dynamicStats.getStat(Stats.PRODUCTION_QUALITY_MOD).unmodify(id)
        val faction = market.faction ?: return
        for (factionMarket in faction.getMarketsCopy()) {
            factionMarket.stats.dynamic.getStat(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
        }

    }

    private fun getDesc(): String {
        if (industry == null) return "error. you shouldnt see this"
        val market = industry!!.market ?: return "weirder error. you shouldnt see this"

        return "$market ${industry!!.nameForModifier} ${getName()}"
    }

    private fun getName(): String {
        return "Overgrown Nanoforge"
    }

}

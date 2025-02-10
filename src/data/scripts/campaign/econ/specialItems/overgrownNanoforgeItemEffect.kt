package data.scripts.campaign.econ.specialItems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BoostIndustryInstallableItemEffect
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.isApplied
import data.utilities.niko_MPC_marketUtils.isPrimaryHeavyIndustry
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import lunalib.lunaExtensions.getMarketsCopy

class overgrownNanoforgeItemEffect(id: String?, supplyIncrease: Int, demandIncrease: Int) : BoostIndustryInstallableItemEffect(
    id,
    supplyIncrease,
    demandIncrease) {
    val fleetSizeRemover = overgrownNanoforgeFleetSizeRemovalScript()
    var industry: Industry? = null
        set(value) {
            fleetSizeRemover.industry = value
            field = value
        }
    override fun apply(industry: Industry?) {
        if (industry == null) return
        this.industry = industry

        increaseSupplyAndDemand(industry)
        super.apply(industry)

        val shipOutput = industry.getSupply(Commodities.SHIPS)
        if (shipOutput != null && shipOutput.quantity.modifiedInt > 0) {
            modifyShipProduction(industry)
        }

        industry.upkeep.unmodifyFlat(id)
        industry.upkeep.modifyFlatAlways(id, getUpkeepIncrement(), getName())
        industry.market?.hazard?.modifyFlat(id, getHazardIncrement(), getDesc(industry))
    }

    private fun increaseSupplyAndDemand(industry: Industry) {
        val desc = getDesc(industry)

        for (supply in industry.allSupply) {
            val commodity = supply.commodityId
            if (!supplyIsValidToAlter(commodity)) continue

            supply.quantity.unmodifyFlat(id)
            supply.quantity.modifyMultAlways(id, getSupplyMult(), desc)

        }

        for (demand in industry.allDemand) {
            demand.quantity.unmodifyFlat(id)
            demand.quantity.modifyMultAlways(id, getDemandMult(), desc)
        }
    }

    private fun resetSupplyAndDemand(industry: Industry) {

        for (supply in industry.allSupply) {
            supply.quantity.unmodify(id)
        }

        for (demand in industry.allDemand) {
            demand.quantity.unmodify(id)
        }
    }

    private fun getSupplyMult(): Float {
        return 2.5f
    }

    private fun getDemandMult(): Float {
        return 1.5f
    }

    private fun getHazardIncrement(): Float {
        return 0.15f
    }

    companion object {
        fun supplyIsValidToAlter(commodity: String): Boolean {
            if (commodity == Commodities.CREW ||
                commodity == Commodities.MARINES ||
                commodity == Commodities.ORGANS) return false
            return true
        }
    }

    private fun getUpkeepIncrement(): Float {
        return 250f
    }

    private fun modifyShipProduction(industry: Industry) {
        val market = industry.market ?: return
        val dynamicStats = market.stats.dynamic
        val desc = getDesc(industry)
        dynamicStats.getMod(Stats.CUSTOM_PRODUCTION_MOD).modifyFlat(id, 0f) //just be safe
        dynamicStats.getMod(Stats.CUSTOM_PRODUCTION_MOD).modifyMultAlways(id, getShipProductionMult(), desc)
        dynamicStats.getMod(Stats.CUSTOM_PRODUCTION_MOD).modifyFlatAlways(id, getShipProductionIncrement(market), desc)

        dynamicStats.getMod(Stats.PRODUCTION_QUALITY_MOD).modifyMultAlways(id, getProductionQualityMult(), desc)

        val shouldIncreaseFleetsize: Boolean = industry.isPrimaryHeavyIndustry()
        val faction = market.faction ?: return
        for (factionMarket in faction.getMarketsCopy()) {
            if (shouldIncreaseFleetsize) {
                increaseFleetSize(factionMarket, desc)
            } else {
                resetFleetSize(factionMarket)
            }
        }
    }

    private fun increaseFleetSize(factionMarket: MarketAPI, desc: String) {
        factionMarket.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMultAlways(id, getShipSizeMult(), desc)
        factionMarket.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyMultAlways(getFleetQualityId(), getProductionQualityMult(), desc)
        //TODO: for some fucking reason this quality mod isnt showing up in the fuckin descriptionnnn of the damn quality screen
        fleetSizeRemover.trackMarket(factionMarket)
    }

    private fun getFleetQualityId(): String {
        return id + 1
    }

    private fun resetFleetSize(factionMarket: MarketAPI) {
        fleetSizeRemover.unapplyFleetsize(factionMarket)
    }

    private fun getProductionQualityMult(): Float {
        return 0.25f
    }

    private fun getShipProductionMult(): Float {
        return getShipSizeMult()
    }

    private fun getShipSizeMult(): Float {
        return 2f
    }

    val shipProductionBaseIncrement = 1000f
    private fun getShipProductionIncrement(market: MarketAPI): Float {
        val marketSize = market.size
        return shipProductionBaseIncrement*marketSize
    }

    override fun unapply(industry: Industry?) {
        super.unapply(industry)
        if (industry == null) return

        resetSupplyAndDemand(industry)

        industry.upkeep.unmodify(id)

        val market = industry.market ?: return
        val dynamicStats = market.stats.dynamic

        dynamicStats.getMod(Stats.CUSTOM_PRODUCTION_MOD).unmodify(id)
        dynamicStats.getMod(Stats.PRODUCTION_QUALITY_MOD).unmodify(id)
        dynamicStats.getMod(Stats.FLEET_QUALITY_MOD).unmodify(getFleetQualityId())
        val faction = market.faction ?: return
        for (factionMarket in faction.getMarketsCopy()) {
            resetFleetSize(factionMarket)
            factionMarket.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
        }
        industry.market?.hazard?.unmodify(id)
        fleetSizeRemover.delete()

    }
    private fun getDesc(industry: Industry): String {
        val market = industry.market ?: return "weirder error. you shouldnt see this"

        return "${market.name} ${industry.nameForModifier} ${getName()}"
    }

    override fun addItemDescriptionImpl(industry: Industry?, text: TooltipMakerAPI?, data: SpecialItemData?,
        mode: InstallableIndustryItemPlugin.InstallableItemDescriptionMode?, pre: String?, pad: Float
    ) {
        if (text == null) return
        val description = pre + "Increases all supply on installed industries by %s, all demand by " +
                "%s, and increases upkeep by %s per month. Also increases hazard rating of the market by %s.\n" +
                "\n" +
                "If installed in a heavy industry, increases production capacity by %s and %s. " +
                "If said heavy industry is the primary ship producer of it's faction, increases faction-wide fleet size by %s, but decreases ship quality by %s. \n" +
                "\n" +
                "On habitable worlds, causes pollution which becomes permanent."
        val para = text.addPara(description, pad, Misc.getHighlightColor(),
            "${getSupplyMult().trimHangingZero()}x", "${getDemandMult().trimHangingZero()}x", "${getUpkeepIncrement().toInt()} credits", "${(getHazardIncrement() * 100f).toInt()}%",

            "${getShipProductionMult().trimHangingZero()}x", "${shipProductionBaseIncrement.toInt()} * Market Size", "${getShipSizeMult().trimHangingZero()}x",
            "${((1 - getProductionQualityMult()) * 100f).trimHangingZero()}%")

        para.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(),
            Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
    }

    private fun getName(): String {
        return "Overgrown Nanoforge"
    }

    inner class overgrownNanoforgeFleetSizeRemovalScript(
    ): niko_MPC_baseNikoScript() {
        var industry: Industry? = null
        val trackedMarkets = HashSet<MarketAPI>()

        override fun startImpl() {
            Global.getSector().addScript(this)
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
        }

        override fun runWhilePaused(): Boolean {
            return false
        }

        override fun advance(amount: Float) {
            if (industry == null) return
            if (!industry!!.isApplied()) return
            val industryFactionId = industry!!.market.factionId ?: return
            for (market in ArrayList(trackedMarkets)) {
                val marketFactionId = market.factionId
                if (market.isInvalid() || industryFactionId != marketFactionId) {
                    unapplyFleetsize(market)
                }
            }
        }

        fun unapplyFleetsize(market: MarketAPI) {
            market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).unmodify(getFleetQualityId())
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
            untrackMarket(market)
        }

        fun trackMarket(factionMarket: MarketAPI) {
            trackedMarkets += factionMarket
        }
        fun untrackMarket(factionMarket: MarketAPI) {
            trackedMarkets -= factionMarket
        }

        private fun MarketAPI.isInvalid(): Boolean {
            return (isPlanetConditionMarketOnly)
        }

    }
}

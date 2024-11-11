package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.econ.impl.Spaceport
import com.fs.starfarer.api.impl.campaign.econ.impl.Waystation
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.Pair
import com.fs.starfarer.campaign.econ.Market
import com.fs.starfarer.campaign.econ.PlanetConditionMarket
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCondition
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_ids.overgrownNanoforgeHandlerMemoryId
import data.utilities.niko_MPC_settings.MAX_STRUCTURES_ALLOWED
import lunalib.lunaExtensions.getMarketsCopy
import org.lwjgl.util.vector.Vector2f

object niko_MPC_marketUtils {

    val commodities = hashSetOf(
        Commodities.FUEL,
        Commodities.DRUGS,
        Commodities.FOOD,
        Commodities.DOMESTIC_GOODS,
        Commodities.HEAVY_MACHINERY,
        Commodities.HAND_WEAPONS,
        Commodities.METALS,
        Commodities.ORE,
        Commodities.RARE_METALS,
        Commodities.RARE_ORE,
        Commodities.SHIPS,
        Commodities.LUXURY_GOODS,
        Commodities.ORGANICS,
        Commodities.VOLATILES,
        Commodities.SUPPLIES,
        "IndEvo_parts",
    )

    @JvmStatic
    fun removeNonNanoforgeProducableCommodities(list: MutableSet<String>): MutableSet<String> {
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (overgrownNanoforgeCommodityDataStore[entry] == null) {
                iterator.remove()
                continue
            }
        }
        return list
    }

    @JvmStatic
    fun MarketAPI.getProducableCommodityModifiers(): MutableMap<String, Int> {
        // things that just arent producable without certain conditions
        val removeIfNull: HashSet<String> = hashSetOf(Commodities.FOOD, Commodities.ORE, Commodities.ORGANICS, Commodities.RARE_ORE, Commodities.VOLATILES)
        val producableCommodities = ArrayList(commodities)
        val producableModifiers = HashMap<String, Int>()
        for (commodityId in producableCommodities) {
            val modifier = getCommodityModifier(commodityId)
            if (modifier == null) {
                if (removeIfNull.contains(commodityId)) {
                    producableModifiers -= commodityId
                    continue
                }
            }
            producableModifiers[commodityId] = modifier ?: 0
        }
        return producableModifiers
    }

    @JvmStatic
    fun MarketAPI.getProducableCommoditiesForOvergrownNanoforge(): MutableSet<String> {
        val producableCommodities = getProducableCommodities()
        return removeNonNanoforgeProducableCommodities(producableCommodities)
    }

    @JvmStatic
    fun MarketAPI.getProducableCommodities(): MutableSet<String> {
        return getProducableCommodityModifiers().keys
    }

    fun MarketAPI.getCommodityModifier(id: String): Int? {
        var modifier: Int? = null
        for (mc in conditions) {
            val commodity = ResourceDepositsCondition.COMMODITY[mc.id] ?: continue
            if (commodity == id) {
                val amount = ResourceDepositsCondition.MODIFIER[mc.id] ?: continue
                if (modifier == null) modifier = 0
                modifier += amount
            }
        }
        return modifier
    }

    @JvmStatic
    fun MarketAPI.getOvergrownNanoforge(): overgrownNanoforgeIndustry? {
        return getIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId) as? overgrownNanoforgeIndustry
    }

    @JvmStatic
    fun MarketAPI.getOvergrownNanoforgeCondition(): overgrownNanoforgeCondition? {
        return getCondition(niko_MPC_ids.overgrownNanoforgeConditionId)?.plugin as? overgrownNanoforgeCondition
    }

    @JvmStatic
    fun MarketAPI.hasMaxStructures(): Boolean {
        return (getVisibleIndustries().size == MAX_STRUCTURES_ALLOWED)
    }

    fun MarketAPI.getVisibleIndustries(): MutableList<Industry> {
        val visibleIndustries = ArrayList<Industry>()
        for (industry in industries) {
            if (industry.isVisible()) visibleIndustries += industry
        }
        return visibleIndustries
    }

    fun Industry.isVisible(): Boolean {
        return (!this.isHidden)
    }

    @JvmStatic
    fun MarketAPI.exceedsMaxStructures(): Boolean {
        return (getVisibleIndustries().size > MAX_STRUCTURES_ALLOWED)
    }


    @JvmStatic
    fun Industry.isOrbital(): Boolean {
        return (this is OrbitalStation || this is Waystation)
    }

    @JvmStatic
    fun MarketAPI.hasCustomControls(): Boolean {
        val allowsFreePortTrade = (faction?.getCustomBoolean(Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE)) ?: false
        return (!isFreePort && !allowsFreePortTrade)
    }

    @JvmStatic
    fun Industry.isPrimaryHeavyIndustry(): Boolean {
        val shipProduction = getSupply(Commodities.SHIPS) ?: return false
        if (market == null || market.faction == null) return false
        val ourProduction = shipProduction.quantity.modifiedInt
        if (ourProduction <= 0) return false
        val faction = market.faction
        for (colony in faction.getMarketsCopy()) {
            if (colony.industries.any { it.getSupply(Commodities.SHIPS) != null && it.getSupply(Commodities.SHIPS).quantity.modifiedInt > ourProduction }) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun MarketAPI.getNextOvergrownJunkId(): String? {
        val designation = getNextOvergrownJunkDesignation() ?: return null
        return overgrownNanoforgeJunkHandler.baseStructureId + designation
    }

    fun MarketAPI.getNextOvergrownJunkDesignation(): Int? {
        if (exceedsMaxStructures()) return null
        val existingJunkHandlers = getOvergrownJunkHandlers()
        val existingDesignations = HashSet<Int>()
        for (handler in existingJunkHandlers) {
            existingDesignations += handler.getOurDesignation() ?: continue
        }
        for (designation: Int in 1..overgrownNanoforgeJunkHandler.maxStructuresPossible) {
            if (!existingDesignations.contains(designation)) return designation
        }
        return null
    }

    fun MarketAPI.getOvergrownJunkHandlers(): MutableSet<overgrownNanoforgeJunkHandler> {
        val handlers = HashSet<overgrownNanoforgeJunkHandler>()
        for (designation: Int in 1..overgrownNanoforgeJunkHandler.maxStructuresPossible) {
            val handler = getOvergrownJunkHandler(designation) ?: continue
            handlers += handler
        }
        return handlers
    }

    fun MarketAPI.getOvergrownJunk(): HashSet<overgrownNanoforgeJunk> {
        val junk = HashSet<overgrownNanoforgeJunk>()
        for (structure in industries) {
            if (structure.isJunk()) junk += structure as overgrownNanoforgeJunk
        }
        return junk
    }
    fun Industry.isJunk(): Boolean {
        return (this is overgrownNanoforgeJunk)
    }
    fun Industry.isJunkStructure(): Boolean {
        return (this is baseOvergrownNanoforgeStructure)
    }

    fun Industry.isApplied(): Boolean {
        return (market != null)
    }

    fun MarketAPI.hasNonJunkStructures(): Boolean {
        for (industry in industries) {
            if (!industry.isVisible()) continue
            if (!industry.isJunk()) return true
        }
        return false
    }

    fun MarketAPI.addJunkStructure(id: String, source: overgrownNanoforgeRandomizedSource): overgrownNanoforgeJunk? {
        addIndustry(id)
        val industry = getIndustry(id)
        if (!industry.isJunk()) {
            niko_MPC_debugUtils.displayError("addjunkstructure failed on $this due to cast error")
            logDataOf(this)
            return null
        }
        val junkIndustry = industry as overgrownNanoforgeJunk
        return junkIndustry
    }

    fun MarketAPI.hasJunkStructures(): Boolean {
        return getOvergrownJunk().isNotEmpty()
    }

    fun MarketAPI.purgeOvergrownNanoforgeBuildings() {
        val iterator = this.getOvergrownNanoforgeBuildings().iterator()
        while (iterator.hasNext()) {
            val building = iterator.next()
            building.delete()
        }
    }
    fun MarketAPI.getOvergrownNanoforgeBuildings(): MutableSet<baseOvergrownNanoforgeStructure> {
        val buildings: MutableSet<baseOvergrownNanoforgeStructure> = HashSet()
        buildings.addAll(getOvergrownJunk())
        getOvergrownNanoforge()?.let { buildings += it }

        return buildings
    }

    fun MarketAPI.getOvergrownNanoforgeIndustryHandler(): overgrownNanoforgeIndustryHandler? {
        return memoryWithoutUpdate[overgrownNanoforgeHandlerMemoryId] as? overgrownNanoforgeIndustryHandler
    }

    fun MarketAPI.setOvergrownNanoforgeIndustryHandler(handler: overgrownNanoforgeIndustryHandler?) {
        val currHandler = getOvergrownNanoforgeIndustryHandler()
        if (currHandler != null) {
            niko_MPC_debugUtils.displayError("replacement attempt for overgrown nanoforge handler on ${this.name}")
        }
        memoryWithoutUpdate[overgrownNanoforgeHandlerMemoryId] = handler
    }
    fun MarketAPI.removeOvergrownNanoforgeIndustryHandler() {
        memoryWithoutUpdate.unset(overgrownNanoforgeHandlerMemoryId)
    }

    fun MarketAPI.isValidTargetForOvergrownHandler(): Boolean {
        return (id != "fake_Colonize"
                && (!(isConvertingToMarket() || isConvertingToCondition()))
                && (primaryEntity.market == this))
    }

    fun MarketAPI.isConvertingToMarket(): Boolean {
        return (this is Market && primaryEntity == null && location == Vector2f(0f, 0f) && factionId == null)
    }
    fun MarketAPI.isConvertingToCondition(): Boolean {
        return (this is PlanetConditionMarket && primaryEntity == null && location == Vector2f(0f, 0f))
    }

    fun MarketAPI.shouldHaveOvergrownNanoforgeIndustry(): Boolean {
        return (isInhabited() && getOvergrownNanoforge() == null)
    }

    fun MarketAPI.isInhabited(): Boolean {
        return  (!isPlanetConditionMarketOnly && id != "fake_Colonize" && isInEconomy)
    }

    // i want to fucking obliterate this function
    fun MarketAPI.isDeserializing(): Boolean {

        var result: Boolean = false
        try {
            memoryWithoutUpdate.isEmpty
        } catch (ex: NullPointerException) {
            result = true
        }
        return result ||
                industries == null ||
                surveyLevel == null ||
                stats == null ||
                hazard == null ||
                conditions == null
    }

    fun LocationAPI.isDeserializing(): Boolean {
        var result: Boolean = false
        try {
            terrainCopy
            fleets
        } catch (ex: NullPointerException) {
            result = true
        }
        return result
    }

    fun MarketAPI.getOvergrownJunkHandler(designation: Int): overgrownNanoforgeJunkHandler? {
        val modifiedId = overgrownNanoforgeJunkHandler.baseStructureId + designation
        return getOvergrownJunkHandler(modifiedId)
    }
    fun MarketAPI.getOvergrownJunkHandler(id: String): overgrownNanoforgeJunkHandler? {
        return memoryWithoutUpdate[convertToMemKey(id)] as? overgrownNanoforgeJunkHandler
    }

    fun MarketAPI.setOvergrownNanoforgeJunkHandler(handler: overgrownNanoforgeJunkHandler) {
        val id = handler.cachedBuildingId ?: return
        memoryWithoutUpdate[convertToMemKey(id)] = handler
    }

    fun convertToMemKey(id: String): String {
        return "\$" + id
    }

    fun MarketAPI.getMaxIndustries(): Int {
        return stats.dynamic.getStat(Stats.MAX_INDUSTRIES).modifiedInt
    }

    fun Industry.isPopulationAndInfrastructure(): Boolean {
        return id == Industries.POPULATION
    }

    fun Industry.isSpacePort(): Boolean {
        return this is Spaceport
    }

    fun Industry.getIndustryDisruptTime(): Float {
        return spec.disruptDanger.disruptionDays
    }

    // TODO: update this method if it ever changes, 1:1 with applyDeficitToProduction from baseindustry.java
    fun Industry.applyDeficitToProductionStatic(index: Int, deficit: Pair<String, Int>, vararg commodities: String) {
        if (this !is BaseIndustry) return
        for (commodity in commodities) {
//			if (this instanceof Mining && market.getName().equals("Louise")) {
//				System.out.println("efwefwe");
//			}
            if (getSupply(commodity).quantity.isUnmodified) continue
            supply(index, commodity, -deficit.two, BaseIndustry.getDeficitText(deficit.one))
        }
    }

    // key: escortee, value: escorter
    fun MarketAPI.getEscortFleetList(): MutableMap<CampaignFleetAPI, CampaignFleetAPI> {
        var fleetList = memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEETS_MEMID] as? HashMap<CampaignFleetAPI, CampaignFleetAPI>
        if (fleetList !is HashMap<CampaignFleetAPI, CampaignFleetAPI>) {
            memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEETS_MEMID] = HashMap<CampaignFleetAPI, CampaignFleetAPI>()
            fleetList = memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEETS_MEMID] as HashMap<CampaignFleetAPI, CampaignFleetAPI>
        }
        return fleetList
    }

    fun MarketAPI.getStockpileNumConsumedOverTime(com: CommodityOnMarketAPI, days: Float, available: Int = com.available): Float {
        val demand = com.maxDemand

        var deficitDrawBaseAmount = BaseIndustry.getSizeMult(demand.toFloat()) - BaseIndustry.getSizeMult(available.toFloat())
        deficitDrawBaseAmount *= com.commodity.econUnit

        val drawAmount = deficitDrawBaseAmount * days / 30f

        return drawAmount
    }

    fun MarketAPI.isFractalMarket(): Boolean {
        return (admin.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID)
    }
}

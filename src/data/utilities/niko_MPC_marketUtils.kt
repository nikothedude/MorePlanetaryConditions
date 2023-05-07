package data.utilities

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.econ.impl.Waystation
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.utilities.niko_MPC_debugUtils.logDataOf
import lunalib.lunaExtensions.getMarketsCopy
import niko.MCTE.utils.MCTE_debugUtils.displayError

object niko_MPC_marketUtils {

    const val maxStructureAmount = 12
    val commodities = hashSetOf(Commodities.FUEL, Commodities.DRUGS, Commodities.FOOD, Commodities.DOMESTIC_GOODS,
    Commodities.HEAVY_MACHINERY, Commodities.HAND_WEAPONS, Commodities.METALS, Commodities.ORE, Commodities.RARE_METALS, Commodities.RARE_ORE,
    Commodities.SHIPS, Commodities.LUXURY_GOODS, Commodities.ORGANICS, Commodities.VOLATILES, Commodities.SUPPLIES)

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
        producableModifiers.keys.addAll(producableCommodities)
        for (commodityId in producableCommodities) {
            val modifier = getCommodityModifier(commodityId)
            if (modifier == null) {
                if (removeIfNull.contains(commodityId)) {
                    producableModifiers -= commodityId
                }
                continue
            }
            producableModifiers[commodityId] = modifier
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
    fun MarketAPI.hasMaxStructures(): Boolean {
        return (getVisibleIndustries().size == maxStructureAmount)
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
        return (getVisibleIndustries().size > maxStructureAmount)
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
        if (exceedsMaxStructures()) return null
        val existingDesignations = HashSet<Int>()
        val existingJunk = getOvergrownJunk()
            for (junk in existingJunk) {
                val junkId = junk.id
                existingDesignations += (junkId.filter { it.isDigit() }.toInt())
            }
        for (designation: Int in 1..maxStructureAmount) {
            if (!existingDesignations.contains(designation)) return niko_MPC_industryIds.overgrownNanoforgeJunkStructureId + designation
        }
        return null
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
            displayError("addjunkstructure failed on $this due to cast error")
            logDataOf(this)
            return null
        }
        val junkIndustry = industry as overgrownNanoforgeJunk
        junkIndustry.source = source
        junkIndustry.properlyAdded = true
        return junkIndustry
    }
    fun MarketAPI.hasJunkStructures(): Boolean {
        return getOvergrownJunk().isNotEmpty()
    }

    fun MarketAPI.purgeOvergrownNanoforgeBuildings() {
        val iterator = this.getOvergrownNanoforgeBuildings().iterator()
        while (iterator.hasNext()) {
            val building = iterator.next()
            building.unapply()
        }
    }
    fun MarketAPI.getOvergrownNanoforgeBuildings(): MutableSet<baseOvergrownNanoforgeStructure> {
        val buildings: MutableSet<baseOvergrownNanoforgeStructure> = HashSet()
        buildings.addAll(getOvergrownJunk())
        getOvergrownNanoforge()?.let { buildings += it }

        return buildings
    }
}

package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore

class overgrownNanoforgeAlterSupplySource(
    nanoforge: overgrownNanoforgeIndustry,
    val supply: MutableMap<String, Int> = HashMap()
): overgrownNanoforgeRandomizedEffect(nanoforge) {

    val negativeSupply: MutableMap<String, Int> = HashMap()
    val positiveSupply: MutableMap<String, Int> = HashMap()

    init {
        for (entry in supply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            if (quantity == 0) continue //why would this even ever happen
            if (quantity > 0) positiveSupply[commodityId] = quantity
            if (quantity < 0) negativeSupply[commodityId] = quantity
        }
    }

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isSupplyNegative()) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isSupplyNegative(): Boolean {
        var score = 0f
        for (entry in supply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            val supplyData = overgrownNanoforgeCommodityDataStore[commodityId] ?: continue
            score -= (supplyData.cost * quantity)
        }
        return (score < 0f)
    }

    override fun getName(): String = "Supply Alteration"

    override fun getDescription(): String {
        var desc = "Supply of the overgrown nanoforge modified by"
        for (entry in supply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            desc += " $quantity $commodityId"
        }
        return desc
    }

    override fun applyBenefits() {
        for (entry in positiveSupply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            getIndustry().supply(getId(), commodityId, quantity, getName())
        }
    }

    override fun unapplyBenefits() {
        for (entry in positiveSupply.entries) {
            val commodityId = entry.key

            getIndustry().supply(getId(), commodityId, 0, getName())
        }
    }

    override fun applyDeficits() {
        for (entry in negativeSupply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            getIndustry().supply(getId(), commodityId, quantity, getName())
        }
    }

    override fun unapplyDeficits() {
        for (entry in negativeSupply.entries) {
            val commodityId = entry.key

            getIndustry().supply(getId(), commodityId, 0, getName())
        }
    }
}
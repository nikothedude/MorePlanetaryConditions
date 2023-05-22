package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import kotlin.math.abs

class overgrownNanoforgeAlterSupplySource(
    handler: overgrownNanoforgeHandler,
    val supply: MutableMap<String, Int> = HashMap()
): overgrownNanoforgeRandomizedEffect(handler) {

    override val baseFormat: String = "Nanoforge supply $adjectiveChar by $changeChar"

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
        val structure = getStructure() ?: return
        for (entry in positiveSupply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            structure.supply(getOurId(), commodityId, quantity, getName())
        }
    }

    override fun unapplyBenefits() {
        val structure = getStructure() ?: return
        for (entry in positiveSupply.entries) {
            val commodityId = entry.key

            structure.supply(getOurId(), commodityId, 0, getName())
        }
    }

    override fun applyDeficits() {
        val structure = getStructure() ?: return
        for (entry in negativeSupply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            structure.supply(getOurId(), commodityId, quantity, getName())
        }
    }

    override fun unapplyDeficits() {
        val structure = getStructure() ?: return
        for (entry in negativeSupply.entries) {
            val commodityId = entry.key

            structure.supply(getOurId(), commodityId, 0, getName())
        }
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val effects = ArrayList<String>()
        val toLoopThrough = if (positive) positiveSupply else negativeSupply

        for (entry in toLoopThrough) {
            val spec = Global.getSettings().getCommoditySpec(entry.key) ?: continue
            val nameOfKey = spec.name
            val modifiedFormat = "Nanoforge supply of $nameOfKey %b by %c"
            effects += getFormattedEffect(modifiedFormat, positive, entry)
        }
        return effects
    }

    override fun getChange(positive: Boolean, vararg args: Any): String {
        val pair = args[0] as MutableMap.MutableEntry<String, Int>
        return "${abs(pair.value)}"
    }
}
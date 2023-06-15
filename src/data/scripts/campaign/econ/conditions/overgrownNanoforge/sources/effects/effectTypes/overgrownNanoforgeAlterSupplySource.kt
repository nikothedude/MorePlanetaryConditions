package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import kotlin.math.abs

class overgrownNanoforgeAlterSupplySource(
    handler: overgrownNanoforgeHandler,
    val commodityId: String,
    val amount: Int
): overgrownNanoforgeFormattedEffect(handler), simpleFormat {

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isSupplyNegative()) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isSupplyNegative(): Boolean {
        return amount < 0
    }

    override fun getName(): String = "Supply Alteration"

    override fun getBaseFormat(): String {
        val spec = Global.getSettings().getCommoditySpec(commodityId) ?: return "error"
        val nameOfKey = spec.name
        return "Nanoforge supply of $nameOfKey $adjectiveChar by $changeChar"
    }

    override fun applyEffects() {
        val structure = getStructure() ?: return
        structure.supply(getOurId(), commodityId, amount, getName())
    }

    override fun unapplyEffects() {
        val structure = getStructure() ?: return
        structure.supply(getOurId(), commodityId, 0, getName())
    }


    override fun getStructure(): baseOvergrownNanoforgeStructure? {
        return handler.getCoreHandler().getStructure()
    }

    override fun getChange(positive: Boolean): String {
        return "${abs(amount)}"
    }

    override fun getDisabledCriteria(): Boolean {
        val structure = getStructure() ?: return true
        val supplyStats = structure.getSupply(commodityId).quantity

        val unmodifiedQuantity = quantity.modifiedValue - amount
        if (unmodifiedQuantity <= 0) return true

        return super.getDisabledCriteria()
    }
}
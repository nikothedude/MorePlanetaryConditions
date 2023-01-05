package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams

class overgrownNanoforgeAlterSupplySource(
    nanoforge: overgrownNanoforgeIndustry,
    val supply: MutableMap<String, Int> = HashMap()
): overgrownNanoforgeRandomizedEffect(params) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isSupplyNegative()) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isSupplyNegative(): Boolean {

    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getDescription(): String {
        TODO("Not yet implemented")
    }

    override fun apply() {
        for (entry in supply.entries) {
            val commodityId = entry.key
            val quantity = entry.value

            getIndustry().supply(getId(), commodityId, quantity, getName())
        }
        TODO("Not yet implemented")
    }

    override fun unapply() {
        for (entry in supply.entries) {
            val commodityId = entry.key

            getIndustry().supply(getId(), commodityId, 0, getName())
        }
    }
}
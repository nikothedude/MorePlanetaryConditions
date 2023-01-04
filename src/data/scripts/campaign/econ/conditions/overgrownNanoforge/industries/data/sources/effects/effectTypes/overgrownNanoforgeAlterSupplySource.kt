package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeRandomizedSource

class overgrownNanoforgeAlterSupplySource(
    source: overgrownNanoforgeRandomizedSource,
    val supply: MutableMap<String, Float> = HashMap()
): overgrownNanoforgeRandomizedEffect(source) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isSupplyNegative()) overgrownNanoforgeEffectCategories.DEFECIT else overgrownNanoforgeEffectCategories.BENEFIT
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
            val quantitiy = entry.value

            getIndustry().supply(getId(), TODO())
        }
        TODO("Not yet implemented")
    }

    override fun unapply() {
        TODO("Not yet implemented")
    }
}
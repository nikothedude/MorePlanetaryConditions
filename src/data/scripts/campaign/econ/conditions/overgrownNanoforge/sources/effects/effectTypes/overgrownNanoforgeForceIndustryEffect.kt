package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories

class overgrownNanoforgeForceIndustryEffect(override val handler: overgrownNanoforgeJunkHandler): overgrownNanoforgeRandomizedEffect(
    handler
) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return overgrownNanoforgeEffectCategories.DEFICIT
    }

    override fun getName(): String = "Industrial"

    override fun getDescription(): String = "awdawdw"

    override fun applyBenefits() {
        return
    }

    override fun applyDeficits() {
        handler.industry = true
    }

    override fun unapplyBenefits() {
        return
    }

    override fun unapplyDeficits() {
        handler.industry = false
    }

    override val baseFormat: String
        get() = "Structure forced to be an industry"

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return ""
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive) return list
        return super.getAllFormattedEffects(positive)
    }
}
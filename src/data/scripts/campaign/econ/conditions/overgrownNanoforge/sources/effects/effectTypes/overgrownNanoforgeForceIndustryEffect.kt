package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories

class overgrownNanoforgeForceIndustryEffect(override val handler: overgrownNanoforgeJunkHandler): overgrownNanoforgeRandomizedEffect(
    handler
) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return overgrownNanoforgeEffectCategories.DEFICIT
    }

    override fun getName(): String = "Industrial"

    override fun getDescription(): String = "Structure forced to be an industry"
    override fun applyEffects() {
        handler.industry = true
    }

    override fun unapplyEffects() {
        handler.industry = false
    }
}
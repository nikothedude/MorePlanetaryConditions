package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeAlterStabilityEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeRandomizedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (stabilityIsNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun stabilityIsNegative(): Boolean {
        return (increment < 0)
    }

    override fun getName(): String {
        if (stabilityIsNegative()) return "Ungovernable" else return "Establishment Stronghold"
    }

    override fun getDescription(): String {
        return "wdabuhiwdawu"
    }

    override fun applyBenefits() {
        if (stabilityIsNegative()) return
        getMarket().stability.modifyFlat(getId(), increment)
    }

    override fun applyDeficits() {
        if (!stabilityIsNegative()) return
        getMarket().stability.modifyFlat(getId(), increment)
    }

    override fun unapplyBenefits() {
        if (stabilityIsNegative()) return
        getMarket().stability.unmodifyFlat(getId())
    }

    override fun unapplyDeficits() {
        if (!stabilityIsNegative()) return
        getMarket().stability.unmodifyFlat(getId())
    }

}

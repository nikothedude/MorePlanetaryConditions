package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeAlterStabilityEffect(
    nanoforge: overgrownNanoforgeIndustry,
    val increment: Float
): overgrownNanoforgeRandomizedEffect(nanoforge) {
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

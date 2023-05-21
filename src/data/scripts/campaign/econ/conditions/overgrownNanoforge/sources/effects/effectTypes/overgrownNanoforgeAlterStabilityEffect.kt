package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import kotlin.math.abs

class overgrownNanoforgeAlterStabilityEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeRandomizedEffect(handler) {

    override val baseFormat: String = "Market stability $adjectiveChar by $changeChar"
    override fun getChange(positive: Boolean, vararg args: Any): String {
        if (positive && stabilityIsNegative()) return ""
        return "${abs(increment)}"
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive && stabilityIsNegative()) return list
        if (!positive && !stabilityIsNegative()) return list
        return super.getAllFormattedEffects(positive)
    }

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
        getMarket().stability.modifyFlat(getId(), increment, getNameForModifier())
    }

    override fun applyDeficits() {
        if (!stabilityIsNegative()) return
        getMarket().stability.modifyFlat(getId(), increment, getNameForModifier())
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

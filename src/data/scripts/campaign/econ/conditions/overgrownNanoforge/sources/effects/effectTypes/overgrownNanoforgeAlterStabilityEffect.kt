package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import kotlin.math.abs

class overgrownNanoforgeAlterStabilityEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeFormattedEffect(handler) {

    override fun getBaseFormat(): String {
        return "Market stability $adjectiveChar by $changeChar"
    }
    override fun getChange(positive: Boolean): String {
        return "${abs(increment)}"
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

    override fun applyEffects() {
        getMarket().stability.modifyFlat(getOurId(), increment, getNameForModifier())
    }

    override fun unapplyEffects() {
        getMarket().stability.unmodifyFlat(getOurId())
    }

}

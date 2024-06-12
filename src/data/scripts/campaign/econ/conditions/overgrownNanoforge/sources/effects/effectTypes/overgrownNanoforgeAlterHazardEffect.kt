package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import kotlin.math.abs

class overgrownNanoforgeAlterHazardEffect(
    handler: overgrownNanoforgeHandler,
    val increment: Float
): overgrownNanoforgeFormattedEffect(handler) {

    override val negativeAdjective: String
        get() = "increased"
    override val positiveAdjective: String
        get() = "decreased"

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (!hazardIsNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun hazardIsNegative(): Boolean {
        return (increment < 0)
    }

    override fun getName(): String {
        if (!hazardIsNegative()) return "Hazardous" else return "Safe"
    }

    override fun applyEffects() {
        getMarket().hazard.modifyFlat(getOurId(), increment, getNameForModifier())
    }
    override fun unapplyEffects() {
        getMarket().hazard.unmodifyFlat(getOurId())
    }

    override fun getBaseFormat(): String {
        return "Market hazard $adjectiveChar by $changeChar%"
    }

    override fun getChange(positive: Boolean): String {
        return "${abs(increment * 100)}"
    }
}

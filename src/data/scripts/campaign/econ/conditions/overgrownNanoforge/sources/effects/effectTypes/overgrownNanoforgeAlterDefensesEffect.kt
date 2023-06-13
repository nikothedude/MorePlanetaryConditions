package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import kotlin.math.abs

class overgrownNanoforgeAlterDefensesEffect(
    handler: overgrownNanoforgeHandler,
    val mult: Float

): overgrownNanoforgeFormattedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (defenseIsNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun defenseIsNegative(): Boolean {
        return mult < 1f
    }

    override fun getName(): String {
        if (defenseIsNegative()) return "Indefensible" else return "Exceptionally defensible"
    }

    override fun applyEffects() {
        if (defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(getOurId(), mult, getNameForModifier())
    }

    override fun unapplyEffects() {
        if (defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(getOurId())
    }

    override fun getBaseFormat(): String {
        return "Market defense rating $adjectiveChar by ${changeChar}x"
    }

    override fun getChange(positive: Boolean): String {
        return "${abs(mult)}"
    }

}

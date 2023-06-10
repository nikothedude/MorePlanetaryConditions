package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import kotlin.math.abs

class overgrownNanoforgeAlterDefensesEffect(
    handler: overgrownNanoforgeHandler,
    val mult: Float

): overgrownNanoforgeRandomizedEffect(handler) {
    override fun getCategory(): overgrownNanoforgeEffectCategories {
        if (defenseIsNegative()) return overgrownNanoforgeEffectCategories.DEFICIT else return overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun defenseIsNegative(): Boolean {
        return mult < 1f
    }

    override fun getName(): String {
        if (defenseIsNegative()) return "Indefensible" else return "Exceptionally defensible"
    }

    override fun getDescription(): String {
        return "placeholder"
    }

    override fun applyBenefits() {
        if (defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(getOurId(), mult, getNameForModifier())
    }

    override fun applyDeficits() {
        if (!defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(getOurId(), mult, getNameForModifier())
    }

    override fun unapplyBenefits() {
        if (defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(getOurId())
    }

    override fun unapplyDeficits() {
        if (!defenseIsNegative()) return
        getMarket().stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(getOurId())
    }

    override val baseFormat: String = "Market defense rating $adjectiveChar by ${changeChar}x"

    override fun getChange(positive: Boolean, vararg args: Any): String {
        return "${abs(mult)}"
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive && defenseIsNegative()) return list
        if (!positive && !defenseIsNegative()) return list
        return super.getAllFormattedEffects(positive)
    }

}

package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import kotlin.math.abs

class overgrownNanoforgeAlterUpkeepEffect(
    handler: overgrownNanoforgeHandler,
    val mult: Float
): overgrownNanoforgeFormattedEffect(handler) {

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isUpkeepNegative()) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isUpkeepNegative(): Boolean {
        return (mult < 0f)
    }

    override fun getName(): String = if (isUpkeepNegative()) "Expensive maintenance" else "Cheap maintenance"

    override fun getBaseFormat(): String {
        return "Industry upkeep $adjectiveChar by ${changeChar}x"
    }

    override fun getChange(positive: Boolean): String {
        return "$mult"
    }

    override fun applyEffects() {
        val structure = getStructure() ?: return
        structure.upkeep.modifyMult(getOurId(), mult, getNameForModifier())
    }

    override fun unapplyEffects() {
        val structure = getStructure() ?: return
        structure.upkeep.unmodifyMult(getOurId())
    }

    override fun delete() {
        unapply()
    }
}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import kotlin.math.abs

class overgrownNanoforgeAlterUpkeepEffect(
    handler: overgrownNanoforgeHandler,
    val mult: Float
): overgrownNanoforgeRandomizedEffect(handler) {

    override val baseFormat: String = "Nanoforge upkeep $adjectiveChar by $changeChar"
    override fun getChange(positive: Boolean, vararg args: Any): String {
        return "${abs(mult * 100)}"
    }

    override fun getCategory(): overgrownNanoforgeEffectCategories {
        return if (isUpkeepNegative()) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT
    }

    private fun isUpkeepNegative(): Boolean {
        return (mult < 0f)
    }

    override fun getName(): String = if (isUpkeepNegative()) "Expensive maintenance" else "Cheap maintenance"

    override fun getDescription(): String {
        if (isUpkeepNegative()) {
            return "This area of the overgrown nanoforge is irritatingly expensive to maintain, and as such raises maintenance costs of the primary industry by ${mult}x."
        } else {
            return "This area of the overgrown nanoforge is pleasantly cheap to maintain, and as such lowers maintenance costs of the primary industry by ${mult}x."
        }
    }

    override fun applyBenefits() {
        val structure = getStructure() ?: return
        if (isUpkeepNegative()) return
        structure.upkeep.modifyMult(getId(), mult, getNameForModifier())
    }

    override fun applyDeficits() {
        val structure = getStructure() ?: return
        if (!isUpkeepNegative()) return
        structure.upkeep.modifyMult(getId(), mult, getNameForModifier())
    }

    override fun unapplyBenefits() {
        val structure = getStructure() ?: return
        if (isUpkeepNegative()) return
        structure.upkeep.unmodifyMult(getId())
    }

    override fun unapplyDeficits() {
        val structure = getStructure() ?: return
        if (!isUpkeepNegative()) return
        structure.upkeep.unmodifyMult(getId())
    }

    override fun delete() {
        unapply()
    }

    override fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        val list = ArrayList<String>()
        if (positive && isUpkeepNegative()) return list
        if (!positive && !isUpkeepNegative()) return list
        return super.getAllFormattedEffects(positive)
    }
}
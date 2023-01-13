package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeAlterUpkeepEffect(
    nanoforge: overgrownNanoforgeIndustry,
    val mult: Float
): overgrownNanoforgeRandomizedEffect(nanoforge) {
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
        if (isUpkeepNegative()) return
        getIndustry().getUpkeepMult().modifyMult(getId(), mult)
    }

    override fun applyDeficits() {
        if (!isUpkeepNegative()) return
        getIndustry().getUpkeepMult().modifyMult(getId(), mult)
    }

    override fun unapplyBenefits() {
        if (isUpkeepNegative()) return
        getIndustry().getUpkeepMult().unmodifyMult(getId())
    }

    override fun unapplyDeficits() {
        if (!isUpkeepNegative()) return
        getIndustry().getUpkeepMult().unmodifyMult(getId())
    }

    override fun delete() {
        unapply()
    }
}
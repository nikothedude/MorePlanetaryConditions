package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getPotentialPrototypes

class overgrownNanoforgeRandomizedSourceParams(
    nanoforge: overgrownNanoforgeIndustry,
    val type: overgrownNanoforgeSourceTypes,
): overgrownNanoforgeSourceParams() {
    val effects: MutableSet<overgrownNanoforgeEffect> = HashSet()
    var budget = getBudget(nanoforge)

    init {
        generateRandomizedEffects(nanoforge)
    }

    private fun generateRandomizedEffects(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        var effects = HashSet<overgrownNanoforgeEffect>()
        val market = nanoforge.market
        var availableBudget = budget

        var maxToPick: Float = getMaxEffectsToPick()

        val potentialPrototypes = getPotentialPrototypes(this, availableBudget)
        for (prototype in potentialPrototypes) {

        }
    }

    private fun getMaxEffectsToPick(): Float {
        TODO("Not yet implemented")
    }

    private fun getBudget(nanoforge: overgrownNanoforgeIndustry): Float {
        return randomizedSourceBudgetsPicker.getRandomBudget(nanoforge)
    }

}

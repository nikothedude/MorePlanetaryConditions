package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getPotentialPrototypes
import niko.MCTE.utils.MCTE_debugUtils.displayError

class overgrownNanoforgeRandomizedSourceParams(
    val nanoforge: overgrownNanoforgeIndustry,
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

        while (maxToPick-- > 0) {
            val pickedPrototype = getPotentialPrototypes(this, availableBudget).randomOrNull() ?: break
            val prototypeInstance = pickedPrototype.getInstance(this, availableBudget)
        }

    private fun getMaxEffectsToPick(): Float {
        TODO("Not yet implemented")
    }

    private fun getBudget(nanoforge: overgrownNanoforgeIndustry): Float {
        return randomizedSourceBudgetsPicker.getRandomBudget(nanoforge)
    }

}

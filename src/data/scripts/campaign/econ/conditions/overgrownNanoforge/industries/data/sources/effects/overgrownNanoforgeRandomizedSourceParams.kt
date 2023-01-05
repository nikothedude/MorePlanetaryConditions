package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getPotentialPrototypes
import data.utilities.niko_MPC_mathUtils.randomlyDistributeNumberAcrossEntries

class overgrownNanoforgeRandomizedSourceParams(
    val nanoforge: overgrownNanoforgeIndustry,
    val type: overgrownNanoforgeSourceTypes,
): overgrownNanoforgeSourceParams() {
    val effects: MutableSet<overgrownNanoforgeEffect> = generateRandomizedEffects(nanoforge)
    var ourBudgetHolder = budgetHolder(getInitialBudget(nanoforge))

    class budgetHolder(var budget: Float)

    private fun generateRandomizedEffects(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        val effects = HashSet<overgrownNanoforgeEffect>()

        val potentialPrototypes: MutableSet<overgrownNanoforgeEffectPrototypes> = HashSet()
        var maxToPick: Float = getMaxEffectsToPick()

        while (maxToPick-- > 0) {
            val pickedPrototype = getPotentialPrototypes(this).randomOrNull() ?: break
            potentialPrototypes += pickedPrototype
        }
        if (potentialPrototypes.isEmpty()) return HashSet()
        val weightedPrototypes = randomlyDistributeNumberAcrossEntries(
            potentialPrototypes,
            getBudget(),
            { budget: Float, remainingRuns: Int, entry: overgrownNanoforgeEffectPrototypes, -> entry.getMinimumCost(this) ?: 0f},
        )
        for (entry in weightedPrototypes) ourBudgetHolder.budget -= entry.value
        for (entry in weightedPrototypes) {
            val prototype = entry.key
            val score = entry.value
            val instance = prototype.getParamsForInstance(this, score) ?: continue
            effects += instance
        }
        return effects
    }

    private fun getMaxEffectsToPick(): Float {
        TODO("Not yet implemented")
    }

    private fun getInitialBudget(nanoforge: overgrownNanoforgeIndustry): Float {
        return randomizedSourceBudgetsPicker.getRandomBudget(nanoforge)
    }
    fun getBudget(): Float {
        return ourBudgetHolder.budget
    }

    fun getMarket(): MarketAPI {
        return nanoforge.market
    }
}

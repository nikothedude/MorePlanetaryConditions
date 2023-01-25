package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

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
    var positiveBudgetHolder: budgetHolder
    var negativeBudgetHolder: budgetHolder
    var specialBudgetHolder: budgetHolder

    init {
        val budget = getInitialBudget(nanoforge)
        positiveBudgetHolder = budgetHolder(budget)
        negativeBudgetHolder = budgetHolder(-budget)
        specialBudgetHolder = budgetHolder(getSpecialBudget())
    }

    private fun getSpecialBudget(): Float {
        return 0f
    }

    class budgetHolder(var budget: Float)

    private fun generateRandomizedEffects(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        val effects = HashSet<overgrownNanoforgeEffect>()

        effects += pickPositives(nanoforge)
        effects += pickNegatives(nanoforge)
        effects += pickSpecial(nanoforge)

        return effects
    }

    private fun pickPositives(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(nanoforge, positiveBudgetHolder, setOf(overgrownNanoforgeEffectCategories.BENEFIT))
    }

    private fun pickNegatives(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(nanoforge, negativeBudgetHolder, setOf(overgrownNanoforgeEffectCategories.DEFICIT))
    }

    private fun pickSpecial(nanoforge: overgrownNanoforgeIndustry): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(nanoforge, specialBudgetHolder, setOf(overgrownNanoforgeEffectCategories.SPECIAL))
    }

    private fun pickEffects(
        nanoforge: overgrownNanoforgeIndustry,
        holder: budgetHolder,
        allowedCategories: Set<overgrownNanoforgeEffectCategories>,
        maxToPick: Float = getMaxEffectsToPick()
    ): MutableSet<overgrownNanoforgeEffect> {
        var maxToPick = maxToPick
        val initialBudget = holder.budget

        val effects = HashSet<overgrownNanoforgeEffect>()

        val potentialPrototypes: MutableSet<overgrownNanoforgeEffectPrototypes> = HashSet()

        while (maxToPick-- > 0) {
            val pickedPrototype = getPotentialPrototypes(this, holder, allowedCategories).randomOrNull() ?: break
            potentialPrototypes += pickedPrototype
        }
        if (potentialPrototypes.isEmpty()) return HashSet()
        val weightedPrototypes = randomlyDistributeNumberAcrossEntries(
            potentialPrototypes,
            initialBudget,
            { budget: Float, remainingRuns: Int, entry: overgrownNanoforgeEffectPrototypes, -> entry.getMinimumCost(nanoforge) ?: 0f},
        )
        for (entry in weightedPrototypes) positiveBudgetHolder.budget -= entry.value
        for (entry in weightedPrototypes) {
            val prototype = entry.key
            val score = entry.value
            val instance = prototype.getInstance(nanoforge, score) ?: continue //TODO: this fucking sucks
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

    fun getMarket(): MarketAPI {
        return nanoforge.market
    }
}

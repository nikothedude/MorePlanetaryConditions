package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getPotentialPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getWeightedPotentialPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_mathUtils.randomlyDistributeNumberAcrossEntries
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_NEGATIVE_EFFECT_BUDGET_MULT
import kotlin.math.abs

class overgrownNanoforgeRandomizedSourceParams(
    val handler: overgrownNanoforgeJunkHandler,
    val type: overgrownNanoforgeSourceTypes,
): overgrownNanoforgeSourceParams() {
    var positiveBudgetHolder: budgetHolder
    var negativeBudgetHolder: budgetHolder
    var specialBudgetHolder: budgetHolder
    val budget = getInitialBudget(handler)
    init {
        positiveBudgetHolder = budgetHolder(budget)
        negativeBudgetHolder = budgetHolder((-budget*OVERGROWN_NANOFORGE_NEGATIVE_EFFECT_BUDGET_MULT))
        specialBudgetHolder = budgetHolder(getSpecialBudget())
    }

    val effects: MutableSet<overgrownNanoforgeEffect> = generateRandomizedEffects(handler)

    private fun getSpecialBudget(): Float {
        return 0f
    }

    class budgetHolder(var budget: Float)

    private fun generateRandomizedEffects(handler: overgrownNanoforgeHandler): MutableSet<overgrownNanoforgeEffect> {
        val effects = HashSet<overgrownNanoforgeEffect>()

        effects += pickPositives(handler)
        effects += pickNegatives(handler)
        effects += pickSpecial(handler)

        return effects
    }

    private fun pickPositives(handler: overgrownNanoforgeHandler): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(handler, positiveBudgetHolder, setOf(overgrownNanoforgeEffectCategories.BENEFIT))
    }

    private fun pickNegatives(handler: overgrownNanoforgeHandler): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(handler, negativeBudgetHolder, setOf(overgrownNanoforgeEffectCategories.DEFICIT))
    }

    private fun pickSpecial(handler: overgrownNanoforgeHandler): MutableSet<overgrownNanoforgeEffect> {
        return pickEffects(handler, specialBudgetHolder, setOf(overgrownNanoforgeEffectCategories.SPECIAL))
    }

    private fun pickEffects(
        handler: overgrownNanoforgeHandler,
        holder: budgetHolder,
        allowedCategories: Set<overgrownNanoforgeEffectCategories>,
        maxToPick: Float = getMaxEffectsToPick()
    ): MutableSet<overgrownNanoforgeEffect> {
        var maxToPick = maxToPick
        val initialBudget = holder.budget

        val effects = HashSet<overgrownNanoforgeEffect>()

        val potentialPrototypes: MutableSet<overgrownNanoforgeEffectPrototypes> = HashSet()

        val initialPrototypes = getWeightedPotentialPrototypes(this, holder, allowedCategories, handler)
        val picker = WeightedRandomPicker<overgrownNanoforgeEffectPrototypes>()
        for (entry in initialPrototypes) {
            picker.add(entry.key, entry.value)
        }
        while (maxToPick-- > 0) {
            val pickedPrototype = picker.pick() ?: break
            //TODO: add support for unique things
            potentialPrototypes += pickedPrototype
            //initialPrototypes -= pickedPrototype
        }
        if (potentialPrototypes.isEmpty()) return HashSet()
        val negative = initialBudget < 0
        val getMax = { budget: Float, remainingRuns: Int, entry: overgrownNanoforgeEffectPrototypes, ->
            (entry.getMaximumCost(handler))?.coerceAtMost(budget) ?: budget}
        val weightedPrototypes = randomlyDistributeNumberAcrossEntries(
            potentialPrototypes,
            abs(initialBudget),
            { budget: Float, remainingRuns: Int, entry: overgrownNanoforgeEffectPrototypes, -> entry.getMinimumCost(handler) ?: 0f},
            getMax,
        )
        if (negative) {
            for (entry in weightedPrototypes) {
                weightedPrototypes[entry.key] = entry.value * -1f
            }
        }
        for (entry in weightedPrototypes) holder.budget -= entry.value
        for (entry in weightedPrototypes) {
            val prototype = entry.key
            val score = entry.value
            val instance = prototype.getInstance(handler, score) ?: continue
            effects += instance
        }
        return effects
    }

    private fun getMaxEffectsToPick(): Float {
        return 1f
        //TODO("Not yet implemented")
    }

    private fun getInitialBudget(handler: overgrownNanoforgeHandler): Float {
        return randomizedSourceBudgetsPicker.getRandomBudget(handler)
    }

    fun getMarket(): MarketAPI {
        return handler.market
    }

    fun createJunk(): overgrownNanoforgeJunkHandler? {
        val market = getMarket()
        val source = createSource()
        val newHandler =
            market.getOvergrownNanoforgeIndustryHandler()?.let { overgrownNanoforgeJunkHandler(market, it, market.getNextOvergrownJunkDesignation()) }
        newHandler?.init(source)

        return newHandler
    }

    fun createSource(): overgrownNanoforgeRandomizedSource {
        return overgrownNanoforgeRandomizedSource(handler, this)
    }
}

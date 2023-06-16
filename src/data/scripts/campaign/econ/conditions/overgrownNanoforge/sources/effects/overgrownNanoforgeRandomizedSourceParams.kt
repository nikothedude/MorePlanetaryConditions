package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.generateEffects
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getRandomPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
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
        maxToPick: Int = getMaxEffectsToPick()
    ): MutableSet<overgrownNanoforgeEffect> {
        val initialBudget = holder.budget

        val negative = initialBudget < 0
        val category = if (negative) overgrownNanoforgeEffectCategories.DEFICIT else overgrownNanoforgeEffectCategories.BENEFIT

        val effects = generateEffects(handler, getRandomPrototypes(
            handler, 
            holder,
            setOf(category),
            maxToPick
        ))

        return effects
    }

    private fun getMaxEffectsToPick(): Int {
        return 1
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

class budgetHolder(var budget: Float)


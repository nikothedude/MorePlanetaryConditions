package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getPotentialPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkDesignation
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_mathUtils.randomlyDistributeNumberAcrossEntries

class overgrownNanoforgeRandomizedSourceParams(
    val handler: overgrownNanoforgeIndustryHandler,
    val type: overgrownNanoforgeSourceTypes,
): overgrownNanoforgeSourceParams() {
    var positiveBudgetHolder: budgetHolder
    var negativeBudgetHolder: budgetHolder
    var specialBudgetHolder: budgetHolder
    init {
        val budget = getInitialBudget(handler)
        positiveBudgetHolder = budgetHolder(budget)
        negativeBudgetHolder = budgetHolder(-budget)
        specialBudgetHolder = budgetHolder(getSpecialBudget())
    }

    val effects: MutableSet<overgrownNanoforgeEffect> = generateRandomizedEffects(handler)

    private fun getSpecialBudget(): Float {
        return 0f
    }

    var cullingResistance = getInitialCullingResistance()

    fun getInitialCullingResistance(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
        )
    }

    var cullingResistanceRegeneration = getInitialCullingResistanceRegen()

    fun createBaseCullingResistanceRegeneration(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
        )
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

        val initialPrototypes = getPotentialPrototypes(this, holder, allowedCategories)
        while (maxToPick-- > 0) {
            val pickedPrototype = getPotentialPrototypes(this, holder, allowedCategories).randomOrNull() ?: break
            potentialPrototypes += pickedPrototype
            initialPrototypes -= pickedPrototype
        }
        if (potentialPrototypes.isEmpty()) return HashSet()
        val weightedPrototypes = randomlyDistributeNumberAcrossEntries(
            potentialPrototypes,
            initialBudget,
            { budget: Float, remainingRuns: Int, entry: overgrownNanoforgeEffectPrototypes, -> entry.getMinimumCost(handler.getCoreHandler()) ?: 0f},
        )
        for (entry in weightedPrototypes) positiveBudgetHolder.budget -= entry.value
        for (entry in weightedPrototypes) {
            val prototype = entry.key
            val score = entry.value
            val instance = prototype.getInstance(handler.market.getOvergrownNanoforgeIndustryHandler()!!, score) ?: continue //TODO: this fucking sucks
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

    fun createJunk(resistance: Int = cullingResistance, resistanceRegen: Int = cullingResistanceRegeneration): overgrownNanoforgeJunkHandler? {
        val market = getMarket()
        val source = createSource()
        val newHandler =
            market.getOvergrownNanoforgeIndustryHandler()?.let { overgrownNanoforgeJunkHandler(market, it, market.getNextOvergrownJunkDesignation()) }
        newHandler?.init(source, resistanec, resistanceRegen)

        return newHandler
    }

    fun createSource(): overgrownNanoforgeRandomizedSource {
        return overgrownNanoforgeRandomizedSource(handler, this)
    }
}

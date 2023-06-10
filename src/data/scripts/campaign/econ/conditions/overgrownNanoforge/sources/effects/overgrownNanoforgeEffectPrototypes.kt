package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.*
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.getProducableCommodities
import data.utilities.niko_MPC_marketUtils.removeNonNanoforgeProducableCommodities
import data.utilities.niko_MPC_marketUtils.getProducableCommoditiesForOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.hasNonJunkStructures
import data.utilities.niko_MPC_mathUtils.ensureIsJsonValidFloat
import data.utilities.niko_MPC_mathUtils.randomlyDistributeBudgetAcrossCommodities
import data.utilities.niko_MPC_settings.ANCHOR_POINT_FOR_DEFENSE
import data.utilities.niko_MPC_settings.HARD_LIMIT_FOR_DEFENSE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
import org.lazywizard.lazylib.MathUtils
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.absoluteValue

enum class overgrownNanoforgeEffectPrototypes(
    val possibleCategories: Set<overgrownNanoforgeEffectCategories>,
    val canInvert: Boolean = possibleCategories.contains(overgrownNanoforgeEffectCategories.DEFICIT)
) {

    //TODO: Replace params with a special class that has its own budget
    // This is important because as it stands the caller hsa no way of knowing how much budget was actually used up

    ALTER_SUPPLY(setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)) {
        override fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(nanoforge, maxBudget)
            val market = nanoforge.market
            return (superValue && market.getProducableCommoditiesForOvergrownNanoforge().isNotEmpty())
        }
        override fun getWeight(growth: overgrownNanoforgeHandler): Float = 45f
        override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float? {
            val market = growth.market
            val producableCommodities = market.getProducableCommodities()
            removeNonNanoforgeProducableCommodities(producableCommodities)
            if (producableCommodities.isEmpty()) return null
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities) {
                val cost = getCostForCommodity(growth, commodityId) ?: continue
                if (lowestCost > cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(nanoforge: overgrownNanoforgeHandler, commodityId: String): Float? {
            return overgrownNanoforgeCommodityDataStore[commodityId]?.cost
        }
        override fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canAfford(growth, maxBudget)) return null
            val shouldInvert = maxBudget < 0
            val maxBudget = maxBudget.absoluteValue
            val market = growth.market
            val producableCommodities = market.getProducableCommoditiesForOvergrownNanoforge()

            val picker = WeightedRandomPicker<String>()
            val iterator = producableCommodities.iterator()
            while (iterator.hasNext()) {
                val commodityId: String = iterator.next()
                val cost = overgrownNanoforgeCommodityDataStore[commodityId]!!.cost
                if (cost > maxBudget) {
                    iterator.remove()
                    continue
                }
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId,
                    growth, shouldInvert)
                if (weight != 0f) picker.add(commodityId, weight)
            }

            var timesToPick = getTimesToPickCommodities(growth, maxBudget, picker)
            val pickedCommodities = HashSet<String>()
            while (timesToPick-- > 0 && !picker.isEmpty) pickedCommodities += picker.pickAndRemove()
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(pickedCommodities, maxBudget) //assign quantities to the things
            if (shouldInvert) {
                for (entry in themeToScore.entries) {
                    val commodityId = entry.key
                    themeToScore[commodityId] = -themeToScore[commodityId]!!.absoluteValue
                }
            }
            val effect = overgrownNanoforgeAlterSupplySource(growth, themeToScore)
            return effect
        }

        private fun getTimesToPickCommodities(nanoforge: overgrownNanoforgeHandler, availableBudget: Float, picker: WeightedRandomPicker<String>): Int {
            var times: Int = OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
            val threshold = 0.7f
            val randomFloat = MathUtils.getRandom().nextFloat()
            if (randomFloat > threshold) times++
            return times.coerceAtMost(picker.items.size)
        }
    },
        /*ALTER_UPKEEP(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 0f //TODO: disabled
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? = getCostPerPointOneIncrement(nanoforge) //TODO: this is bad
            fun getCostPerPointOneIncrement(nanoforge: overgrownNanoforgeIndustryHandler): Float = 25f
            
            override fun getInstance(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): overgrownNanoforgeAlterUpkeepEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var maxBudget = maxBudget.absoluteValue
                var mult = 0f
                while (maxBudget > 0) {
                    maxBudget -= getCostPerPointOneIncrement(nanoforge)
                    mult += 0.1f
                }
                if (shouldInvert) mult = -mult.absoluteValue
                return overgrownNanoforgeAlterUpkeepEffect(nanoforge, mult)
            }

        },*/
        ALTER_ACCESSIBILITY(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(growth: overgrownNanoforgeHandler): Float = 10f
            override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float? = getCostPerOnePercentAccessability(growth)
            fun getCostPerOnePercentAccessability(nanoforge: overgrownNanoforgeHandler): Float = 2f
            
            override fun getInstance(
                growth: overgrownNanoforgeHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterAccessibilityEffect? {
                if (!canAfford(growth, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var remainingBudget = maxBudget.absoluteValue
                var increment = 0f
                while (canAfford(growth, remainingBudget)) {
                    increment += 0.01f
                    val cost = getCostPerOnePercentAccessability(growth)
                    remainingBudget -= cost
                }
                if (increment == 0f) return null
                if (shouldInvert) increment = -increment.absoluteValue
                return overgrownNanoforgeAlterAccessibilityEffect(growth, increment)
            }

        },
        ALTER_DEFENSES(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(growth: overgrownNanoforgeHandler): Float {
                val weight = 10f
                val market = growth.market
                val groundDefense = market.stats.dynamic.getStat(Stats.GROUND_DEFENSES_MOD).modifiedValue
                val hardLimit: Float = HARD_LIMIT_FOR_DEFENSE //if we are at this or below, we will never ever be picked
                val divisor: Float = (ANCHOR_POINT_FOR_DEFENSE/(groundDefense-hardLimit)).coerceAtLeast(0f)
                var mult = ((1f/divisor).ensureIsJsonValidFloat()).toFloat()

                return weight*mult
            }
            override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float = getCostPerOnePointOneDefenseRating(
                growth
            )
            override fun getMaximumCost(growth: overgrownNanoforgeHandler): Float {
                return getMinimumCost(growth) * 1/getIncrementAmount()
            }
            fun getIncrementAmount(): Float = 0.01f
            fun getCostPerOnePointOneDefenseRating(nanoforge: overgrownNanoforgeHandler): Float = 5f

            override fun getInstance(
                growth: overgrownNanoforgeHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterDefensesEffect? {
                if (!canAfford(growth, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var remainingBudget = maxBudget.absoluteValue
                var mult = 1f
                val incrementAmount = getIncrementAmount()
                while (canAfford(growth, remainingBudget)) {
                    mult += incrementAmount
                    val cost = getCostPerOnePointOneDefenseRating(growth)
                    remainingBudget -= cost
                    if (mult <= 0.01f) break
                }
                if (mult == 1f) return null
                if (shouldInvert) mult = -mult.absoluteValue
                return overgrownNanoforgeAlterDefensesEffect(growth, mult)
            }
        },

        ALTER_STABILITY(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            fun getCostPerStability(nanoforge: overgrownNanoforgeHandler): Float = 50f

            override fun getWeight(growth: overgrownNanoforgeHandler): Float {
                val weight = 20f
                val market = growth.market
                val stability = market.stability.modifiedValue
                val divisor = (ANCHOR_POINT_FOR_STABILITY/stability).coerceAtLeast(0f)
                // TODO: can i store a variable directly on a enum? ex. store anchor point in val
                var mult = ((1/divisor).ensureIsJsonValidFloat()).toFloat()
                return weight*mult //always returns 0 if stability is 0
            }
            override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float = getCostPerStability(growth)

            override fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeAlterStabilityEffect? {
                if (!canAfford(growth, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var maxBudget = maxBudget.absoluteValue
                var instance: overgrownNanoforgeAlterStabilityEffect? = null
                var stabilityIncrement = getTimesToIncrement(growth, maxBudget)
                if (shouldInvert) stabilityIncrement = -stabilityIncrement.absoluteValue
                if (stabilityIncrement != 0f) instance = overgrownNanoforgeAlterStabilityEffect(growth, stabilityIncrement)

                return instance
            }
            fun getTimesToIncrement(nanoforge: overgrownNanoforgeHandler, availableBudget: Float): Float {
                var availableBudget = availableBudget
                var timesToIncrement = 0f
                while (canAfford(nanoforge, availableBudget)) {
                    val cost = getCostPerStability(nanoforge)
                    availableBudget -= cost
                    timesToIncrement++
                }
                return timesToIncrement
            }
        },
        ALTER_HAZARD(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(growth: overgrownNanoforgeHandler): Float = 15f

            override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float {
                return getCostPerOnePercent(growth)
            }

            fun getCostPerOnePercent(nanoforge: overgrownNanoforgeHandler): Float = 2f

            override fun getInstance(
                growth: overgrownNanoforgeHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterHazardEffect? {
                if (!canAfford(growth, maxBudget)) return null //worth noting: positive alterations should return a NEGATIVE value
                val shouldInvert = maxBudget < 0
                var maxBudget = maxBudget.absoluteValue
                var instance: overgrownNanoforgeAlterHazardEffect? = null
                var hazardIncrement = getTimesToIncrement(growth, maxBudget)
                if (shouldInvert) hazardIncrement = -(hazardIncrement)
                if (hazardIncrement != 0f) instance = overgrownNanoforgeAlterHazardEffect(growth, hazardIncrement)

                return instance
            }

            fun getTimesToIncrement(nanoforge: overgrownNanoforgeHandler, availableBudget: Float): Float {
                var availableBudget = availableBudget
                var timesToIncrement = 0f
                while (canAfford(nanoforge, availableBudget)) {
                    val cost = getCostPerOnePercent(nanoforge)
                    availableBudget -= cost
                    timesToIncrement -= 0.01f
                }
                return timesToIncrement
            }

        },
        
        // SPECIAL
        EXPLODE_UPON_DESTRUCTION(setOf(overgrownNanoforgeEffectCategories.SPECIAL)) {
            override fun getWeight(growth: overgrownNanoforgeHandler): Float = 5f
            override fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): Boolean {
                val superValue = super.canBeAppliedTo(nanoforge, maxBudget)
                val market = nanoforge.market
                return (superValue && market.hasNonJunkStructures())
            }
            override fun getMinimumCost(growth: overgrownNanoforgeHandler): Float? = getCost(growth)
            fun getCost(nanoforge: overgrownNanoforgeHandler): Float = 50f
            override fun getInstance(
                growth: overgrownNanoforgeHandler,
                maxBudget: Float
            ): overgrownNanoforgeVolatileEffect? {
                if (!canAfford(growth, maxBudget)) return null
                return overgrownNanoforgeVolatileEffect(growth)
            }
        };

        // FIXME: disabled, finish later
        /*SPAWN_HOSTILE_FLEETS(setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.SPECIAL)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 0.05f

            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? = getCost(nanoforge)

            fun getCost(nanoforge: overgrownNanoforgeIndustryHandler) = 100f

            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustryHandler,
                maxBudget: Float
            ): overgrownNanoforgeSpawnFleetEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                return overgrownNanoforgeSpawnFleetEffect(nanoforge)
            }
        }; */



    open fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): Boolean = canAfford(nanoforge, maxBudget)
    fun canAfford(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {
        val maxBudget = if (canInvert) maxBudget.absoluteValue else maxBudget
        val minimumCost = getMinimumCost(growth) ?: return false
        return (minimumCost <= maxBudget)
    }
    abstract fun getWeight(growth: overgrownNanoforgeHandler): Float
    abstract fun getMinimumCost(growth: overgrownNanoforgeHandler): Float?
    open fun getMaximumCost(growth: overgrownNanoforgeHandler): Float? = Float.MAX_VALUE
    abstract fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeEffect?

    companion object {
        val ANCHOR_POINT_FOR_STABILITY: Int = 5
        val allPrototypes = overgrownNanoforgeEffectPrototypes.values().toSet()

        val prototypesByCategory: MutableMap<overgrownNanoforgeEffectCategories, MutableSet<overgrownNanoforgeEffectPrototypes>> =
            EnumMap(overgrownNanoforgeEffectCategories::class.java)
        init {
            for (category in overgrownNanoforgeEffectCategories.values()) prototypesByCategory[category] = HashSet()

            for (entry in overgrownNanoforgeEffectPrototypes.values()) {
                for (category in entry.possibleCategories) prototypesByCategory[category]!! += entry
            }
        }
        fun getPotentialPrototypes(
            params: overgrownNanoforgeRandomizedSourceParams,
            holder: overgrownNanoforgeRandomizedSourceParams.budgetHolder,
            allowedCategories: Set<overgrownNanoforgeEffectCategories> = setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)): MutableSet<overgrownNanoforgeEffectPrototypes>
        {
            val potentialPrototypes = HashSet<overgrownNanoforgeEffectPrototypes>()
            for (prototype in ArrayList(allPrototypes)) {
                if (!prototype.possibleCategories.any { allowedCategories.contains(it) }) continue
                if (prototype.canBeAppliedTo(params.handler.market.getOvergrownNanoforgeIndustryHandler()!!, holder.budget)) potentialPrototypes += prototype
            }
            return potentialPrototypes
        }

        fun getWeightedPotentialPrototypes(
            params: overgrownNanoforgeRandomizedSourceParams,
            holder: overgrownNanoforgeRandomizedSourceParams.budgetHolder,
            allowedCategories: Set<overgrownNanoforgeEffectCategories> = setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT),
            nanoforge: overgrownNanoforgeHandler): MutableMap<overgrownNanoforgeEffectPrototypes, Float>
        {
            val potentialPrototypes = getPotentialPrototypes(params, holder, allowedCategories)

            val weightedPrototypes = HashMap<overgrownNanoforgeEffectPrototypes, Float>()
            for (prototype in potentialPrototypes) {
                val weight = prototype.getWeight(nanoforge)
                weightedPrototypes[prototype] = weight
            }
            return weightedPrototypes
        }

    }
}

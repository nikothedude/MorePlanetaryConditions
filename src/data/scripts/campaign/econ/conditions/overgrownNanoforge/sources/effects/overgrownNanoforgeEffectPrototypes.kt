package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.util.WeightedRandomPicker
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
        override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 50f
        override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? {
            val market = nanoforge.market
            val producableCommodities = market.getProducableCommodities()
            removeNonNanoforgeProducableCommodities(producableCommodities)
            if (producableCommodities.isEmpty()) return null
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities) {
                val cost = getCostForCommodity(nanoforge, commodityId) ?: continue
                if (lowestCost > cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(nanoforge: overgrownNanoforgeIndustryHandler, commodityId: String): Float? {
            return overgrownNanoforgeCommodityDataStore[commodityId]?.cost
        }
        override fun getInstance(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canAfford(nanoforge, maxBudget)) return null
            val shouldInvert = maxBudget < 0
            val maxBudget = maxBudget.absoluteValue
            val market = nanoforge.market
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
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId, nanoforge, shouldInvert)
                if (weight != 0f) picker.add(commodityId, weight)
            }

            var timesToPick = getTimesToPickCommodities(nanoforge, maxBudget, picker)
            val pickedCommodities = HashSet<String>()
            while (timesToPick-- > 0 && !picker.isEmpty) pickedCommodities += picker.pickAndRemove()
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(pickedCommodities, maxBudget) //assign quantities to the things
            if (shouldInvert) {
                for (entry in themeToScore.entries) {
                    val commodityId = entry.key
                    themeToScore[commodityId] = -themeToScore[commodityId]!!.absoluteValue
                }
            }
            val effect = overgrownNanoforgeAlterSupplySource(nanoforge, themeToScore)
            return effect
        }

        private fun getTimesToPickCommodities(nanoforge: overgrownNanoforgeIndustryHandler, availableBudget: Float, picker: WeightedRandomPicker<String>): Int {
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
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 10f
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? = getCostPerOnePercentAccessability(nanoforge)
            fun getCostPerOnePercentAccessability(nanoforge: overgrownNanoforgeIndustryHandler): Float = 5f
            
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustryHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterAccessibilityEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var remainingBudget = maxBudget.absoluteValue
                var increment = 0f
                while (canAfford(nanoforge, remainingBudget)) {
                    increment += 0.01f
                    val cost = getCostPerOnePercentAccessability(nanoforge)
                    remainingBudget -= cost
                }
                if (increment == 0f) return null
                if (shouldInvert) increment = -increment.absoluteValue
                return overgrownNanoforgeAlterAccessibilityEffect(nanoforge, increment)
            }

        },
        ALTER_DEFENSES(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float {
                val weight = 10f
                val market = nanoforge.market
                val groundDefense = market.stats.dynamic.getStat(Stats.GROUND_DEFENSES_MOD).modifiedValue
                val hardLimit: Float = HARD_LIMIT_FOR_DEFENSE //if we are at this or below, we will never ever be picked
                val divisor: Float = (ANCHOR_POINT_FOR_DEFENSE/(groundDefense-hardLimit)).coerceAtLeast(0f)
                var mult = ((1f/divisor).ensureIsJsonValidFloat()).toFloat()

                return weight*mult
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float = getCostPerOneDefenseRating(nanoforge)
            fun getCostPerOneDefenseRating(nanoforge: overgrownNanoforgeIndustryHandler): Float = 0.5f
            
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustryHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterDefensesEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var remainingBudget = maxBudget.absoluteValue
                var increment = 0f
                while (canAfford(nanoforge, remainingBudget)) {
                    increment += 1f
                    val cost = getCostPerOneDefenseRating(nanoforge)
                    remainingBudget -= cost
                }
                if (increment == 0f) return null
                if (shouldInvert) increment = -increment.absoluteValue
                return overgrownNanoforgeAlterDefensesEffect(nanoforge, increment)
            }
        },

        ALTER_STABILITY(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            fun getCostPerStability(nanoforge: overgrownNanoforgeIndustryHandler): Float = 60f

            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float {
                val weight = 20f
                val market = nanoforge.market
                val stability = market.stability.modifiedValue
                val divisor = (ANCHOR_POINT_FOR_STABILITY/stability).coerceAtLeast(0f)
                // TODO: can i store a variable directly on a enum? ex. store anchor point in val
                var mult = ((1/divisor).ensureIsJsonValidFloat()).toFloat()
                return weight*mult //always returns 0 if stability is 0
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float = getCostPerStability(nanoforge)

            override fun getInstance(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): overgrownNanoforgeAlterStabilityEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                val shouldInvert = maxBudget < 0
                var maxBudget = maxBudget.absoluteValue
                var instance: overgrownNanoforgeAlterStabilityEffect? = null
                var stabilityIncrement = getTimesToIncrement(nanoforge, maxBudget)
                if (shouldInvert) stabilityIncrement = -stabilityIncrement.absoluteValue
                if (stabilityIncrement != 0f) instance = overgrownNanoforgeAlterStabilityEffect(nanoforge, stabilityIncrement)

                return instance
            }
            fun getTimesToIncrement(nanoforge: overgrownNanoforgeIndustryHandler, availableBudget: Float): Float {
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
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 0.5f

            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float {
                return getCostPerOnePercent(nanoforge)
            }

            fun getCostPerOnePercent(nanoforge: overgrownNanoforgeIndustryHandler): Float = 2f

            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustryHandler,
                maxBudget: Float
            ): overgrownNanoforgeAlterHazardEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null //worth noting: positive alterations should return a NEGATIVE value
                val shouldInvert = maxBudget < 0
                var maxBudget = maxBudget.absoluteValue
                var instance: overgrownNanoforgeAlterHazardEffect? = null
                var hazardIncrement = getTimesToIncrement(nanoforge, maxBudget)
                if (shouldInvert) hazardIncrement = -(hazardIncrement)
                if (hazardIncrement != 0f) instance = overgrownNanoforgeAlterHazardEffect(nanoforge, hazardIncrement)

                return instance
            }

            fun getTimesToIncrement(nanoforge: overgrownNanoforgeIndustryHandler, availableBudget: Float): Float {
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
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 5f
            override fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): Boolean {
                val superValue = super.canBeAppliedTo(nanoforge, maxBudget)
                val market = nanoforge.market
                return (superValue && market.hasNonJunkStructures())
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? = getCost(nanoforge)
            fun getCost(nanoforge: overgrownNanoforgeIndustryHandler): Float = 50f
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustryHandler,
                maxBudget: Float
            ): overgrownNanoforgeVolatileEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                return overgrownNanoforgeVolatileEffect(nanoforge)
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
    fun canAfford(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): Boolean {
        val maxBudget = if (canInvert) maxBudget.absoluteValue else maxBudget
        val minimumCost = getMinimumCost(nanoforge) ?: return false
        return (minimumCost <= maxBudget)
    }
    abstract fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float
    abstract fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float?
    abstract fun getInstance(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): overgrownNanoforgeEffect?

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
    }
}

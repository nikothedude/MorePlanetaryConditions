package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.utilities.niko_MPC_marketUtils.removeNonNanoforgeProducableCommodities
import data.utilities.niko_MPC_marketUtils.getProducableCommoditiesForOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getProducableCommodityModifiers
import data.utilities.niko_MPC_mathUtils.randomlyDistributeBudgetAcrossCommodities
import org.lazywizard.lazylib.MathUtils
import java.util.*
import kotlin.collections.HashSet

enum class overgrownNanoforgeEffectPrototypes {

    //TODO: Replace params with a special class that has its own budget
    // This is important because as it stands the caller hsa no way of knowing how much budget was actually used up

    ALTER_SUPPLY {
        override fun getPossibleCategories(): Set<overgrownNanoforgeEffectCategories> {
            return setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)
        }
        override fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, maxBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(params, maxBudget)
            val market = params.nanoforge.market
            return (superValue && market.getProducableCommoditiesForOvergrownNanoforge().isNotEmpty())
        }
        override fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float = 50f
        override fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float? {
            val market = params.nanoforge.market
            val producableCommodities = market.getProducableCommodityModifiers()
            removeNonNanoforgeProducableCommodities(producableCommodities.keys)
            if (producableCommodities.isEmpty()) return null
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities.keys) {
                val cost = getCostForCommodity(params, commodityId) ?: continue
                if (lowestCost < cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(params: overgrownNanoforgeRandomizedSourceParams, commodityId: String): Float? {
            return overgrownNanoforgeCommodityDataStore[commodityId]?.cost
        }
        override fun getParamsForInstance(params: overgrownNanoforgeRandomizedSourceParams, maxBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canAfford(params, maxBudget)) return null
            val market = params.nanoforge.market
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
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId, params.nanoforge)
                picker.add(commodityId, weight)
            }

            var timesToPick = getTimesToPickCommodities(params, maxBudget, picker)
            val pickedCommodities = HashSet<String>()
            while (timesToPick-- > 0 && !picker.isEmpty) pickedCommodities += picker.pickAndRemove()
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(pickedCommodities, maxBudget) //assign quantities to the things
            val effect = overgrownNanoforgeAlterSupplySource(params, themeToScore)
            return effect
        }
        private fun getTimesToPickCommodities(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float, picker: WeightedRandomPicker<String>): Int {
            var times: Int = OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
            val threshold = 0.9f
            val randomFloat = MathUtils.getRandom().nextFloat()
            if (randomFloat > threshold) times++
            return times.coerceAtMost(picker.items.size)
        }
    },
        ALTER_UPKEEP {
            override fun getPossibleCategories(): Set<overgrownNanoforgeEffectCategories> {
                return setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.BENEFIT)
            }

            override fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float {
                TODO("Not yet implemented")
            }

            override fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float? {
                TODO("Not yet implemented")
            }

            override fun getParamsForInstance(
                params: overgrownNanoforgeRandomizedSourceParams,
                maxBudget: Float
            ): overgrownNanoforgeEffect? {
                TODO("Not yet implemented")
            }

        },
        ALTER_ACCESSABILITY {

        },
        ALTER_DEFENSES {

        },
        ALTER_STABILITY {
            override fun getPossibleCategories(): Set<overgrownNanoforgeEffectCategories> {
                return setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)
            }
            fun getCostPerStability(params: overgrownNanoforgeRandomizedSourceParams): Float = 25f

            override fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float {
                val weight = 20f
                val market = params.getMarket()
                val stability = market.stability.modifiedValue
                val divisor = (ANCHOR_POINT_FOR_STABILITY/stability).coerceAtLeast(1f)
                // TODO: can i store a variable directly on a enum? ex. store anchor point in val
                var mult = (1/divisor)
                return weight*mult //always returns 0 if stability is 0
            }
            override fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float? = getCostPerStability(params)
            override fun getParamsForInstance(
                params: overgrownNanoforgeRandomizedSourceParams,
                maxBudget: Float
            ): overgrownNanoforgeEffect? {
                TODO("Not yet implemented")
            }

            override fun getInstance(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): overgrownNanoforgeAlterStabilityEffect? {
                var availableBudget = availableBudget
                val instance: overgrownNanoforgeAlterStabilityEffect? = null
                var stabilityIncrement = 0
                var timesToIncrement = getTimesToIncrement(params, availableBudget)
                while (timesToIncrement-- > 0) {
                    availableBudget -= getCostPerStability(params)
                    stabilityIncrement++
                }
                if (stabilityIncrement > 0) instance = overgrownNanoforgeAlterStabilityEffect(TODO(), stabilityIncrement)

                return instance
            }
            fun getTimesToIncrement(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float) {
                var availableBudget = availableBudget
                var timesToIncrement = 0
                while (canAfford(params, availableBudget)) {
                    availableBudget -= getCostPerStability(params)
                    timesToIncrement++
                }
                return timesToIncrement
            }
        ALTER_HAZARD {

        },
        
        // SPECIAL
        EXPLODE_UPON_DESTRUCTION {
            override fun getPossibleCategories(params: overgrownNanoforgeRandomizedSourceParams): MutableSet<overgrownNanoforgeEffectCategories> {
                return setOf(overgrownNanoforgeEffectCategories.SPECIAL)
            }
        }

        SPAWN_HOSTILE_FLEETS {
            override fun getPossibleCategories(params: overgrownNanoforgeRandomizedSourceParams): MutableSet<overgrownNanoforgeEffectCategories> {
                return mutableSetOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.SPECIAL)
            }
        }

    },

    abstract fun getPossibleCategories(): Set<overgrownNanoforgeEffectCategories>
    open fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, maxBudget: Float): Boolean = canAfford(params, maxBudget)
    fun canAfford(params: overgrownNanoforgeRandomizedSourceParams, maxBudget: Float): Boolean {
        val minimumCost = getMinimumCost(params) ?: return false
        return (minimumCost <= maxBudget)
    }
    abstract fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float
    abstract fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float?
    abstract fun getParamsForInstance(params: overgrownNanoforgeRandomizedSourceParams, maxBudget: Float): overgrownNanoforgeEffect?

    companion object {
        val ANCHOR_POINT_FOR_STABILITY: Int = 5
        val allPrototypes = overgrownNanoforgeEffectPrototypes.values().toSet()

        val prototypesByCategory: MutableMap<overgrownNanoforgeEffectCategories, MutableSet<overgrownNanoforgeEffectPrototypes>> =
            EnumMap(overgrownNanoforgeEffectCategories::class.java)
        init {
            for (category in overgrownNanoforgeEffectCategories.values()) prototypesByCategory[category] = HashSet()

            for (entry in overgrownNanoforgeEffectPrototypes.values()) {
                for (category in entry.getPossibleCategories()) prototypesByCategory[category]!! += entry
            }
        }
        fun getPrototype(
            params: overgrownNanoforgeRandomizedSourceParams,
            availableBudget: Float,
            potentialPrototypes: MutableSet<overgrownNanoforgeEffectPrototypes> = getPotentialPrototypes(
                params,
                availableBudget
            )
        ): overgrownNanoforgeEffectPrototypes? {

            val picker = WeightedRandomPicker<overgrownNanoforgeEffectPrototypes>()
            for (prototype in potentialPrototypes) picker.add(prototype, prototype.getWeight(params))

        }

        fun getPotentialPrototypes(params: overgrownNanoforgeRandomizedSourceParams): MutableSet<overgrownNanoforgeEffectPrototypes> {
            val potentialPrototypes = HashSet<overgrownNanoforgeEffectPrototypes>()
            for (prototype in allPrototypes) {
                if (prototype.canBeAppliedTo(params, params.getBudget())) potentialPrototypes += prototype
            }
            return potentialPrototypes
        }
    }
}
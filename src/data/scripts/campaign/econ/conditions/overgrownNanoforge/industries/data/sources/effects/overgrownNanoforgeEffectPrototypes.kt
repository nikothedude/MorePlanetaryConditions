package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.utilities.niko_MPC_marketUtils.removeNonNanoforgeProducableCommodities
import data.utilities.niko_MPC_marketUtils.getProducableCommoditiesForOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getProducableCommodityModifiers
import data.utilities.niko_MPC_mathUtils.randomlyDistributeBudgetAcrossCommodities
import org.lazywizard.lazylib.MathUtils
import java.util.*
import kotlin.collections.HashSet

enum class overgrownNanoforgeEffectPrototypes(
    val possibleCategories: Set<overgrownNanoforgeEffectCategories>
) {

    //TODO: Replace params with a special class that has its own budget
    // This is important because as it stands the caller hsa no way of knowing how much budget was actually used up

    ALTER_SUPPLY(setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)) {
        override fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(nanoforge, maxBudget)
            val market = nanoforge.market
            return (superValue && market.getProducableCommoditiesForOvergrownNanoforge().isNotEmpty())
        }
        override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float = 50f
        override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? {
            val market = nanoforge.market
            val producableCommodities = market.getProducableCommodityModifiers()
            removeNonNanoforgeProducableCommodities(producableCommodities.keys)
            if (producableCommodities.isEmpty()) return null
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities.keys) {
                val cost = getCostForCommodity(nanoforge, commodityId) ?: continue
                if (lowestCost < cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(nanoforge: overgrownNanoforgeIndustry, commodityId: String): Float? {
            return overgrownNanoforgeCommodityDataStore[commodityId]?.cost
        }
        override fun getInstance(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canAfford(nanoforge, maxBudget)) return null
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
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId, nanoforge)
                picker.add(commodityId, weight)
            }

            var timesToPick = getTimesToPickCommodities(nanoforge, maxBudget, picker)
            val pickedCommodities = HashSet<String>()
            while (timesToPick-- > 0 && !picker.isEmpty) pickedCommodities += picker.pickAndRemove()
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(pickedCommodities, maxBudget) //assign quantities to the things
            val effect = overgrownNanoforgeAlterSupplySource(nanoforge, themeToScore)
            return effect
        }
        private fun getTimesToPickCommodities(nanoforge: overgrownNanoforgeIndustry, availableBudget: Float, picker: WeightedRandomPicker<String>): Int {
            var times: Int = OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
            val threshold = 0.9f
            val randomFloat = MathUtils.getRandom().nextFloat()
            if (randomFloat > threshold) times++
            return times.coerceAtMost(picker.items.size)
        }
    },
        ALTER_UPKEEP(setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.BENEFIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float = 10f
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? = getCostPerPointOneIncrement(nanoforge) //TODO: this is bad
            fun getCostPerPointOneIncrement(nanoforge: overgrownNanoforgeIndustry): Float = 25f
            
            override fun getInstance(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): overgrownNanoforgeAlterUpkeepEffect? {
                var mult = TODO() //todo: maybe cap the amount of budget a effect can actually take? i dunno.
                return overgrownNanoforgeAlterUpkeepEffect(nanoforge, mult)
            }

        },
        ALTER_ACCESSABILITY(setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.BENEFIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float = 10f
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? = getCostPerOnePercentAccessability(nanoforge)
            fun getCostPerOnePercentAccessability(nanoforge: overgrownNanoforgeIndustry): Float = 5f
            
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustry,
                maxBudget: Float
            ): overgrownNanoforgeAlterAccessabilityEffect? {
                var remainingBudget = maxBudget
                var increment = 0
                while (canAfford(nanoforge, remainingBudget)) {
                    increment++
                    val cost = getCostPerOnePercentAccessability(nanoforge)
                    remainingBudget -= cost
                }
                if (increment == 0) return null
                return overgrownNanoforgeAlterAccessabilityEffect(nanoforge, increment)
            }

        },
        ALTER_DEFENSES(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float {
                val weight = 10f
                val market = nanoforge.market
                val groundDefense = market.stats.dynamic.getStat(Stats.TODO()).modifiedValue
                val hardLimit = HARD_LIMIT_FOR_DEFENSE //if we are at this or below, we will never ever be picked
                val divisor = (ANCHOR_POINT_FOR_DEFENSE/(groundDefense-hardLimit)).coerceAtLeast(0f)
                var mult = (1/divisor).ensureIsJsonValidFloat()

                return weight*mult
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? = getCostPerOneDefenseRating(nanoforge)
            fun getCostPerOneDefenseRating(nanoforge: overgrownNanoforgeIndustry): Float = 0.5f
            
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustry,
                maxBudget: Float
            ): overgrownNanoforgeAlterDefensesEffect? {
                var remainingBudget = maxBudget
                var increment = 0f
                while (canAfford(nanoforge, remainingBudget)) {
                    increment++
                    val cost = getCostPerOneDefenseRating(nanoforge)
                    remainingBudget -= cost
                }
                if (increment == 0) return 0f
                return overgrownNanoforgeAlterDefensesEffect(nanoforge, increment)
            }
        },

        ALTER_STABILITY(setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)) {
            fun getCostPerStability(nanoforge: overgrownNanoforgeIndustry): Float = 25f

            override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float {
                val weight = 20f
                val market = nanoforge.market
                val stability = market.stability.modifiedValue
                val divisor = (ANCHOR_POINT_FOR_STABILITY/stability).coerceAtLeast(0f)
                // TODO: can i store a variable directly on a enum? ex. store anchor point in val
                var mult = (1/divisor).ensureIsJsonValidFloat()
                return weight*mult //always returns 0 if stability is 0
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? = getCostPerStability(nanoforge)
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustry,
                maxBudget: Float
            ): overgrownNanoforgeEffect? {
                TODO("Not yet implemented")
            }

            override fun getInstance(nanoforge: overgrownNanoforgeIndustry, availableBudget: Float): overgrownNanoforgeAlterStabilityEffect? {
                var availableBudget = availableBudget
                val instance: overgrownNanoforgeAlterStabilityEffect? = null
                var stabilityIncrement = getTimesToIncrement(nanoforge, availableBudget)
                if (stabilityIncrement > 0) instance = overgrownNanoforgeAlterStabilityEffect(TODO(), stabilityIncrement)

                return instance
            }
            fun getTimesToIncrement(nanoforge: overgrownNanoforgeIndustry, availableBudget: Float) {
                var availableBudget = availableBudget
                var timesToIncrement = 0
                while (canAfford(nanoforge, availableBudget)) {
                    val cost = getCostPerStability(nanoforge)
                    availableBudget -= cost
                    timesToIncrement++
                }
                return timesToIncrement
            }
        },
        ALTER_HAZARD() {

        },
        
        // SPECIAL
        EXPLODE_UPON_DESTRUCTION(setOf(overgrownNanoforgeEffectCategories.SPECIAL)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float = 5f
            overide fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): Boolean {
                val superValue = super.canBeAppliedTo(nanoforge, maxBudget)
                val market = nanoforge.market
                return (superValue && market.hasNonJunkStructures())
            }
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float? = getCost(nanoforge)
            fun getCost(nanoforge: overgrownNanoforgeIndustry): Float = 50f
            override fun getInstance(
                nanoforge: overgrownNanoforgeIndustry,
                maxBudget: Float
            ): overgrownNanoforgeVolatileEffect? {
                if (!canAfford(nanoforge, maxBudget)) return null
                return overgrownNanoforgeVolatileEffect(nanoforge)
            }
        },

        SPAWN_HOSTILE_FLEETS(setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.SPECIAL)) {
        };



    open fun canBeAppliedTo(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): Boolean = canAfford(nanoforge, maxBudget)
    fun canAfford(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): Boolean {
        val minimumCost = getMinimumCost(nanoforge) ?: return false
        return (minimumCost <= maxBudget)
    }
    abstract fun getWeight(nanoforge: overgrownNanoforgeIndustry): Float
    abstract fun getMinimumCost(nanoforge: overgrownNanoforgeIndustry): Float?
    abstract fun getInstance(nanoforge: overgrownNanoforgeIndustry, maxBudget: Float): overgrownNanoforgeEffect?

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
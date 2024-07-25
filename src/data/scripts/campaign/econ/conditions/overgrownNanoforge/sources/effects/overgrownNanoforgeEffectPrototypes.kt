package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.*
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetEffect
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_marketUtils.getMaxIndustries
import data.utilities.niko_MPC_marketUtils.getProducableCommoditiesForOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.hasNonJunkStructures
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_mathUtils.ensureIsJsonValidFloat
import data.utilities.niko_MPC_mathUtils.randomlyDistributeBudgetAcrossCommodities
import data.utilities.niko_MPC_mathUtils.randomlyDistributeNumberAcrossEntries
import data.utilities.niko_MPC_settings.ANCHOR_POINT_FOR_DEFENSE
import data.utilities.niko_MPC_settings.HARD_LIMIT_FOR_DEFENSE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
import org.lazywizard.lazylib.MathUtils
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue

enum class overgrownNanoforgeEffectPrototypes(
    val possibleCategories: Set<overgrownNanoforgeEffectCategories>,
    val canInvert: Boolean = possibleCategories.contains(overgrownNanoforgeEffectCategories.DEFICIT)
) {

    ALTER_SUPPLY(setOf(overgrownNanoforgeEffectCategories.BENEFIT, overgrownNanoforgeEffectCategories.DEFICIT)) {
        override fun canBeAppliedTo(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(growth, maxBudget)
            val market = growth.market
            return (superValue && market.getProducableCommoditiesForOvergrownNanoforge().isNotEmpty())
        }

        override fun getIdealTimesToCreate(growth: overgrownNanoforgeHandler, maxBudget: Float): Int {
            var times: Int = OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
            val budgetThreshold = 30f
            if (budgetThreshold > maxBudget) return times
            val threshold = 0.7f
            val randomFloat = MathUtils.getRandom().nextFloat()
            if (randomFloat > threshold) times++
            return times
        }

        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float = 45f
        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float? {
            val market = growth.market
            val producableCommodities = market.getProducableCommoditiesForOvergrownNanoforge()
            if (producableCommodities.isEmpty()) return null
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities) {
                val cost = getCostForCommodity(growth, commodityId) ?: continue
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId, growth, !positive)
                if (weight == 0f) continue
                if (lowestCost > cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(nanoforge: overgrownNanoforgeHandler, commodityId: String): Float? {
            return overgrownNanoforgeCommodityDataStore[commodityId]?.cost
        }
        override fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
            val shouldInvert = maxBudget < 0
            val maxBudget = maxBudget.absoluteValue
            val market = growth.market
            val producableCommodities = market.getProducableCommoditiesForOvergrownNanoforge()

            val picker = WeightedRandomPicker<String>()
            val iterator = producableCommodities.iterator()
            while (iterator.hasNext()) {
                val commodityId: String = iterator.next()
                val cost = getCostForCommodity(growth, commodityId)
                if (cost == null) {
                    iterator.remove()
                    continue
                }
                if (cost > maxBudget) {
                    iterator.remove()
                    continue
                }
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId,
                    growth, shouldInvert)
                if (weight != 0f) picker.add(commodityId, weight)
            }

            val pickedCommodity = picker.pick() ?: return null
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(setOf(pickedCommodity), maxBudget) //assign quantities to the things
            if (shouldInvert) {
                for (entry in themeToScore.entries) {
                    val commodityId = entry.key
                    themeToScore[commodityId] = -themeToScore[commodityId]!!.absoluteValue
                }
            }
            val effect = themeToScore[pickedCommodity]?.let { overgrownNanoforgeAlterSupplySource(growth, pickedCommodity, it) }
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
    SET_INDUSTRY(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
        override fun canBeAppliedTo(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {
            if (growth !is overgrownNanoforgeJunkHandler) return false
            return super.canBeAppliedTo(growth, maxBudget)
        }
        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float {

            val market = growth.market
            val industries = market.industries.size
            var divisor = 1f
            val industryLimitDivisorIncrement: Float = ((industries + 1f) - market.getMaxIndustries()).coerceAtLeast(0f)
            divisor += industryLimitDivisorIncrement

            val base = 10f

            return (base/divisor)
        }

        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCost()
        override fun getMaximumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCost()

        private fun getCost(): Float {
            return 50f
        }

        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeForceIndustryEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
            val castedGrowth = growth as? overgrownNanoforgeJunkHandler ?: return null

            return overgrownNanoforgeForceIndustryEffect(castedGrowth)
        }

    },
        /*ALTER_UPKEEP(setOf(overgrownNanoforgeEffectCategories.DEFICIT)) {
            override fun getWeight(nanoforge: overgrownNanoforgeIndustryHandler): Float = 0f //TODO: disabled
            override fun getMinimumCost(nanoforge: overgrownNanoforgeIndustryHandler): Float? = getCostPerPointOneIncrement(nanoforge) //TODO: this is bad
            fun getCostPerPointOneIncrement(nanoforge: overgrownNanoforgeIndustryHandler): Float = 25f
            
            override fun getInstance(nanoforge: overgrownNanoforgeIndustryHandler, maxBudget: Float): overgrownNanoforgeAlterUpkeepEffect? {
                if (!canBeAppliedTo(nanoforge, maxBudget)) return null
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
        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float = 19f
        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float? = getCostPerOnePercentAccessability(growth)
        fun getCostPerOnePercentAccessability(nanoforge: overgrownNanoforgeHandler): Float = 2f

        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeAlterAccessibilityEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
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
        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float {
            val weight = 30f
            val market = growth.market
            val groundDefense = market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).computeEffective(0f)
            val hardLimit: Float = HARD_LIMIT_FOR_DEFENSE //if we are at this or below, we will never ever be picked
            val divisor: Float = (ANCHOR_POINT_FOR_DEFENSE/(groundDefense-hardLimit)).coerceAtLeast(0f)
            var mult = ((1f/divisor).ensureIsJsonValidFloat()).toFloat()

            return weight*mult
        }
        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCostPerOnePointOneDefenseRating(growth)
        override fun getMaximumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float {
            return getMinimumCost(growth, positive) * 1/getIncrementAmount()
        }
        fun getIncrementAmount(): Float = 0.01f
        fun getCostPerOnePointOneDefenseRating(nanoforge: overgrownNanoforgeHandler): Float = 5f

        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeAlterDefensesEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
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
        fun getCostPerStability(nanoforge: overgrownNanoforgeHandler): Float = 30f

        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float {
            val weight = 20f
            val market = growth.market
            val stability: Float = if (!market.isInhabited()) ANCHOR_POINT_FOR_STABILITY.toFloat() else market.stability.modifiedValue
            val stabilityAnchorMult: Float = (stability/ANCHOR_POINT_FOR_STABILITY).coerceAtMost(1f)
            // TODO: can i store a variable directly on a enum? ex. store anchor point in val
            var mult = ((1*stabilityAnchorMult).ensureIsJsonValidFloat()).toFloat()
            return weight*mult //always returns 0 if stability is 0
        }
        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCostPerStability(growth)

        override fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeAlterStabilityEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
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
        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float = 15f

        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float {
            return getCostPerOnePercent(growth)
        }

        fun getCostPerOnePercent(nanoforge: overgrownNanoforgeHandler): Float = 2f

        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeAlterHazardEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null //worth noting: positive alterations should return a NEGATIVE value
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
        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float = 5f
        override fun canBeAppliedTo(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(growth, maxBudget)
            val market = growth.market
            return (superValue && market.hasNonJunkStructures())
        }
        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float? = getCost(growth)
        fun getCost(nanoforge: overgrownNanoforgeHandler): Float = 50f
        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeVolatileEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null
            return overgrownNanoforgeVolatileEffect(growth)
        }
    },

    SPAWN_HOSTILE_FLEETS(setOf(overgrownNanoforgeEffectCategories.DEFICIT, overgrownNanoforgeEffectCategories.BENEFIT)) {

        private val POPULATION_SIZE_ANCHOR: Int = 6
        private val POPULATION_SIZE_MULT_DIVISOR: Float = 2f

        override fun canBeAppliedTo(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {

            val system = growth.market.starSystem
            if (system != null && shouldntApplyFleetScript(system)) return false

            return super.canBeAppliedTo(growth, maxBudget)
        }

        override fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float {

            val negative = budget <= 0
            val negativeMult = if (!negative) 0.05f else 1f

            val weight = 40f //40f
            val market = growth.market
            val popSize: Int = if (!market.isInhabited()) POPULATION_SIZE_ANCHOR else market.size
            val popDivided = (popSize/POPULATION_SIZE_ANCHOR.toFloat())
            val popsizeAnchorMult: Float = (popDivided).coerceAtMost(1f)
            var mult = ((1*popsizeAnchorMult).ensureIsJsonValidFloat()).toFloat()
            return (weight*mult)*negativeMult
        }

        override fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCost(growth, positive)
        override fun getMaximumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float = getCost(growth, positive)
        fun getCost(nanoforge: overgrownNanoforgeHandler, positive: Boolean): Float {
            return if (positive) 80f else 60f
        }
        override fun getInstance(
            growth: overgrownNanoforgeHandler,
            maxBudget: Float
        ): overgrownNanoforgeSpawnFleetEffect? {
            if (!canBeAppliedTo(growth, maxBudget)) return null

            var availableBudget = maxBudget
            val hostile = availableBudget <= 0

            val clock = Global.getSector().clock
            return overgrownNanoforgeSpawnFleetEffect(
                growth,
                hostile,
                clock.convertToSeconds(15f),
                clock.convertToSeconds(25f),
                500f)
        }
    };



    open fun canBeAppliedTo(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean = canAfford(growth, maxBudget)
    fun canAfford(growth: overgrownNanoforgeHandler, maxBudget: Float): Boolean {
        val shouldInvert = maxBudget < 0
        val maxBudget = if (canInvert) maxBudget.absoluteValue else maxBudget
        val minimumCost = getMinimumCost(growth, !shouldInvert) ?: return false
        return (minimumCost <= maxBudget)
    }
    abstract fun getWeight(growth: overgrownNanoforgeHandler, budget: Float): Float
    abstract fun getMinimumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float?
    open fun getMaximumCost(growth: overgrownNanoforgeHandler, positive: Boolean): Float? = Float.MAX_VALUE
    abstract fun getInstance(growth: overgrownNanoforgeHandler, maxBudget: Float): overgrownNanoforgeEffect?
    open fun getIdealTimesToCreate(growth: overgrownNanoforgeHandler, maxBudget: Float): Int = 1

    companion object {
        val blacklistedFleetSpawnerSystemTags = mutableSetOf<String>(
            Tags.THEME_RUINS,
            Tags.PK_SYSTEM,
            Tags.THEME_SPECIAL,
            Tags.THEME_REMNANT_RESURGENT,
            Tags.THEME_REMNANT_SECONDARY,
            Tags.THEME_CORE_POPULATED
        )

        val ANCHOR_POINT_FOR_STABILITY: Int = 2
        val allPrototypes = overgrownNanoforgeEffectPrototypes.values().toSet()

        val prototypesByCategory: MutableMap<overgrownNanoforgeEffectCategories, MutableSet<overgrownNanoforgeEffectPrototypes>> =
            EnumMap(overgrownNanoforgeEffectCategories::class.java)

        init {
            for (category in overgrownNanoforgeEffectCategories.values()) prototypesByCategory[category] = HashSet()

            for (entry in overgrownNanoforgeEffectPrototypes.values()) {
                for (category in entry.possibleCategories) prototypesByCategory[category]!! += entry
            }
        }

        fun shouldntApplyFleetScript(system: StarSystemAPI): Boolean {
            if (Global.getCurrentState() == GameState.TITLE) {
                if (!system.isProcgen) return true
                for (tag in blacklistedFleetSpawnerSystemTags) {
                    if (system.hasTag(tag)) return true
                }
            }
            return false
        }

        fun getPotentialPrototypes(
            handler: overgrownNanoforgeHandler,
            allowedCategories: Set<overgrownNanoforgeEffectCategories> = setOf(
                overgrownNanoforgeEffectCategories.BENEFIT,
                overgrownNanoforgeEffectCategories.DEFICIT
            ),
            budget: Float
        ): MutableSet<overgrownNanoforgeEffectPrototypes> {
            val potentialPrototypes = HashSet<overgrownNanoforgeEffectPrototypes>()
            for (prototype in ArrayList(allPrototypes)) {
                if (!prototype.possibleCategories.any { allowedCategories.contains(it) }) continue
                if (prototype.canBeAppliedTo(handler, budget)) potentialPrototypes += prototype
            }
            return potentialPrototypes
        }

        fun getWeightedPotentialPrototypes(
            handler: overgrownNanoforgeHandler,
            allowedCategories: Set<overgrownNanoforgeEffectCategories> = setOf(
                overgrownNanoforgeEffectCategories.BENEFIT,
                overgrownNanoforgeEffectCategories.DEFICIT
            ),
            budget: Float
        ): MutableMap<overgrownNanoforgeEffectPrototypes, Float> {
            val potentialPrototypes = getPotentialPrototypes(handler, allowedCategories, budget)

            val weightedPrototypes = HashMap<overgrownNanoforgeEffectPrototypes, Float>()
            for (prototype in potentialPrototypes) {
                val weight = prototype.getWeight(handler, budget)
                weightedPrototypes[prototype] = weight
            }
            return weightedPrototypes
        }

        fun wrapPrototypes(
            handler: overgrownNanoforgeHandler,
            budget: Float,
            prototypes: MutableSet<overgrownNanoforgeEffectPrototypes>
        ): MutableSet<prototypeHolder> {

            val wrappedPrototypes = HashSet<prototypeHolder>()

            for (entry in prototypes) {
                val duplicatePrototypes = getPrototypeInstantiationList(entry, handler, budget)
                for (prototype in duplicatePrototypes) wrappedPrototypes += prototypeHolder(prototype)
            }
            return wrappedPrototypes
        }

        fun scorePrototypes(
            handler: overgrownNanoforgeHandler,
            budgetHolder: budgetHolder,
            allowedCategories: Set<overgrownNanoforgeEffectCategories> = setOf(
                overgrownNanoforgeEffectCategories.BENEFIT,
                overgrownNanoforgeEffectCategories.DEFICIT
            ),
            wrappedPrototypes: MutableSet<prototypeHolder> = wrapPrototypes(
                handler,
                budgetHolder.budget,
                getPotentialPrototypes(handler, allowedCategories, budgetHolder.budget)
            )
        ): MutableMap<prototypeHolder, Float> {

            val budget = budgetHolder.budget
            val negative = budget < 0
            val scoredWrappedPrototypes = randomlyDistributeNumberAcrossEntries(
                wrappedPrototypes,
                abs(budget),
                { budget: Float, remainingRuns: Int, entry: prototypeHolder ->
                    entry.prototype.getMinimumCost(
                        handler,
                        !negative
                    ) ?: 0f
                },
                { budget: Float, remainingRuns: Int, entry: prototypeHolder ->
                    (entry.prototype.getMaximumCost(handler, !negative))?.coerceAtMost(budget) ?: budget
                },
            )

            if (negative) {
                for (entry in scoredWrappedPrototypes) {
                    scoredWrappedPrototypes[entry.key] = entry.value * -1f
                }
            }

            for (entry in scoredWrappedPrototypes) {
                budgetHolder.budget -= entry.value
            }

            return scoredWrappedPrototypes
        }

        fun getRandomPrototypes(
            handler: overgrownNanoforgeHandler,
            budgetHolder: budgetHolder,
            allowedCategories: Set<overgrownNanoforgeEffectCategories>,
            timesToPick: Int
        ): MutableMap<prototypeHolder, Float> {

            val weightedPotentialPrototypes =
                getWeightedPotentialPrototypes(handler, allowedCategories, budgetHolder.budget)
            val pickedPrototypes = pickPrototypes(handler, budgetHolder, timesToPick, weightedPotentialPrototypes)

            val scoredPrototypes = scorePrototypes(handler, budgetHolder, allowedCategories, pickedPrototypes)
            return scoredPrototypes
        }

        private fun pickPrototypes(
            handler: overgrownNanoforgeHandler,
            budgetHolder: budgetHolder,
            timesToPick: Int,
            weightedPrototypes: MutableMap<overgrownNanoforgeEffectPrototypes, Float>
        ): MutableSet<prototypeHolder> {
            val pickedList: MutableSet<prototypeHolder> = HashSet()
            var picksLeft = timesToPick
            var remainingBudget = budgetHolder.budget

            var absBudget = abs(remainingBudget)

            val picker = WeightedRandomPicker<overgrownNanoforgeEffectPrototypes>()

            for (entry in weightedPrototypes) {
                picker.add(entry.key, entry.value)
            }

            var index: Int = 0
            val threshold = 20
            while (true) {
                index++

                while (picksLeft > 0) {
                    picksLeft--
                    val picked = picker.pickAndRemove() ?: break

                    val maxCost = picked.getMaximumCost(handler, budgetHolder.budget > 0)
                    if (maxCost == null) {
                        displayError("null maxcost during pickPrototypes")
                        continue // this is chaos if this happens
                    }
                    absBudget -= maxCost
                    pickedList += prototypeHolder(picked)
                }

                if (picker.isEmpty) break

                if (absBudget > 0) {
                    picksLeft++ //try again
                } else break

                if (index >= threshold) {
                    displayError("anti-loop failsafe triggered during pickprototypes")
                    break
                }
            }

            return pickedList
        }


        fun generateEffects(
            handler: overgrownNanoforgeHandler,
            scoredPrototypes: MutableMap<prototypeHolder, Float>): MutableSet<overgrownNanoforgeEffect> {

            val effects: MutableSet<overgrownNanoforgeEffect> = HashSet()
            for (entry in scoredPrototypes) {
                val prototype = entry.key.prototype
                val score = entry.value
                val instance = prototype.getInstance(handler, score) ?: continue
                effects += instance
            }
            return effects
        }

        fun getPrototypeInstantiationList(
            prototype: overgrownNanoforgeEffectPrototypes,
            handler: overgrownNanoforgeHandler,
            budget: Float
        ): MutableList<overgrownNanoforgeEffectPrototypes> {

            if (!prototype.canBeAppliedTo(handler, budget)) return ArrayList()

            val prototypeCopies = ArrayList<overgrownNanoforgeEffectPrototypes>()
            
            var idealTimes = prototype.getIdealTimesToCreate(handler, budget)

            while (idealTimes-- > 0) {
                prototypeCopies += prototype
            }

            return prototypeCopies
        }

        fun getWrappedInstantiationList(
            prototype: overgrownNanoforgeEffectPrototypes,
            handler: overgrownNanoforgeHandler,
            budget: Float
        ): MutableSet<prototypeHolder> {
            
            val instantionList = getPrototypeInstantiationList(prototype, handler, budget)
            val holders = HashSet<prototypeHolder>()

            for (entry in instantionList) {
                holders += prototypeHolder(entry)
            }
            return holders
        }

        // some jank to let us hold duplicates in hashmaps
        class prototypeHolder(
            val prototype: overgrownNanoforgeEffectPrototypes
        )

    }
}

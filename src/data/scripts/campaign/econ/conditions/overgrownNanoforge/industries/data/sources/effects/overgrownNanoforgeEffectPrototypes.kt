package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import data.utilities.niko_MPC_marketUtils.getProducableCommoditiesForOvergrownNanoforge
import data.utilities.niko_MPC_mathUtils.randomlyDistributeBudgetAcrossCommodities
import org.lazywizard.lazylib.MathUtils
import kotlin.collections.HashSet

enum class overgrownNanoforgeEffectPrototypes {

    ALTER_SUPPLY {
        override fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(params, availableBudget)
            val market = params.nanoforge.market
            return (superValue && market.getProducableCommoditiesForOvergrownNanoforge().isNotEmpty())
        }
        override fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float = 5f
        override fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float? {
            val market = params.nanoforge.market
            val producableCommodities = market.getProducableCommodityModifiers()
            getNanoforgeProducableCommoditiesOutOfList(producableCommodities.keys)
            if (producableCommodities.isEmpty()) return null 
            var lowestCost = Float.MAX_VALUE
            for (commodityId in producableCommodities.keys) {
                val cost = getCostForCommodity(params, commodityId) ?: continue
                if (lowestCost < cost) lowestCost = cost
            }
            return lowestCost
        }
        fun getCostForCommodity(params: overgrownNanoforgeRandomizedSourceParams, commodityId: String): Float? {
            val market = params.nanoforge.market
            return overgrownNanoforgeCommodityDataStore[entry]?.cost
        }
        override fun getInstance(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): overgrownNanoforgeAlterSupplySource? {
            if (!canAfford(params, availableBudget)) return null
            val market = params.nanoforge.market
            val producableCommodities = market.getProducableCommoditiesForOvergrownNanoforge()

            val picker = WeightedRandomPicker<String>()
            val iterator = producableCommodities.iterator()
            while (iterator.hasNext()) {
                val commodityId: String = iterator.next()
                val cost = overgrownNanoforgeCommodityDataStore[commodityId]!!.cost
                if (cost > availableBudget) {
                    iterator.remove()
                    continue
                }
                val weight = overgrownNanoforgeCommodityDataStore.getWeightForCommodity(commodityId, params.nanoforge)
                picker.add(commodityId, weight)
            }

            var timesToPick = getTimesToPickCommodities(params, availableBudget, picker)
            val pickedCommodities = HashSet<String>()
            while (timesToPick-- > 0 && !picker.isEmpty) pickedCommodities += picker.pickAndRemove()
            val themeToScore = randomlyDistributeBudgetAcrossCommodities(pickedCommodities, availableBudget) //assign quantities to the things
            val effect = overgrownNanoforgeAlterSupplySource(TODO(), themeToScore)
            return effect
        }
        private fun getTimesToPickCommodities(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float, picker: WeightedRandomPicker<String>): Float {
            var times: Float = OVERGROWN_NANOFORGE_ALTER_SUPPLY_EFFECT_MIN_COMMODITY_TYPES
            val threshold = 0.9f
            val randomFloat = MathUtils.getRandom().nextFloat()
            if (randomFloat > threshold) times++
            return times.coerceAtMost(picker.items.size.toFloat())
        }
    },

    open fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean = canAfford(params, availableBudget)
    fun canAfford(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean {
        val minimumCost = getMinimumCost(params) ?: return false
        return (getMinimumCost(params) <= availableBudget)
    }
    abstract fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float
    abstract fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float?
    abstract fun getInstance(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): overgrownNanoforgeEffect?

    companion object {
        val allPrototypes = overgrownNanoforgeEffectPrototypes.values().toSet()

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

        fun getPotentialPrototypes(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): MutableSet<overgrownNanoforgeEffectPrototypes> {
            val potentialPrototypes = HashSet<overgrownNanoforgeEffectPrototypes>()
            for (prototype in allPrototypes) {
                if (prototype.canBeAppliedTo(params, availableBudget)) potentialPrototypes += prototype
            }
            return potentialPrototypes
        }
    }
}
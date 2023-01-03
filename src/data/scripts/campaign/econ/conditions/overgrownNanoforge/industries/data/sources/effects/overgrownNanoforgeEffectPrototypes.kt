package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import kotlin.collections.HashSet

enum class overgrownNanoforgeEffectPrototypes {

    ALTER_SUPPLY {
        override fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean {
            val superValue = super.canBeAppliedTo(params, availableBudget)
            return superValue
        }
        override fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float = 5f
        override fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float {
            TODO("Not yet implemented")
        }
        override fun getInstance(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): overgrownNanoforgeAlterSupplySource {
            TODO("Not yet implemented")
        }
    },

    open fun canBeAppliedTo(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean = canAfford(params, availableBudget)
    fun canAfford(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): Boolean = getMinimumCost(params) <= availableBudget
    abstract fun getWeight(params: overgrownNanoforgeRandomizedSourceParams): Float
    abstract fun getMinimumCost(params: overgrownNanoforgeRandomizedSourceParams): Float
    abstract fun getInstance(params: overgrownNanoforgeRandomizedSourceParams, availableBudget: Float): overgrownNanoforgeEffect

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
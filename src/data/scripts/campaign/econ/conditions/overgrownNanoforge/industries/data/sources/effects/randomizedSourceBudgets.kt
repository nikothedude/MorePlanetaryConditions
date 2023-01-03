package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

enum class randomizedSourceBudgets(
    val chance: Float, val value: Float
) {
    EXTREMELY_LOW(0.1f, 25f),
    VERY_LOW(1f, 50f),
    LOW(5f, 75f),
    MEDIUM(50f, 100f),
    HIGH(10f, 125f),
    VERY_HIGH(5f, 150f),
    EXTREMELY_HIGH(1f, 175f),
    INSANE(0.5f, 200f)
}

object randomizedSourceBudgetsPicker: WeightedRandomPicker<randomizedSourceBudgets>() {
    fun getRandomBudget(nanoforge: overgrownNanoforgeIndustry): Float {
        val market = nanoforge.market
        val pickedBudget = pick().value
        return pickedBudget*OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT
    }

    init {
        val budgets = randomizedSourceBudgets.values().toMutableList()
        for (entry in budgets) add(entry, entry.chance)
    }
}
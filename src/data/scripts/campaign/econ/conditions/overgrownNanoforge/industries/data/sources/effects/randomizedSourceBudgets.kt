package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN
import org.lazywizard.lazylib.MathUtils

enum class randomizedSourceBudgets(
    val chance: Float, val value: Float
) {
    USELESS(0.01f, 10f),
    EXTREMELY_LOW(0.1f, 25f),
    VERY_LOW(1f, 50f),
    LOW(5f, 75f),
    MEDIUM(30f, 100f),
    HIGH(10f, 125f),
    VERY_HIGH(5f, 150f),
    EXTREMELY_HIGH(1f, 175f),
    INSANE(0.5f, 200f),
    EXOTIC(0.1f, 280f)
}

object randomizedSourceBudgetsPicker: WeightedRandomPicker<randomizedSourceBudgets>() {
    fun getRandomBudget(nanoforge: overgrownNanoforgeIndustry): Float {
        val pickedBudget = pick().value
        val randomMult = MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN, OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX)
        return pickedBudget*randomMult
    }

    init {
        val budgets = randomizedSourceBudgets.values().toMutableList()
        for (entry in budgets) add(entry, entry.chance)
    }
}
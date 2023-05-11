package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN
import org.lazywizard.lazylib.MathUtils

enum class randomizedSourceBudgets(
    val chance: Float, val value: Float
) {
    USELESS(0.01f, 10f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    EXTREMELY_LOW(0.1f, 25f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    VERY_LOW(1f, 50f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    LOW(5f, 75f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    MEDIUM(30f, 100f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    HIGH(10f, 125f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    VERY_HIGH(5f, 150f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    EXTREMELY_HIGH(1f, 175f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    INSANE(0.5f, 200f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT),
    EXOTIC(0.1f, 280f*OVERGROWN_NANOFORGE_OVERALL_BUDGET_MULT)
}

object randomizedSourceBudgetsPicker: WeightedRandomPicker<randomizedSourceBudgets>() {
    fun getRandomBudget(handler: overgrownNanoforgeHandler): Float {
        val pickedBudget = pick().value
        val randomMult = MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MIN, OVERGROWN_NANOFORGE_RANDOM_BUDGET_MULT_MAX)
        return pickedBudget*randomMult
    }

    init {
        val budgets = randomizedSourceBudgets.values().toMutableList()
        for (entry in budgets) add(entry, entry.chance)
    }
}
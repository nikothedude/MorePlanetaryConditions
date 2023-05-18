package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.baseOvergrownNanoforgeIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeGrowthIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.baseOvergrownNanoforgeManipulationIntel
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT
import lunalib.lunaUI.elements.LunaProgressBar
import org.lazywizard.lazylib.MathUtils
import java.lang.Math.abs

class overgrownNanoforgeSpreadingBrain(
    val industryNanoforge: overgrownNanoforgeIndustryHandler
) {

    var viewingMode = viewMode.DEFAULT

    var maxGrowthManipulation: Float = OVERGROWN_NANOFORGE_MAXIMUM_GROWTH_MANIPULATION
    var minGrowthManipulation: Float = -maxGrowthManipulation

    fun getOverallGrowthManipulation(): Float {
        var amount = 0f
        getAllIntel().forEach { amount += kotlin.math.abs(it.growthManipulation) }
        return amount
    }

    var hidden: Boolean = true
        set(value) {
            if (field != value) {
                updateIntelHiddenStatus(value)
            }
            field = value
        }

    private fun updateIntelHiddenStatus(value: Boolean) {
        getAllIntel().forEach { it.isHidden = value }
    }
    private fun getAllIntel(): Set<baseOvergrownNanoforgeManipulationIntel> {
        return (intelInstances + spreadingIntel)
    }
    val spreadingIntel: overgrownNanoforgeGrowthIntel = createBaseSpreadingIntel()

    private fun createBaseSpreadingIntel(): overgrownNanoforgeGrowthIntel {
        return overgrownNanoforgeGrowthIntel(this, industryNanoforge)
    }
    val intelInstances: MutableSet<baseOvergrownNanoforgeManipulationIntel> = HashSet()

    fun init() {

    }

    private fun updateUIs() {
        getAllIntel().forEach { it.updateUI() }
    }

    fun startDestroyingStructure(handler: overgrownNanoforgeHandler) {
        val newIntel = createDestructionIntel(handler)
        newIntel.init()
    }

    private fun createDestructionIntel(handler: overgrownNanoforgeHandler): baseOvergrownNanoforgeManipulationIntel {
        return baseOvergrownNanoforgeManipulationIntel(this, handler)
    }

    fun delete() {
        getAllIntel().forEach { it.delete() }
    }

    fun getMarket(): MarketAPI = industryNanoforge.market
    fun getManipulationBudget(intel: baseOvergrownNanoforgeIntel): Float {
        return (maxGrowthManipulation - (getOverallGrowthManipulation() - intel.growthManipulation))
    }

    fun calculateOverallCreditCost(): Float {
        val absIntensity = kotlin.math.abs(getOverallGrowthManipulation())
        val standardCost = (absIntensity * OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT)
        val extraCost = (((absIntensity - OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD).coerceAtLeast(0f)) * niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT)

        return (standardCost + extraCost)* baseOvergrownNanoforgeIntel.getOverallCullingStrength(getMarket())
    }
}
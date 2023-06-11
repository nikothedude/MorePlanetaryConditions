package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeGrowthIntel
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.hasNonJunkStructures
import data.utilities.niko_MPC_marketUtils.isApplied
import data.utilities.niko_MPC_marketUtils.isJunk
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_marketUtils.isPopulationAndInfrastructure
import data.utilities.niko_MPC_marketUtils.isSpacePort
import data.utilities.niko_MPC_marketUtils.isVisible
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

class overgrownSpreadingParams(
    val handler: overgrownNanoforgeJunkHandler,
    val brain: overgrownNanoforgeSpreadingBrain,
    target: Industry? = null
) {

    var nameKnown: Boolean = false


    val percentThresholdToTotalScoreKnowledge = MathUtils.getRandomNumberInRange(30, 60)

    var ourIndustryTarget = target ?: getIndustryTarget()
        set(value) {
            getGrowthIntel()?.alertPlayerTargetChanged(value)
            field = value
        }

    init {
        nameKnown = ourIndustryTarget == null
    }

    fun getGrowthIntel(): overgrownNanoforgeGrowthIntel? {
        return brain.growthIntel
    }

    fun getIndustryName(): String {
        if (ourIndustryTarget == null) return "None"
        if (nameKnown) return ourIndustryTarget?.currentName ?: "None"

        return "Unknown"
    }

    fun getIndustryTarget(): Industry? {
        if (!getMarket().hasMaxStructures()) return null

        var population: Industry? = null
        val picker: WeightedRandomPicker<Industry> = WeightedRandomPicker()
        for (structure in getMarket().industries) {
            if (!structure.isValidTarget()) continue
            if (structure is PopulationAndInfrastructure) {
                population = structure
                continue
            }
            picker.add(structure, getTargettingChance(structure))
        }
        return picker.pick() ?: population
    }

    fun getTargettingChance(structure: Industry): Float {
        var score = niko_MPC_settings.overgrownNanoforgeBaseJunkSpreadTargettingChance
        if (structure.isImproved) score *= 0.8f
        if (structure.isOrbital()) score *= 0.2f
        if (structure.isSpacePort()) score *= 0.5f
        return score
    }

    fun Industry.isValidTarget(): Boolean {
        return (isVisible() && isApplied() && !isJunkStructure())
    }

    fun getMarket(): MarketAPI = handler.market

    fun init() {

    }

    fun spread() {
        val decivilized = getGrowthIntel()?.destroyTarget()
        if (decivilized == true) {
            handler.delete() // dont let us fill all 12 slots
        } else {
            handler.instantiate()
        }

        reportSpreaded()
    }

    private fun reportSpreaded() {
    }

    fun updateIndustryTarget() {
        if (shouldRetarget())  {
            ourIndustryTarget = getIndustryTarget()
        }
    }

    private fun shouldRetarget(): Boolean {
      if (ourIndustryTarget == null && getMarket().hasMaxStructures()) return true
        if (ourIndustryTarget != null) {
            if (!getMarket().hasMaxStructures()) return true
            if (ourIndustryTarget is PopulationAndInfrastructure) {
                for (industry in getMarket().industries) {
                    if (industry != ourIndustryTarget && !industry.isJunkStructure()) {
                        return true
                    }
                }
            }
            if (!ourIndustryTarget!!.isValidTarget()) return true
        }
        return false
    }
}


package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.hasNonJunkStructures
import data.utilities.niko_MPC_marketUtils.isApplied
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

class overgrownSpreadingParams(
    val handler: overgrownNanoforgeJunkHandler,
    target: Industry? = null
) {

    var nameKnown: Boolean = false

    val percentThresholdToTotalScoreKnowledge = MathUtils.getRandomNumberInRange(30, 60)

    var ourIndustryTarget = target ?: getIndustryTarget()
        set(value) {
            alertPlayerTargetChanged(value)
            field = value
        }

    fun getIndustryName(): String {
        if (nameKnown) return ourIndustryTarget?.currentName ?: "None"

        return "Unknown"
    }

    fun alertPlayerTargetChanged(newIndustry: Industry?) {
        Global.getSector().campaignUI.addMessage("industry changed to $newIndustry")
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
        return score
    }

    fun Industry.isValidTarget(): Boolean {
        return (isApplied() && !isJunkStructure())
    }

    fun getMarket(): MarketAPI = handler.market

    fun init() {

    }

    fun spread() {
        destroyTarget()
        handler.instantiate()

        reportSpreaded()
    }

    private fun reportSpreaded() {
        Global.getSector().campaignUI.addMessage("spread complete woo")
    }

    fun destroyTarget() {
        if (ourIndustryTarget != null) {
            getMarket().removeIndustry(ourIndustryTarget!!.id, null, false)
        }
    }

    fun updateIndustryTarget() {
        if (shouldRetarget()) ourIndustryTarget = getIndustryTarget()
    }

    private fun shouldRetarget(): Boolean {
      if (ourIndustryTarget == null && getMarket().hasMaxStructures()) return true
        if (ourIndustryTarget != null) {
            if (!getMarket().hasMaxStructures()) return true
            if (ourIndustryTarget is PopulationAndInfrastructure && getMarket().hasNonJunkStructures()) return true
        }
        return false
    }
}


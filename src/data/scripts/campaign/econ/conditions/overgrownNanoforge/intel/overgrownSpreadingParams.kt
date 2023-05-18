package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.isApplied
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN
import org.lazywizard.lazylib.MathUtils

class overgrownSpreadingParams(
    val handler: overgrownNanoforgeHandler,
    target: Industry? = null
) {

    var industryTarget = target ?: getIndustryTarget()
        set(value) {
            alertPlayerTargetChanged(value)
            field = value
        }

    fun alertPlayerTargetChanged(newIndustry: Industry?) {

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
    fun getIntel(): baseOvergrownNanoforgeIntel = getMarket().getOvergrownNanoforgeIndustryHandler()!!.intel

    var cullingResistance = getInitialCullingResistance()

    fun getInitialCullingResistance(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
        )
    }

    var cullingResistanceRegeneration = createBaseCullingResistanceRegeneration()

    fun createBaseCullingResistanceRegeneration(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
        )
    }

    fun init() {
        TODO("Not yet implemented")
    }
}


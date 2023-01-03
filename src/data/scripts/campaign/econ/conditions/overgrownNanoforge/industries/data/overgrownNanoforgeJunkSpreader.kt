package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.scripts.overgrownNanoforgeJunkSpreadingScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_settings.overgrownNanoforgeBaseJunkSpreadTargettingChance
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeJunkSpreader(
    val nanoforge: overgrownNanoforgeIndustry
) {
    val spreadingTimer = IntervalUtil(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    var spreadingScript: overgrownNanoforgeJunkSpreadingScript? = null

    fun spreadJunkIfPossible(amount: Float) {
        val dayAmount = Misc.getDays(amount)
        if (shouldSpreadJunk()) {
            tryToSpreadJunk(dayAmount) //only increment the timer if we should even try
        }
    }

    fun shouldSpreadJunk(): Boolean {
        if (getMarket().exceedsMaxStructures() || spreadingScript != null) {
            spreadingTimer.elapsed = 0f
            return false
        }
        return true
    }

    fun tryToSpreadJunk(dayAmount: Float) {
        spreadingTimer.advance(dayAmount)
        if (spreadingTimer.intervalElapsed()) spreadJunk()
    }


    fun spreadJunk(): overgrownNanoforgeJunkSpreadingScript? {
        val script = createSpreadingScript()
        script?.start()
        return script
    }

    private fun createSpreadingScript(): overgrownNanoforgeJunkSpreadingScript? {
        val target: Industry? = getTargetForJunk()
        val minDays = getMinSpreadingDays()
        val maxDays = getMaxSpreadingDays()
        val timeTilSpread = MathUtils.getRandomNumberInRange(minDays, maxDays)

        val effectParams = generateSourceParams() ?: return null
        val constructionParams = overgrownNanoforgeJunkSpreadingScript.createParams(target, effectParams)

        spreadingScript = createSpreadingScriptInstance(target, timeTilSpread, constructionParams)
        return spreadingScript
    }

    private fun getSourceType(): overgrownNanoforgeSourceTypes? {
        val picker = WeightedRandomPicker<overgrownNanoforgeSourceTypes>()
        picker.addAll(getAdjustedTypeChances())

        return picker.pick()
    }

    fun generateSourceParams(): overgrownNanoforgeRandomizedSourceParams? {
        val chosenSourceType = getSourceType() ?: return null
        return overgrownNanoforgeRandomizedSourceParams(nanoforge, chosenSourceType)
    }

    private fun getAdjustedTypeChances(): MutableList<overgrownNanoforgeSourceTypes> {
        val types = overgrownNanoforgeSourceTypes.values().toMutableList()
        val iterator = types.iterator()
        while (iterator.hasNext()) {
            if (types.size <= 1) break
            val type = iterator.next()
            when (type) {
                overgrownNanoforgeSourceTypes.STRUCTURE -> {
                    if (!OVERGROWN_NANOFORGE_USE_JUNK_STRUCTURES) {
                        iterator.remove()
                        continue
                    }
                }
                overgrownNanoforgeSourceTypes.INTERNAL -> {}
            }
        }
        return types
    }

    fun createSpreadingScriptInstance(
        target: Industry?,
        timeTilSpread: Float,
        params: overgrownNanoforgeJunkSpreadingScript.constructionParams
    ): overgrownNanoforgeJunkSpreadingScript {
        return overgrownNanoforgeJunkSpreadingScript(nanoforge, timeTilSpread, params)
    }

    fun getMinSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS
    }

    fun getMaxSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS
    }

    fun getTargetForJunk(): Industry? {
        if (!getMarket().hasMaxStructures()) return null
        var population: Industry? = null
        val picker: WeightedRandomPicker<Industry> = WeightedRandomPicker()
        for (structure in getMarket().industries) {
            if (structure is PopulationAndInfrastructure) {
                population = structure
                continue
            }
            picker.add(structure, getTargettingChance(structure))
        }
        return picker.pick() ?: population
    }

    fun getTargettingChance(structure: Industry): Float {
        var score = overgrownNanoforgeBaseJunkSpreadTargettingChance
        if (structure.isImproved) score *= 0.8f
        if (structure.isOrbital()) score *= 0.2f
        return score
    }

    fun getMarket(): MarketAPI {
        return nanoforge.market
    }

    fun delete() {

    }

    fun getExistingJunk(): MutableSet<overgrownNanoforgeJunk> {
        return nanoforge.junk
    }
}

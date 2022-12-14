package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.scripts.overgrownNanoforgeJunkSpreadingScript
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.utilities.niko_MPC_marketUtils
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_settings.overgrownNanoforgeBaseJunkSpreadTargettingChance
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeJunkSpreader(
    val nanoforge: overgrownNanoforgeIndustry
) {

    val timeTilSpread = spreadTimer(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    var spreadingScript: overgrownNanoforgeJunkSpreadingScript? = null

    enum class advancementAlteration() {
        TOO_MANY_STRUCTURES() {
            override fun getText(nanoforge: overgrownNanoforgeIndustry): String {
                val market = nanoforge.market
                return "${market.name} has exceeded the structure limit by ${market.getVisibleIndustries().size - niko_MPC_marketUtils.maxStructureAmount}"
            }
            override fun shouldApply(nanoforge: overgrownNanoforgeIndustry): Boolean {
                val spreadingScript = nanoforge.junkSpreader.spreadingScript ?: return false
                return spreadingScript.marketExceedsMaxStructuresAndDoWeCare()
            }
            override fun getMult(nanoforge: overgrownNanoforgeIndustry): Float = -1f //halt all growth but not recession
        };


        abstract fun getText(nanoforge: overgrownNanoforgeIndustry): String
        abstract fun shouldApply(nanoforge: overgrownNanoforgeIndustry): Boolean
        open fun getMult(nanoforge: overgrownNanoforgeIndustry): Float = 0f
        open fun getPositiveIncrement(nanoforge: overgrownNanoforgeIndustry): Float = 0f
        open fun getNegativeMult(nanoforge: overgrownNanoforgeIndustry): Float = 0f
        open fun getNegativeIncrement(nanoforge: overgrownNanoforgeIndustry): Float = 0f


        companion object {
            fun getReasons(nanoforge: overgrownNanoforgeIndustry): MutableSet<advancementAlteration> {
                val reasons = HashSet<advancementAlteration>()
                for (reason in advancementAlteration.values().toMutableList()) {
                    if (reason.shouldApply(nanoforge)) reasons += reason
                }
                return reasons
            }
        }
    }

    val advancementAlterations: MutableSet<advancementAlteration> = HashSet()

    fun updateAdvancementAlterations() {
        advancementAlterations.clear()
        for (entry in advancementAlteration.values().toSet()) if (entry.shouldApply(nanoforge)) advancementAlterations += entry
    }

    class spreadTimer(minInterval: Float, maxInterval: Float): IntervalUtil(minInterval, maxInterval) {
        override fun advance(amount: Float) {
            super.advance(amount)
            if (intervalElapsed()) {
                setInterval(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
            }
        }
    }

    fun spreadJunkIfPossible(amount: Float) {
        val dayAmount = Misc.getDays(amount)
        if (shouldSpreadJunk()) {
            tryToSpreadJunk(dayAmount) //only increment the timer if we should even try
        }
    }

    fun shouldSpreadJunk(): Boolean {
        if (spreadingSuppressed() || getMarket().exceedsMaxStructures() || spreadingScript != null) {
            timeTilSpread.elapsed = 0f
            return false
        }
        return true
    }

    fun spreadingSuppressed(): Boolean {
        return (getOverallSpreadProgressPerDay() <= 0f)
    }

    fun tryToSpreadJunk(dayAmount: Float) {
        timeTilSpread.advance(dayAmount)
        if (timeTilSpread.intervalElapsed()) spreadJunk()
    }

    fun spreadJunk(): overgrownNanoforgeJunkSpreadingScript? {
        val script = createSpreadingScript()
        script?.start()
        return script
    }


    fun getOverallSpreadProgressPerDay(): Float {
        return getOverallSpreadProgress(1f)
    }

    fun getOverallSpreadProgress(days: Float = 1f): Float {
        val increment = getProgress(days)
        val decrement = getRegression(days)
        return (increment - decrement)
    }

    fun getProgress(days: Float = 1f): Float {
        return (getPositiveIncrement()*days) * (days * getMult())
    }
    fun getRegression(days: Float = 1f): Float {
        return ((getNegativeIncrement()*days) * getNegativeMult())
    }
    fun getMult(): Float {
        var mult = 1f
        for (entry in advancementAlterations) mult += entry.getMult(nanoforge)
        return mult
    }
    fun getPositiveIncrement(): Float {
        var increment = 0f
        for (entry in advancementAlterations) increment += entry.getPositiveIncrement(nanoforge)
        return increment
    }
    fun getNegativeIncrement(): Float {
        var decrement = 0f
        for (entry in advancementAlterations) decrement += entry.getNegativeIncrement(nanoforge)
        return decrement
    }
    fun getNegativeMult(): Float {
        var negativeMult = 1f
        for (entry in advancementAlterations) negativeMult += entry.getNegativeMult(nanoforge)
        return negativeMult
    }

    private fun createSpreadingScript(): overgrownNanoforgeJunkSpreadingScript? {
        val minDays = getMinSpreadingDays()
        val maxDays = getMaxSpreadingDays()
        val timeTilSpread = MathUtils.getRandomNumberInRange(minDays, maxDays)

        val sourceParams = generateSourceParams() ?: return null

        spreadingScript = createSpreadingScriptInstance(timeTilSpread, sourceParams)
        return spreadingScript
    }

    private fun getSourceType(): overgrownNanoforgeSourceTypes? {
        val picker = WeightedRandomPicker<overgrownNanoforgeSourceTypes>()
        picker.addAll(getAdjustedTypeChances())

        return picker.pick()
    }

    fun getMinSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS
    }

    fun getMaxSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS
    }

    fun generateSourceParams(): overgrownNanoforgeRandomizedSourceParams? {
        val chosenSourceType = getSourceType() ?: return null
        return overgrownNanoforgeRandomizedSourceParams(nanoforge, chosenSourceType)
    }

    fun createSpreadingScriptInstance(
        timeTilSpread: Float,
        sourceParams: overgrownNanoforgeRandomizedSourceParams,
    ): overgrownNanoforgeJunkSpreadingScript {
        return overgrownNanoforgeJunkSpreadingScript(nanoforge, timeTilSpread, this, sourceParams)
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

    fun getMarket(): MarketAPI {
        return nanoforge.market
    }

    fun delete() {
        TODO()
    }

    fun getExistingJunk(): MutableSet<overgrownNanoforgeJunk> {
        return nanoforge.junk
    }
}

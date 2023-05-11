package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.spreading

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.utilities.niko_MPC_marketUtils
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_USE_JUNK_STRUCTURES
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeJunkSpreader(
    val nanoforgeHandler: overgrownNanoforgeIndustryHandler
) {

    val timeTilSpread = spreadTimer(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    var spreadingScript: overgrownNanoforgeJunkSpreadingScript? = null
    val baseSpreadRate = 1f

    enum class advancementAlteration() {
        TOO_MANY_STRUCTURES() {
            override fun getText(nanoforgeHandler: overgrownNanoforgeIndustryHandler): String {
                val market = nanoforgeHandler.market
                return "${market.name} has exceeded the structure limit by ${market.getVisibleIndustries().size - niko_MPC_marketUtils.maxStructureAmount}"
            }
            override fun shouldApply(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Boolean {
                val spreadingScript = nanoforgeHandler.junkSpreader.spreadingScript ?: return false
                return spreadingScript.marketExceedsMaxStructuresAndDoWeCare()
            }
            override fun getMult(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Float = -1f //halt all growth but not recession
        };


        abstract fun getText(nanoforgeHandler: overgrownNanoforgeIndustryHandler): String
        abstract fun shouldApply(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Boolean
        open fun getMult(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Float = 0f // think this is more of a mult increment?
        open fun getPositiveIncrement(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Float = 0f
        open fun getNegativeMult(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Float = 0f
        open fun getNegativeIncrement(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Float = 0f


        companion object {
            val alterations = advancementAlteration.values().toSet()
            fun getReasons(nanoforgeHandler: overgrownNanoforgeIndustryHandler): MutableSet<advancementAlteration> {
                val reasons = HashSet<advancementAlteration>()
                for (reason in advancementAlteration.values().toMutableList()) {
                    if (reason.shouldApply(nanoforgeHandler)) reasons += reason
                }
                return reasons
            }
        }
    }

    val advancementAlterations: MutableSet<advancementAlteration> = HashSet()

    fun updateAdvancementAlterations() {
        advancementAlterations.clear()
        for (entry in advancementAlteration.alterations) if (entry.shouldApply(nanoforgeHandler)) advancementAlterations += entry
    }

    class spreadTimer(minInterval: Float, maxInterval: Float): IntervalUtil(minInterval, maxInterval) {
    }

    fun advance(amount: Float) {
        val dayAmount = Misc.getDays(amount)
        if (shouldSpreadJunk()) {
            tryToSpreadJunk(dayAmount) //only increment the timer if we should even try
        }
    }

    fun shouldSpreadJunk(): Boolean {
        if (spreadingScript != null || spreadingSuppressed() || getMarket().exceedsMaxStructures()) {
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
        if (timeTilSpread.intervalElapsed()) {
            Global.getSector().campaignUI.addMessage("spreaded!")
            //spreadJunk()
        }
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
        for (entry in advancementAlterations) mult += entry.getMult(nanoforgeHandler)
        return mult
    }
    fun getPositiveIncrement(): Float {
        var increment = baseSpreadRate
        for (entry in advancementAlterations) increment += entry.getPositiveIncrement(nanoforgeHandler)
        return increment
    }
    fun getNegativeIncrement(): Float {
        var decrement = 0f
        for (entry in advancementAlterations) decrement += entry.getNegativeIncrement(nanoforgeHandler)
        return decrement
    }
    fun getNegativeMult(): Float {
        var negativeMult = 1f
        for (entry in advancementAlterations) negativeMult += entry.getNegativeMult(nanoforgeHandler)
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
        return overgrownNanoforgeSourceTypes.adjustedPick()
    }

    fun getMinSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS
    }

    fun getMaxSpreadingDays(): Float {
        return OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS
    }

    fun generateSourceParams(): overgrownNanoforgeRandomizedSourceParams? {
        val chosenSourceType = getSourceType() ?: return null
        return overgrownNanoforgeRandomizedSourceParams(nanoforgeHandler, chosenSourceType)
    }

    fun createSpreadingScriptInstance(
        timeTilSpread: Float,
        sourceParams: overgrownNanoforgeRandomizedSourceParams,
    ): overgrownNanoforgeJunkSpreadingScript {
        return overgrownNanoforgeJunkSpreadingScript(nanoforgeHandler, timeTilSpread, this, sourceParams)
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
                overgrownNanoforgeSourceTypes.INTERNAL -> {
                    iterator.remove() // TODO return to this
                }
            }
        }
        return types
    }

    fun getMarket(): MarketAPI {
        return nanoforgeHandler.market
    }

    fun delete() {
        TODO()
    }

    fun getExistingJunk(): MutableSet<overgrownNanoforgeJunkHandler> {
        return nanoforgeHandler.junkHandlers
    }
}

/*package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.spreading


import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_marketUtils
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_SPREADING_DAYS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_SPREADING_DAYS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS
import org.lazywizard.lazylib.MathUtils

class overgrownNanoforgeJunkSpreader(
    val nanoforgeHandler: overgrownNanoforgeIndustryHandler
) {

    var deleted: Boolean = false
    val spreadTimer = IntervalUtil(OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)
    var spreadingScript: overgrownNanoforgeJunkSpreadingScript? = null
    val baseSpreadRate = 1f

    enum class advancementAlteration() {
        TOO_MANY_STRUCTURES() {
            override fun getText(nanoforgeHandler: overgrownNanoforgeIndustryHandler): String {
                val market = nanoforgeHandler.market
                return "${market.name} has exceeded the structure limit by ${market.getVisibleIndustries().size - niko_MPC_marketUtils.maxStructureAmount}"
            }
            override fun shouldApply(nanoforgeHandler: overgrownNanoforgeIndustryHandler): Boolean {
                return false
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

    fun advance(amount: Float) {
        if (deleted) {
            displayError("junk spreader on ${getMarket()?.name} advance called while deleted")
            return
        }
        if (!shouldSpreadJunk()) {
            return
        }
        val dayAmount = Misc.getDays(amount)
        spreadTimer.advance(dayAmount)
        if (spreadTimer.intervalElapsed()) {
            tryToSpreadJunk(dayAmount)
        }
    }

    fun shouldSpreadJunk(): Boolean {
        if (spreadingScript != null || spreadingSuppressed() || getMarket().exceedsMaxStructures()) {
            return false
        }
        return true
    }

    fun spreadingSuppressed(): Boolean {
        return (getOverallSpreadProgressPerDay() <= 0f)
    }

    fun tryToSpreadJunk(dayAmount: Float) {
        spreadTimer.advance(dayAmount)
        if (spreadTimer.intervalElapsed()) {
            Global.getSector().campaignUI.addMessage("spreaded!")
            //spreadJunk()
        }
    }

    fun spreadJunk(): overgrownNanoforgeJunkSpreadingScript? {
        val script = createSpreadingScript()
        script?.start()
        if (script != null) {
            nanoforgeHandler.spreadingStarted(script)
        }
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
        return (getPositiveIncrement()*days) * (days * getMultIncrement())
    }
    fun getRegression(days: Float = 1f): Float {
        return ((getNegativeIncrement()*days) * getNegativeMultIncrement())
    }
    fun getMultIncrement(): Float {
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
    fun getNegativeMultIncrement(): Float {
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

    fun getMarket(): MarketAPI {
        return nanoforgeHandler.market
    }

    fun delete() {
        spreadingScript?.delete()
        deleted = true
    }

    fun getExistingJunk(): MutableSet<overgrownNanoforgeJunkHandler> {
        return nanoforgeHandler.junkHandlers
    }
}

******/
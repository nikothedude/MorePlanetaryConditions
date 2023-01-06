package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.getVisibleIndustries
import data.utilities.niko_MPC_marketUtils.maxStructureAmount

class overgrownNanoforgeJunkSpreadingScript(
    val nanoforge: overgrownNanoforgeIndustry,
    val daysTilSpread: Float,
    val params: constructionParams
): niko_MPC_baseNikoScript() {

    class constructionParams(
        val target: Industry?,
        val effectParams: overgrownNanoforgeRandomizedSource.sourceParams
    )

    enum class advancementAlteration() {
        TOO_MANY_STRUCTURES() {
            override fun getText(script: overgrownNanoforgeJunkSpreadingScript): String {
                val market = script.getMarket()
                return "${market.name} has exceeded the structure limit by ${market.getVisibleIndustries().size - niko_MPC_marketUtils.maxStructureAmount}"
            }
            override fun shouldApply(script: overgrownNanoforgeJunkSpreadingScript): Boolean = script.marketExceedsMaxStructuresAndDoWeCare()
            override fun getMult(script: overgrownNanoforgeJunkSpreadingScript): Float = -1f //halt all growth but not recession
        };


        abstract fun getText(script: overgrownNanoforgeJunkSpreadingScript): String
        abstract fun shouldApply(script: overgrownNanoforgeJunkSpreadingScript): Boolean
        open fun getMult(script: overgrownNanoforgeJunkSpreadingScript): Float = 0f
        open fun getPositiveIncrement(script: overgrownNanoforgeJunkSpreadingScript): Float = 0f
        open fun getNegativeMult(script: overgrownNanoforgeJunkSpreadingScript): Float = 0f
        open fun getNegativeIncrement(script: overgrownNanoforgeJunkSpreadingScript): Float = 0f


        companion object {
            fun getReasons(script: overgrownNanoforgeJunkSpreadingScript): MutableSet<advancementAlteration> {
                val reasons = HashSet<advancementAlteration>()
                for (reason in advancementAlteration.values().toMutableList()) {
                    if (reason.shouldApply(script)) reasons += reason
                }
                return reasons
            }
        }
    }

    companion object {
        fun createParams(target: Industry?, effectParams: overgrownNanoforgeRandomizedSource.sourceParams): constructionParams {
            return constructionParams(target, effectParams)
        }
    }
    val advancementMult = 1f
    var pausedReasons: MutableSet<advancementAlteration> = HashSet()

    class junkSpreadScriptTimer(minInterval: Float, maxInterval: Float, val script: overgrownNanoforgeJunkSpreadingScript): IntervalUtil(minInterval, maxInterval) {
        val advancementAlterations = HashSet<advancementAlteration>()
        val baseRate = 1f

        override fun advance(amount: Float) {
            val days = Misc.getDays(amount)
            updateAdvancementAlterations()
            val finalAmount = getAdvancement(days)
            super.advance(finalAmount)

            if (intervalElapsed()) {
                script.spreadJunk()
            } else if (areWeReverted()) { // negative means it was reverted
                revert()
            }
        }

        fun getAdvancement(days: Float = 1f): Float {
            val increment = getIncrement(days)
            val decrement = getDecrement(days)
            return (increment - decrement)
        }

        private fun revert() {
            script.culled()
        }
        private fun areWeReverted(): Boolean {
            return elapsed < 0
        }
        fun getIncrement(days: Float = 1f): Float {
            return (getPositiveIncrement()*days) * (days * getMult())
        }
        fun getDecrement(days: Float = 1f): Float {
            return ((getNegativeIncrement()*days) * getNegativeMult())
        }
        fun getMult(): Float {
            var mult = 1f
            for (entry in advancementAlterations) mult += entry.getMult(script)
            return mult
        }
        fun getPositiveIncrement(): Float {
            var increment = 0f
            for (entry in advancementAlterations) increment += entry.getPositiveIncrement(script)
            return increment
        }
        fun getNegativeIncrement(): Float {
            var decrement = 0f
            for (entry in advancementAlterations) decrement += entry.getNegativeIncrement(script)
            return decrement
        }
        private fun getNegativeMult(): Float {
            var negativeMult = 1f
            for (entry in advancementAlterations) negativeMult += entry.getNegativeMult(script)
            return negativeMult
        }
        fun updateAdvancementAlterations() {
            advancementAlterations.clear()
            for (entry in advancementAlteration.values().toSet()) if (entry.shouldApply(script)) advancementAlterations += entry
        }
    }
    val timer = junkSpreadScriptTimer(daysTilSpread, daysTilSpread, this)

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        timer.advance(amount)
        TODO("Not yet implemented")
    }

    private fun culled() {
        TODO("Not yet implemented")
    }

    fun spreadJunk(): overgrownNanoforgeRandomizedSource? {
        if (marketExceedsMaxStructuresAndDoWeCare()) { //a last resort
            delete()
            return null
        }

        val source = createSource()
        source.init()

        delete()
        return source
    }

    fun marketExceedsMaxStructuresAndDoWeCare(): Boolean {
        return (params.effectParams.type == overgrownNanoforgeSourceTypes.STRUCTURE && getMarket().exceedsMaxStructures())
    }

    private fun createSource(): overgrownNanoforgeRandomizedSource {
        return overgrownNanoforgeRandomizedSource(nanoforge = this.nanoforge, params = params.effectParams)
    }

    fun getMarket(): MarketAPI {
        return nanoforge.market
    }

    override fun delete(): Boolean {
        return super.delete()
    }

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }
}

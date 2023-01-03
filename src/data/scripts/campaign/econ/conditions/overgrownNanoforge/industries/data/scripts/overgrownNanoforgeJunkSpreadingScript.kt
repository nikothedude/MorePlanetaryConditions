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
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.maxStructureAmount

class overgrownNanoforgeJunkSpreadingScript(
    val nanoforge: overgrownNanoforgeIndustry,
    val daysTilSpread: Float,
    val params: constructionParams
): niko_MPC_baseNikoScript() {

    enum class pauseReasons {
        TOO_MANY_STRUCTURES {
            override fun getText(script: overgrownNanoforgeJunkSpreadingScript): String {
                val market = script.getMarket()
                return "${market.name} has exceeded the structure limit by ${market.industries.size - maxStructureAmount}"
            }
        };

        abstract fun getText(script: overgrownNanoforgeJunkSpreadingScript): String

        companion object {
            fun getReasons(script: overgrownNanoforgeJunkSpreadingScript): MutableSet<pauseReasons> {
                val reasons = HashSet<pauseReasons>()
                if (script.marketExceedsMaxStructuresAndDoWeCare()) reasons += TOO_MANY_STRUCTURES
                return reasons
            }
        }
    }

    class constructionParams(
        val target: Industry?,
        val effectParams: overgrownNanoforgeRandomizedSource.sourceParams
    )

    companion object {
        fun createParams(target: Industry?, effectParams: overgrownNanoforgeRandomizedSource.sourceParams): constructionParams {
            return constructionParams(target, effectParams)
        }
    }
    var paused: Boolean = false
    var pausedReasons: MutableSet<pauseReasons> = HashSet()

    val timer = IntervalUtil(daysTilSpread, daysTilSpread)

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        updatePausedStatus()
        val convertedAmount = (Misc.getDays(amount))*getAdvancementSpeedMult()

        timer.advance(convertedAmount)
        if (timer.intervalElapsed()) {
            spreadJunk()
            return
        } else if (timer.elapsed > daysTilSpread) { // reverted
            culled()
            return
        }
        TODO("Not yet implemented")
    }

    private fun culled() {
        TODO("Not yet implemented")
    }

    /** Can return negatives to revert progress */
    fun getAdvancementSpeedMult(): Float {
        var mult = 1f
        if (paused) mult = 0f

        return mult
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

    fun updatePausedStatus(): Boolean {
        updatePausedReasons()
        paused = shouldPause()
        return paused
    }

    fun updatePausedReasons(): MutableSet<pauseReasons> {
        this.pausedReasons = pauseReasons.getReasons(this)
        return pausedReasons
    }

    fun shouldPause(): Boolean {
        return (pausedReasons.isNotEmpty())
    }

    private fun marketExceedsMaxStructuresAndDoWeCare(): Boolean {
        return  (params.effectParams.type == overgrownNanoforgeSourceTypes.STRUCTURE && getMarket().exceedsMaxStructures())
    }

    private fun createSource(): overgrownNanoforgeRandomizedSource {
        return overgrownNanoforgeRandomizedSource(nanoforge = this.nanoforge, id = this, params = params.effectParams)
    }

    private fun getMarket(): MarketAPI {
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

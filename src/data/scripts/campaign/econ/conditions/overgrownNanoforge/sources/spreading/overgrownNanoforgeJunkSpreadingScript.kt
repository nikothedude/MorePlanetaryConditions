package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.spreading

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.hasMaxStructures
import data.utilities.niko_MPC_marketUtils.isApplied
import data.utilities.niko_MPC_marketUtils.isJunkStructure
import data.utilities.niko_MPC_marketUtils.isOrbital
import data.utilities.niko_MPC_settings

class overgrownNanoforgeJunkSpreadingScript(
    val nanoforgeHandler: overgrownNanoforgeIndustryHandler,
    val daysTilSpread: Float,
    val spreader: overgrownNanoforgeJunkSpreader = nanoforgeHandler.junkSpreader,
    val effectParams: overgrownNanoforgeRandomizedSourceParams,
): niko_MPC_baseNikoScript() {
    var target: Industry? = getTargetForJunk()
    fun getType(): overgrownNanoforgeSourceTypes {
        return effectParams.type
    }

    class junkSpreadScriptTimer(minInterval: Float, maxInterval: Float, val script: overgrownNanoforgeJunkSpreadingScript): IntervalUtil(minInterval, maxInterval) {
        override fun advance(amount: Float) {
            val days = Misc.getDays(amount)
            updateAdvancementAlterations()
            val finalAmount = getAdvancement(days)
            super.advance(finalAmount)
        }

        fun getAdvancement(days: Float = 1f): Float {
            return script.spreader.getOverallSpreadProgress(days)
        }

        fun updateAdvancementAlterations() {
            script.spreader.updateAdvancementAlterations()
        }
    }
    val timer = junkSpreadScriptTimer(daysTilSpread, daysTilSpread, this)

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        updateTarget()
        timer.advance(amount)
        if (timer.intervalElapsed()) {
            spreadJunk()
        } else if (areWeReverted()) { // negative means it was reverted
            culled()
        }
        // TODO: expand more?
    }

    fun areWeReverted(): Boolean {
        return (timer.elapsed < 0f)
    }

    private fun culled() {
        TODO("Not yet implemented")
    }

    fun updateTarget() {
        if (!shouldTarget()) {
            target = null
            return
        }
        if (target == null || !target!!.isValidTarget()) target = getTargetForJunk()
    }

    fun Industry.isValidTarget(): Boolean {
        return (isApplied() && !isJunkStructure())
    }

    fun getTargettingChance(structure: Industry): Float {
        var score = niko_MPC_settings.overgrownNanoforgeBaseJunkSpreadTargettingChance
        if (structure.isImproved) score *= 0.8f
        if (structure.isOrbital()) score *= 0.2f
        return score
    }

    fun getTargetForJunk(): Industry? {
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

    fun shouldTarget(): Boolean {
        if (getType() != overgrownNanoforgeSourceTypes.STRUCTURE) return false
        if (getMarket().hasMaxStructures()) return false

        return true
    }

    fun spreadJunk(): overgrownNanoforgeRandomizedSource? {
        if (marketExceedsMaxStructuresAndDoWeCare()) { //a last resort
            delete()
            return null
        }

        val source = createSource()
        source.init()

        reportJunkSpreaded()

        delete()
        return source
    }

    private fun reportJunkSpreaded() {
        TODO("Not yet implemented")
    }

    fun marketExceedsMaxStructuresAndDoWeCare(): Boolean {
        return (getType() == overgrownNanoforgeSourceTypes.STRUCTURE && getMarket().exceedsMaxStructures())
    }

    private fun createSource(): overgrownNanoforgeRandomizedSource {
        return overgrownNanoforgeRandomizedSource(nanoforgeHandler, effectParams)
    }

    fun getMarket(): MarketAPI {
        return nanoforgeHandler.market
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

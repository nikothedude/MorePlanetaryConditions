package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeCondition.Companion.DURATION
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeCondition.Companion.getAilmar
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import org.magiclib.kotlin.getFactionMarkets

class MPC_knightsDownedCondition: niko_MPC_baseNikoCondition() {
    companion object {
        const val FLEETSIZE_MULT = 0.3f
        const val STABILITY_MULT = 1f
        const val ACCESS_INCR = 0.2f

        fun isValid(market: MarketAPI) = market.factionId == Factions.LUDDIC_CHURCH
    }

    override fun apply(id: String) {
        super.apply(id)

        if (!showIcon()) return

        if (isValid(market)) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, FLEETSIZE_MULT, name)
            market.accessibilityMod.modifyFlat(id, ACCESS_INCR, name)
            market.stability.modifyFlat(id, STABILITY_MULT, name)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
        market.accessibilityMod.unmodify(id)
        market.stability.unmodify(id)
    }

    override fun showIcon(): Boolean {
        return isValid(market)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (isValid(market)) {
            tooltip.addPara("Fleet size decreased by %s", 5f, Misc.getHighlightColor(), "${(FLEETSIZE_MULT).trimHangingZero()}")
            tooltip.addPara("Stability increased by %s", 5f, Misc.getHighlightColor(), "${STABILITY_MULT.toInt()}")
            tooltip.addPara("Accessibility increased by %s", 5f, Misc.getHighlightColor(), "${ACCESS_INCR.trimHangingZero()}%")

            tooltip.addPara("Days remaining: %s", 10f, Misc.getHighlightColor(), "${MPC_knightsDownedScript.getDaysLeft()}")
        }
    }
    class MPC_knightsDownedScript: niko_MPC_baseNikoScript() {

        companion object {
            fun getDaysLeft(): Number {
                val script = getScript() ?: return 0f
                return (script.interval.intervalDuration - script.interval.elapsed).roundNumTo(1).trimHangingZero()
            }

            fun getScript(): MPC_knightsDownedScript? = Global.getSector().scripts.firstOrNull { it is MPC_knightsDownedScript } as? MPC_knightsDownedScript?
            const val DURATION = 710f
        }

        val interval = IntervalUtil(DURATION, DURATION)
        val checkInterval = IntervalUtil(1f, 1.1f)

        override fun startImpl() {
            Global.getSector().addScript(this)
            addConditions()
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
            removeConditions()
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            interval.advance(Misc.getDays(amount))
            if (interval.intervalElapsed()) {
                delete()
                return
            }
            checkInterval.advance(Misc.getDays(amount))
            if (checkInterval.intervalElapsed()) {
                removeConditions()
                addConditions()
            }
        }

        private fun addConditions() {
            for (market in Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getFactionMarkets()) {
                market.addConditionIfNotPresent("MPC_knightsDownedCondition")
            }
        }

        private fun removeConditions() {
            for (market in Global.getSector().economy.marketsCopy) {
                market.removeCondition("MPC_knightsDownedCondition")
            }
        }
    }

}
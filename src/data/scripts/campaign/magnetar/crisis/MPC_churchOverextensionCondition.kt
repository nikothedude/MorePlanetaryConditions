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
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeScript.Companion.getDaysLeft
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import org.magiclib.kotlin.getFactionMarkets

class MPC_churchOverextensionCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val FLEETSIZE_MULT = 0.9f

        fun isValid(market: MarketAPI) = market.factionId == Factions.LUDDIC_CHURCH
    }

    override fun apply(id: String) {
        super.apply(id)

        if (!showIcon()) return

        if (isValid(market)) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, FLEETSIZE_MULT, name)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
    }

    override fun showIcon(): Boolean {
        return isValid(market)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (isValid(market)) {
            tooltip.addPara("Fleet size decreased by %s", 5f, Misc.getHighlightColor(), "${(FLEETSIZE_MULT).trimHangingZero()}")
        }
    }

    override fun isTransient(): Boolean = false

    class MPC_churchOverextensionScript: niko_MPC_baseNikoScript() {
        companion object {
            fun getScript(): MPC_churchOverextensionScript? = Global.getSector().scripts.firstOrNull { it is MPC_churchOverextensionScript } as? MPC_churchOverextensionScript?
        }

        val checkInterval = IntervalUtil(1f, 1.1f)

        override fun startImpl() {
            Global.getSector().addScript(this)
            removeConditions()
            addConditions()
        }

        override fun stopImpl() {
            Global.getSector().removeScript(this)
            removeConditions()
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            checkInterval.advance(Misc.getDays(amount))
            if (checkInterval.intervalElapsed()) {
                addConditions()
            }
        }

        private fun addConditions() {
            for (market in Global.getSector().getFaction(Factions.LUDDIC_CHURCH).getFactionMarkets()) {
                market.addConditionIfNotPresent("MPC_IAIICLCOverextension")
            }
        }

        private fun removeConditions() {
            for (market in Global.getSector().economy.marketsCopy) {
                market.removeCondition("MPC_IAIICLCOverextension")
            }
        }
    }

}
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
import data.scripts.campaign.magnetar.crisis.intel.church.MPC_churchContributionIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import data.utilities.niko_MPC_stringUtils
import org.magiclib.kotlin.getFactionMarkets

class MPC_churchOverextensionCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val FLEETSIZE_MULT = 0.7f
        const val UNREST_STAB_MALUS = 2f
        const val MARTIAL_LAW_INCOME_MULT = 0.75f

        fun isValid(market: MarketAPI) = market.factionId == Factions.LUDDIC_CHURCH
    }

    override fun apply(id: String) {
        super.apply(id)

        if (!showIcon()) return

        if (isValid(market)) {
            val intel = MPC_churchContributionIntel.get() ?: return
            if (intel.unrestActive) {
                if (intel.martialLawEnacted) {
                    market.incomeMult.modifyFlat(id, 1f - MARTIAL_LAW_INCOME_MULT, "Martial Law")
                } else {
                    market.stability.modifyFlat(id, -UNREST_STAB_MALUS, "Knightly Unrest")
                }
            }
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
            tooltip.addPara("Fleet size decreased by %s", 5f, Misc.getHighlightColor(), "${(FLEETSIZE_MULT).trimHangingZero()}x")
            val intel = MPC_churchContributionIntel.get() ?: return
            if (intel.unrestActive) {
                if (intel.martialLawEnacted) {
                    tooltip.addPara(
                        "Unrest has mounted, but %s prevents any stability loss, at the cost of %s colony income.",
                        5f,
                        Misc.getHighlightColor(),
                        niko_MPC_stringUtils.toPercent(MARTIAL_LAW_INCOME_MULT)
                    )
                } else {
                    tooltip.addPara(
                        "Unrest has grown against the Knights of Ludd from within, reducing stability by %s",
                        5f,
                        Misc.getHighlightColor(),
                        "${UNREST_STAB_MALUS.toInt()}"
                    )
                }
            }
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
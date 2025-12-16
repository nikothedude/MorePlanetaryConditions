package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyUnrestScript.Companion.HEGEMONY_UNREST_LEVEL
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import lunalib.lunaExtensions.getMarketsCopy

class MPC_indieUnrestScript: niko_MPC_baseNikoScript() {

    companion object {
        const val INDIE_UNREST_LEVEL = "\$MPC_indieUnrestLevelFloat"

        fun getUnrestLevel(): Float = Global.getSector().memoryWithoutUpdate.getFloat(INDIE_UNREST_LEVEL)
    }

    val daysTilDecay = 40f
    val daysToEnd = 90f
    val daysAfterDecayToEnd = (daysToEnd - daysTilDecay)
    val checkInterval = IntervalUtil(0.3f, 0.4f)

    val marketsAffecting = HashSet<MarketAPI>()

    var daysElapsed = 0f

    override fun startImpl() {
        Global.getSector().addScript(this)
        refreshConditions()
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)

        marketsAffecting.forEach { it.removeCondition("MPC_indieUnrest") }
        marketsAffecting.clear()
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        daysElapsed += days

        if (daysElapsed >= daysToEnd) {
            delete()
            return
        }

        checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {
            refreshConditions()
        }

        val newUnrest = ((daysElapsed - daysTilDecay) / daysAfterDecayToEnd).coerceAtMost(1f).coerceAtLeast(0f)
        Global.getSector().memoryWithoutUpdate[INDIE_UNREST_LEVEL] = 1 - newUnrest
    }

    fun refreshConditions() {
        for (market in marketsAffecting.toMutableSet()) {
            if (!canAffectMarket(market)) {
                market.removeCondition("MPC_indieUnrest")
                marketsAffecting -= market
            }
        }

        for (market in Global.getSector().getFaction(Factions.PLAYER).getMarketsCopy()) {
            if (!canAffectMarket(market)) continue
            market.addConditionIfNotPresent("MPC_indieUnrest")
            marketsAffecting += market
        }
    }

    fun canAffectMarket(market: MarketAPI): Boolean {
        return market.faction.id == Factions.PLAYER
    }

    override fun runWhilePaused(): Boolean = false
}
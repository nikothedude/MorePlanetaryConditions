package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import lunalib.lunaExtensions.getMarketsCopy

class MPC_hegemonyUnrestScript: niko_MPC_baseNikoScript() {

    companion object {
        const val HEGEMONY_UNREST_LEVEL = "\$MPC_hegeUnrestLevelFloat"

        fun getUnrestLevel(): Float = Global.getSector().memoryWithoutUpdate.getFloat(HEGEMONY_UNREST_LEVEL)
    }

    val daysToMax = 60f
    val daysToEnd = 70f
    val checkInterval = IntervalUtil(0.3f, 0.4f)

    val marketsAffecting = HashSet<MarketAPI>()

    var daysElapsed = 0f

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)

        marketsAffecting.forEach { it.removeCondition("MPC_hegeUnrest") }
        marketsAffecting.clear()
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        daysElapsed += days

        if (daysElapsed >= daysToEnd) {
            end()
            delete()
            return
        }

        checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {
            refreshConditions()
        }

        val newUnrest = (daysElapsed / daysToMax).coerceAtMost(1f)
        Global.getSector().memoryWithoutUpdate[HEGEMONY_UNREST_LEVEL] = newUnrest
    }

    private fun refreshConditions() {
        for (market in marketsAffecting.toMutableSet()) {
            if (!canAffectMarket(market)) {
                market.removeCondition("MPC_hegeUnrest")
                marketsAffecting -= market
            }
        }

        for (market in Global.getSector().getFaction(Factions.HEGEMONY).getMarketsCopy()) {
            if (!canAffectMarket(market)) continue
            market.addConditionIfNotPresent("MPC_hegeUnrest")
            marketsAffecting += market
        }
    }

    fun canAffectMarket(market: MarketAPI): Boolean {
        if (market.faction.id != Factions.HEGEMONY) return false

        return true
    }

    fun end() {
        Global.getSector().memoryWithoutUpdate[HEGEMONY_UNREST_LEVEL] = 0f
        val fobIntel = MPC_IAIICFobIntel.get() ?: return
        val hegeContrib = fobIntel.getContributionById(Factions.HEGEMONY) ?: return
        fobIntel.removeContribution(hegeContrib, false)
    }
}
package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import lunalib.lunaExtensions.getMarketsCopy

class MPC_hegemonyUnrestScript: niko_MPC_baseNikoScript() {

    companion object {
        const val HEGEMONY_UNREST_LEVEL = "\$MPC_hegeUnrestLevelFloat"

        fun getUnrestLevel(): Float = Global.getSector().memoryWithoutUpdate.getFloat(HEGEMONY_UNREST_LEVEL)
    }

    val daysToMax = 40f
    val daysToEnd = 50f
    val checkInterval = IntervalUtil(0.3f, 0.4f)

    val marketsAffecting = HashSet<MarketAPI>()

    var daysElapsed = 0f

    override fun startImpl() {
        Global.getSector().addScript(this)
        refreshConditions()
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

    fun refreshConditions() {
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
        return market.faction.id == Factions.HEGEMONY
    }

    fun end() {
        Global.getSector().memoryWithoutUpdate[HEGEMONY_UNREST_LEVEL] = 0f
        val fobIntel = MPC_IAIICFobIntel.get() ?: return
        val hegeContrib = fobIntel.getContributionById(Factions.HEGEMONY) ?: return
        fobIntel.removeContribution(hegeContrib, false)

        val contribIntel = MPC_hegemonyContributionIntel.get(false) ?: return
        contribIntel.state = MPC_hegemonyContributionIntel.State.DONE
        contribIntel.sendUpdateIfPlayerHasIntel(contribIntel.state, false)
        contribIntel.endAfterDelay()

        //Global.getSector().importantPeople.getPerson(People.DAUD).makeImportant(IAIIC_QUEST)
        //Global.getSector().memoryWithoutUpdate["\$MPC_daudWaitingToChastisePlayer"] = true

        class DelayedContactScript: MPC_delayedExecutionNonLambda(
            IntervalUtil(5f, 5.1f),
            true,
            false
        ) {
            override fun execute() {
                if (Global.getSector().intelManager.isPlayerInRangeOfCommRelay) {
                    super.execute()
                }
            }

            override fun executeImpl() {
                val plugin = RuleBasedInteractionDialogPluginImpl("MPC_IAIICDaudAngryCommlinkInit")
                Global.getSector().campaignUI.showInteractionDialog(plugin, Global.getSector().playerFleet)
            }
        }

        //DelayedContactScript().start() // i jsut realized, daud would never really ever call you for this. he has better shit to do
    }
}
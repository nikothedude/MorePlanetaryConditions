package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeCondition.Companion.DURATION
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeCondition.Companion.getAilmar
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_mathUtils.roundNumTo
import org.magiclib.kotlin.getFactionMarkets
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_ailmarFleetsizeScript: niko_MPC_baseNikoScript() {

    companion object {
        fun getDaysLeft(): Number {
            val script = getScript() ?: return 0f
            return (script.interval.intervalDuration - script.interval.elapsed).roundNumTo(1).trimHangingZero()
        }

        fun getScript(): MPC_ailmarFleetsizeScript? = Global.getSector().scripts.firstOrNull { it is MPC_ailmarFleetsizeScript } as? MPC_ailmarFleetsizeScript?
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
            addConditions()
        }
    }

    private fun addConditions() {
        for (market in Global.getSector().getFaction(Factions.PLAYER).getFactionMarkets()) {
            market.removeCondition("MPC_IAIICDockyardsDonated")
            market.addConditionIfNotPresent("MPC_IAIICDockyardsDonated")
        }

        getAilmar()?.removeCondition("MPC_IAIICDockyardsDonated")
        getAilmar()?.addConditionIfNotPresent("MPC_IAIICDockyardsDonated")
    }

    private fun removeConditions() {
        for (market in Global.getSector().economy.marketsCopy) {
            market.removeCondition("MPC_IAIICDockyardsDonated")
        }
    }
}
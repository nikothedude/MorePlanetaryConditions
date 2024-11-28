package data.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_delayedExecution(
    val execute: () -> Unit,
    delay: Float,
    val runWhilePaused: Boolean = false,
    private val useDays: Boolean = false
): niko_MPC_baseNikoScript() {
    val interval: IntervalUtil = IntervalUtil(delay, delay)

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = runWhilePaused

    override fun advance(amount: Float) {
        val translatedAmount = if (useDays) Misc.getDays(amount) else amount
        interval.advance(translatedAmount)
        if (interval.intervalElapsed()) {
            execute()
            stop()
        }
    }
}
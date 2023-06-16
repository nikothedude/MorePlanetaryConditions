package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore

class niko_MPC_conditionCullingScript(var handler: niko_MPC_satelliteHandlerCore): niko_MPC_deltaTimeScript() {
    override val thresholdForAdvancement: Float = 60f

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (!canAdvance(amount)) return

        handler.cullUselessConditions()
    }
}
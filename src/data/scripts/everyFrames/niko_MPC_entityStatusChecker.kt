package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore

class niko_MPC_entityStatusChecker(val handler: niko_MPC_satelliteHandlerCore): niko_MPC_deltaTimeScript() {
    override val thresholdForAdvancement: Float = 1f
    override val onlyUseDeltaIfPlayerNotNear = true

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (handler.entity.isExpired || handler.entity.hasTag(Tags.FADING_OUT_AND_EXPIRING)) {
            handler.delete()
        }
    }
}
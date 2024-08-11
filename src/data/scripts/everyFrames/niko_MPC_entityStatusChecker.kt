package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_miscUtils.isDespawning

class niko_MPC_entityStatusChecker(val handler: niko_MPC_satelliteHandlerCore): niko_MPC_deltaTimeScript() {
    override val thresholdForAdvancement: Float = 2f
    override val onlyUseDeltaIfPlayerNotNear = true

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
        if (handler.entity.isDespawning()) {
            handler.delete()
        }
    }
}

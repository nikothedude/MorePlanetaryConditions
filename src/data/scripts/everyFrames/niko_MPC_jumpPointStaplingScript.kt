package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.campaign.JumpPoint
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc

class niko_MPC_jumpPointStaplingScript(
    val toMove: SectorEntityToken,
    val target: SectorEntityToken
): niko_MPC_baseNikoScript() {
    override fun startImpl() {
        toMove.addScript(this)
    }

    override fun stopImpl() {
        toMove.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!target.isAlive){
            delete()
            return
        }
        toMove.location.set(target.getApproximateHyperspaceLoc())
    }
}
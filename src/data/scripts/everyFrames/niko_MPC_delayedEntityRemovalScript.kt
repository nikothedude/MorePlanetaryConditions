package data.scripts.everyFrames

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.util.IntervalUtil

class niko_MPC_delayedEntityRemovalScript(
    val victim: SectorEntityToken,
    val keepGoingFor: Float
): niko_MPC_baseNikoScript() {

    val interval = IntervalUtil(keepGoingFor, keepGoingFor)

    override fun startImpl() {
        victim.addScript(this)
    }

    override fun stopImpl() {
        victim.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (victim.containingLocation == null) delete()
        interval.advance(keepGoingFor)
        victim.containingLocation?.removeEntity(victim)
        if (interval.intervalElapsed() || !victim.isAlive) delete()
    }
}
package data.scripts.campaign.magnetar.crisis.assignments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.fadeAndExpire

class MPC_distanceBasedDespawnScript(val entity: SectorEntityToken, val distance: Float): niko_MPC_baseNikoScript() {
    override fun startImpl() {
        Global.getSector().playerFleet.addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().playerFleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val dist = MathUtils.getDistance(entity, Global.getSector().playerFleet)
        if (dist >= distance) {
            entity.fadeAndExpire(1f)
            stop()
        }
    }

}

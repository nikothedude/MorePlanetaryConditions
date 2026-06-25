package data.scripts.ghosts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_ambushFleetManager: niko_MPC_baseNikoScript() {
    val cooldown = IntervalUtil(360f, 370f)
    var primed = true

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!primed) {
            cooldown.advance(Misc.getDays(amount))
        }

        if (cooldown.intervalElapsed()) {
            primed = true
        }
        if (primed && MPC_ambushFleetScript.canDoAmbush(Global.getSector().playerFleet)) {
            MPC_ambushFleetScript.createNewEncounter(Global.getSector().playerFleet)
        }

    }
}
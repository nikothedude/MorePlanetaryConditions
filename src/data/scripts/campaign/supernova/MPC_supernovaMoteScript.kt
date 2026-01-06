package data.scripts.campaign.supernova

import com.fs.starfarer.api.campaign.PlanetAPI
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_supernovaMoteScript(val star: PlanetAPI): niko_MPC_baseNikoScript() {

    companion object {
        const val MAX_SWARMS = 10
    }

    val swarms = HashSet<MPC_moteSwarm>()

    override fun startImpl() {
        star.addScript(this)
    }

    override fun stopImpl() {
        star.removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        swarms.forEach { it.advance(amount) }
    }
}
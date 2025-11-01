package data.scripts.campaign.supernova

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_miscUtils
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils

class MPC_supernovaActionScript(
    val star: PlanetAPI
): niko_MPC_baseNikoScript() {

    enum class Stage {
        BEFORE,
        DURING,
        ENDING;

        fun apply() {}
    }

    override fun startImpl() {
        star.addScript(this)
    }

    override fun stopImpl() {
        star.removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        TODO("Not yet implemented")
    }

    override fun advance(amount: Float) {
        TODO("Not yet implemented")
    }

    fun detonate() {

        doExplodeAlwaysEffects()
    }

    private fun doExplodeAlwaysEffects() {
        Global.getSoundPlayer().playUISound(
            "MPC_supernovaDistant",
            1f,
            1f
        ) // boom.
        Global.getSector().campaignUI.addMessage(
            "WARNING::::MASSIVE SPATIAL DISTORTION DETECTED",
            Misc.getNegativeHighlightColor()
        )
    }
}
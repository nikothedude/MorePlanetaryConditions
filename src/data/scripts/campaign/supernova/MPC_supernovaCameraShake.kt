package data.scripts.campaign.supernova

import com.fs.starfarer.api.Global
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_reflectionUtils
import org.lazywizard.lazylib.MathUtils
import kotlin.unaryMinus

class MPC_supernovaCameraShake(
    val script: MPC_supernovaActionScript
): niko_MPC_baseNikoScript() {

    companion object {
        const val BASE_INTENSITY = 500f
    }

    val state = AppDriver.getInstance().currentState as CampaignState
    val offset = niko_MPC_reflectionUtils.get("viewOffset", state)

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    fun getCurrIntensity(): Float {
        val isCurrLoc = script.star.containingLocation.isCurrentLocation
        val inverted = (1 - script.getStageProgress())

        var base = BASE_INTENSITY * inverted
        if (!isCurrLoc) {
            base *= 0.5f
            base -= (BASE_INTENSITY * 0.3f)
        // fades very fast
        }
        return base
    }

    override fun advance(amount: Float) {

        if (offset != null) {
            val intensity = (getCurrIntensity())
            val floats = niko_MPC_reflectionUtils.findFieldsOfType(offset, Float::class.java)
            val x = floats[2]
            val y = floats[3]

            val newX = (x.get(offset) as Float) + MathUtils.getRandomNumberInRange(intensity * amount, intensity * -amount)
            x.set(offset, newX)
            val newY = (y.get(offset) as Float) + MathUtils.getRandomNumberInRange(intensity * amount, intensity * -amount)
            y.set(offset, newY)
        }
    }


}
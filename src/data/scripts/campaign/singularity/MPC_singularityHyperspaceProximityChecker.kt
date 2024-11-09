package data.scripts.campaign.singularity

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils

class MPC_singularityHyperspaceProximityChecker(
    val singularitySystem: StarSystemAPI
): niko_MPC_baseNikoScript() {

    companion object {
        const val RANGE_TO_DETECT = 7000f
    }

    val interval = IntervalUtil(0.5f, 0.6f)

    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            check()
        }
    }

    private fun check() {
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DISCOVERED_SINGULARITY_READINGS] == true) {
            return stop()
        }

        val playerFleet = Global.getSector().playerFleet ?: return
        if (!playerFleet.isInHyperspace || (Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin).abyssPlugin.getAbyssalDepth(playerFleet) < 1f) return
        val dist = MathUtils.getDistance(singularitySystem.location, playerFleet.location)
        if (dist <= RANGE_TO_DETECT) {
            Global.getSector().memoryWithoutUpdate.set("\$MPC_singularityOrientation",  getSingularityOrientation(playerFleet), 0f)
            doDialogue()
            return stop()
        }
    }

    private fun getSingularityOrientation(entity: SectorEntityToken): String {
        return "${VectorUtils.getAngle(singularitySystem.location, entity.location)}°"
    }

    private fun doDialogue() {
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DISCOVERED_SINGULARITY_READINGS] = true
        Global.getSector().campaignUI.showInteractionDialog(RuleBasedInteractionDialogPluginImpl("MPC_singularityProximity"), Global.getSector().playerFleet)
    }
}
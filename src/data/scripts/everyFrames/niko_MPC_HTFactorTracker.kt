package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTFactorTracker
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTPoints
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTScanFactor
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.terrain.niko_MPC_scannableTerrain
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils

class niko_MPC_HTFactorTracker: niko_MPC_baseNikoScript() {

    companion object {
        const val JUMP_POINT_DIST_MAX = 300f

        const val BIPARTISAN_JUMP_POINT_SCORE = 25
    }

    protected var interval = IntervalUtil(HTFactorTracker.CHECK_DAYS * 0.8f, HTFactorTracker.CHECK_DAYS * 1.2f)

    var canCheckSB = true
    var scanned = LinkedHashSet<String>()
    
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Global.getSector().clock.convertToDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            checkSensorBursts()
        }
    }

    fun checkSensorBursts() {
        val playerFleet = Global.getSector().playerFleet ?: return
        val sensorBurstAbility = playerFleet.getAbility(Abilities.SENSOR_BURST) ?: return
        if (sensorBurstAbility.isUsable || sensorBurstAbility.level <= 0) {
            canCheckSB = true
        }
        if ((!canCheckSB || !sensorBurstAbility.isInProgress || sensorBurstAbility.level <= 0.9f) || playerFleet.containingLocation.hasTag(Tags.NO_TOPOGRAPHY_SCANS)) return

        if (playerFleet.isInHyperspace) {
            val jumpPointList: MutableSet<SectorEntityToken>? = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedExitJumppoints] as? MutableSet<SectorEntityToken>
            if (jumpPointList != null) checkJumpPoints(jumpPointList)
        } else {
            for (terrain in playerFleet.containingLocation.terrainCopy) {
                val plugin = terrain.plugin
                if (plugin is niko_MPC_scannableTerrain) plugin.onScanned(this, playerFleet, sensorBurstAbility)
            }
            checkJumpPoints(playerFleet.containingLocation.jumpPoints.toMutableSet())
        }
        canCheckSB = false

    }

    // we can be assured that the jumppoints are in the player's containing loc
    fun checkJumpPoints(jumpPoints: MutableSet<SectorEntityToken>) {
        for (jumpPoint in jumpPoints) {
            if (jumpPoint !is JumpPointAPI) continue
            if (jumpPoint.memoryWithoutUpdate[niko_MPC_ids.hyperspaceLinkedJumpPointDesignationId] != true) continue

            tryScanHyperspaceLinkedJumpPoint(jumpPoint)
        }
    }

    fun tryScanHyperspaceLinkedJumpPoint(jumpPoint: JumpPointAPI) {
        val playerFleet = Global.getSector().playerFleet
        val distance = MathUtils.getDistance(playerFleet, jumpPoint)

        val threshold = jumpPoint.radius + JUMP_POINT_DIST_MAX
        if (distance <= threshold) {
            val id = jumpPoint.id

            val exitOrEntry = if (jumpPoint.isInHyperspace) "exit" else "entry"
            if (scanned.contains(id)) {
                reportNoDataAcquired("Bipartisan $exitOrEntry point already scanned")
            } else {
                HyperspaceTopographyEventIntel.addFactorCreateIfNecessary(
                    HTScanFactor("Bipartisan $exitOrEntry point scanned", BIPARTISAN_JUMP_POINT_SCORE), null
                )
                scanned.add(id)
            }
        }
    }

    fun reportNoDataAcquired(text: String) {
        Global.getSector().campaignUI.messageDisplay.addMessage(
            "$text, no new topographic data acquired",
            Misc.getNegativeHighlightColor()
        )
    }
}
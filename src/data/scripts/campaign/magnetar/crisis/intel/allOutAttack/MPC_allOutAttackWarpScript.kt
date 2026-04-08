package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Pings
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamEntityPlugin2
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.respawnAllFleets
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_allOutAttackWarpScript(val fob: MarketAPI, val target: MarketAPI): niko_MPC_baseNikoScript() {

    enum class Stage(val duration: Float) {
        BEGINNING(3f),
        WARPING(2f) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                super.apply(fob, target, script)

                val tokenLoc = VectorUtils.getDirectionalVector(fob.location, target.location).scale(fob.primaryEntity.radius) as Vector2f
                fob.containingLocation.createToken(tokenLoc)

                val params = SlipstreamTerrainPlugin2.SlipstreamParams2()
                params.burnLevel = 40
                params.baseWidth = 1200f
                params

                val slipstream = fob.containingLocation.addTerrain(Terrain.SLIPSTREAM, params) as CampaignTerrainAPI
                val plugin = slipstream.plugin as SlipstreamTerrainPlugin2
                plugin.addSegment()

                plugin.despawn(duration, LANDED.duration, MathUtils.getRandom())
            }
        },
        LANDED(1f) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                super.apply(fob, target, script)

                val station = Misc.getStationFleet(fob) ?: return
                val targetStation = Misc.getStationFleet(target) ?: return

                Global.getFactory().createBattle(
                    station,
                    targetStation
                )

                fob.primaryEntity.setCircularOrbitPointingDown(
                    target.primaryEntity,
                    VectorUtils.getAngle(target.location, fob.location),
                    MathUtils.getDistance(target.primaryEntity, fob.primaryEntity),
                    30f
                )
            }
        },
        FINISHED(Float.MAX_VALUE) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                script.delete()
            }
        };

        open fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
            script.interval.setInterval(duration, duration)
        }
    }

    val interval = IntervalUtil(0f, 0f)
    var jitterLevel = 0f
    val maxJitterLevel = 5f
    var stage = Stage.BEGINNING
    val color = Color(60, 0, 255, 255)
    val targetAngle = MathUtils.getRandomNumberInRange(0f, 360f)
    val surgeSourceLoc = VectorUtils.getDirectionalVector(fob.location, target.location).scale(fob.primaryEntity.radius) as Vector2f

    init {
        stage.apply(fob, target, this)
    }

    override fun startImpl() {
        Global.getSector().addScript(this)
        val token = fob.containingLocation.createToken(surgeSourceLoc)
        Global.getSector().addPing(token, Pings.SLIPSURGE)
        /*Global.getSoundPlayer().playSound(
            "MPC_IAIICWarpBegin",
            1f,
            1f,
            surgeSourceLoc,
            Misc.ZERO
        )*/
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            if (stage == Stage.FINISHED) {
                delete()
                return
            }
            stage = Stage.entries.toTypedArray()[stage.ordinal + 1]
            stage.apply(fob, target, this)
        }

        val oldLoc = fob.memoryWithoutUpdate["\$MPC_IAIICOldLoc"] as Vector2f
        val targetLoc = MathUtils.getPointOnCircumference(target.primaryEntity.location, target.primaryEntity.radius, targetAngle)

        val progress = (interval.intervalDuration / interval.elapsed)
        when (stage) {
            Stage.BEGINNING -> {
                jitterLevel = (maxJitterLevel * progress)
            }
            Stage.WARPING -> {
                val newLoc = Misc.interpolateVector(oldLoc, targetLoc, progress)
                fob.location.set(newLoc.x, newLoc.y)
                jitterLevel = maxJitterLevel
            }
            Stage.LANDED -> {
                jitterLevel = (maxJitterLevel * (1 - progress))
            }
            Stage.FINISHED -> {
                delete()
                return
            }
        }

        Misc.getStationFleet(fob)?.views?.forEach { it.setJitter(0f, 1f, color, jitterLevel.toInt(), 5f) }
    }
}
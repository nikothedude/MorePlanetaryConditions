package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility.SlipsurgeFadeInScript
import com.fs.starfarer.api.impl.campaign.ids.Pings
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.respawnAllFleets
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.easeOutSine
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.isPatrol
import org.magiclib.kotlin.isWarFleet
import sound.int
import java.awt.Color
import kotlin.math.min
import kotlin.math.sqrt

class MPC_allOutAttackWarpScript(val fob: MarketAPI, val target: MarketAPI): niko_MPC_baseNikoScript() {

    enum class Stage(val duration: Float) {
        BEGINNING(0.2f),
        WARPING(2f) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                super.apply(fob, target, script)

                script.fleets = fob.containingLocation.fleets.filter { it.faction.id == niko_MPC_ids.IAIIC_FAC_ID && MathUtils.getDistance(it, fob.primaryEntity) <= 600f && (it.isWarFleet() || it.isPatrol()) }.toHashSet()
                for (fleet in script.fleets) {
                    fleet.memoryWithoutUpdate["\$MPC_IAIICOldLoc"] = Vector2f(fleet.location)
                }

                fob.primaryEntity.memoryWithoutUpdate["\$MPC_IAIICOldLoc"] = Vector2f(fob.primaryEntity.location)

                val tokenLoc = VectorUtils.getDirectionalVector(fob.location, target.location).scale(fob.primaryEntity.radius) as Vector2f
                fob.containingLocation.createToken(tokenLoc)

                val params = SlipstreamTerrainPlugin2.SlipstreamParams2()
                params.burnLevel = 40
                params.baseWidth = 1200f

                val slipstream = fob.containingLocation.addTerrain(Terrain.SLIPSTREAM, params) as CampaignTerrainAPI
                slipstream.setLocation(fob.primaryEntity.location.x, fob.primaryEntity.location.y)
                val plugin = slipstream.plugin as SlipstreamTerrainPlugin2

                val spacing = 100f
                val length = 3000f
                val width = 2000f
                val incr: Float = spacing / length

                val to = target.location
                val from = fob.location

                val diff = Vector2f.sub(to, from, Vector2f())
                var f = 0f
                while (f <= 1f) {
                    val curr = Vector2f(diff)
                    curr.scale(f)
                    Vector2f.add(curr, from, curr)
                    plugin.addSegment(curr, width - min(300f, 300f * sqrt(f.toDouble()).toFloat()))
                    f += incr
                }

                plugin.recomputeIfNeeded()

                // TODO untested

                slipstream.addScript(SlipsurgeFadeInScript(plugin))
                plugin.despawn(duration, LANDED.duration, MathUtils.getRandom())

                fob.primaryEntity.orbit = null
            }
        },
        LANDED(1f) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                super.apply(fob, target, script)

                fob.primaryEntity.setCircularOrbitPointingDown(
                    target.primaryEntity,
                    VectorUtils.getAngle(target.primaryEntity.location, fob.primaryEntity.location),
                    MathUtils.getDistance(target.primaryEntity.location, fob.primaryEntity.location),
                    30f
                )

                fob.respawnAllFleets()

                val station = Misc.getStationFleet(fob) ?: return
                val targetStation = Misc.getStationFleet(target) ?: return

                val battle = Global.getFactory().createBattle(
                    station,
                    targetStation
                )
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICFinalBattle"] = battle

                for (fleet in script.fleets) {
                    battle.join(fleet)
                }
            }
        },
        FINISHED(Float.MAX_VALUE) {
            override fun apply(fob: MarketAPI, target: MarketAPI, script: MPC_allOutAttackWarpScript) {
                Global.getSector().memoryWithoutUpdate.unset("\$MPC_IAIICFinalBattle")
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
    val surgeSourceLoc = MathUtils.getPointOnCircumference(fob.primaryEntity.location, fob.primaryEntity.radius, VectorUtils.getAngle(fob.primaryEntity.location, target.primaryEntity.location))
    var fleets = HashSet<CampaignFleetAPI>()
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
            stage = Stage.entries[stage.ordinal + 1]
            stage.apply(fob, target, this)
        }

        val targetLoc = MathUtils.getPointOnCircumference(target.primaryEntity.location, target.primaryEntity.radius, targetAngle)

        var progress = (interval.elapsed / interval.intervalDuration)
        when (stage) {
            Stage.BEGINNING -> {
                jitterLevel = (maxJitterLevel * progress)
            }
            Stage.WARPING -> {
                for (entity in fleets + fob.primaryEntity) {
                    val oldLoc = entity.memoryWithoutUpdate["\$MPC_IAIICOldLoc"] as? Vector2f ?: Misc.ZERO
                    val newLoc = Misc.interpolateVector(oldLoc, targetLoc, easeOutSine(progress).toFloat())
                    //val newLoc = Misc.interpolateVector(oldLoc, targetLoc, progress)
                    if (entity is CampaignFleetAPI) {
                        entity.setLocation(newLoc.x, newLoc.y)
                    } else {
                        entity.location.set(newLoc.x, newLoc.y)
                    }
                    jitterLevel = maxJitterLevel
                }
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
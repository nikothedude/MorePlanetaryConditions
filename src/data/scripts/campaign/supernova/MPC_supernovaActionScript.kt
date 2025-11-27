package data.scripts.campaign.supernova

import com.fs.graphics.particle.GenericTextureParticle
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.ParticleControllerAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.campaign.CampaignUIPersistentData
import com.fs.starfarer.campaign.ParticleController
import com.fs.state.AppDriver
import data.scripts.campaign.supernova.entities.MPC_supernovaExplosion
import data.scripts.campaign.supernova.renderers.MPC_supernovaShader
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_miscUtils.changeTypeManual
import data.utilities.niko_MPC_miscUtils.playSoundEvenIfFar
import data.utilities.niko_MPC_miscUtils.playSoundFar
import data.utilities.niko_MPC_reflectionUtils
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import org.lazywizard.console.commands.Jump
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addHitGlow
import java.awt.Color
import kotlin.math.exp

class MPC_supernovaActionScript(
    val star: PlanetAPI
): niko_MPC_baseNikoScript() {

    companion object {
        fun getCurrStage(): Stage? {
            return (Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"]) as? Stage
        }

        const val PREPARE_PHASE_LENGTH = 9f // seconds
        const val DURING_PHASE_LENGTH = 30f
        const val ENDING_PHASE_LENGTH = 5f

        const val MIN_STAR_SIZE = 100f
        const val MIN_CORONA_BAND = 25f
    }

    enum class Stage {
        BEFORE,
        DURING,
        ENDING;

        fun apply() {}
    }

    var coronaRad = 0f
    var coronaBand = 0f
    var baseRadius = 0f
    var baseColor = star.lightColor
    var screenshake: MPC_supernovaCameraShake? = null
    var supernovaGlow: ParticleControllerAPI? = null
    var supernovaParticle: Any? = null
    var shockwaveColor = Color(star.spec.atmosphereColor.red, star.spec.atmosphereColor.blue, star.spec.atmosphereColor.green, star.spec.atmosphereColor.alpha)
        get() {
            if (field == null) field = Color(star.spec.atmosphereColor.red, star.spec.atmosphereColor.blue, star.spec.atmosphereColor.green, star.spec.atmosphereColor.alpha)
            return field
        }
    var shockwave: MPC_supernovaExplosion? = null
    init {
        Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.BEFORE
        playSoundFar("MPC_supernovaInit", star.containingLocation, star.location)
        val corona = Misc.getCoronaFor(star)
        coronaRad = corona.params.middleRadius
        coronaBand = corona.params.bandWidthInEngine
        baseRadius = star.radius

        LunaCampaignRenderer.addRenderer(MPC_supernovaShader(this))
    }

    override fun startImpl() {
        star.addScript(this)
    }

    override fun stopImpl() {
        star.removeScript(this)
        Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    val interval = IntervalUtil(PREPARE_PHASE_LENGTH, PREPARE_PHASE_LENGTH) // will be overridden by stages
    val initRadius = star.radius
    override fun advance(amount: Float) {
        interval.advance(amount)
        if (interval.intervalElapsed()) {
            when (getCurrStage()!!) {
                Stage.BEFORE -> {
                    playSoundFar("MPC_supernova", star.containingLocation, star.location)
                    playSoundFar("MPC_supernovaThunder", star.containingLocation, star.location)
                    playSoundFar("MPC_supernovaTwo", star.containingLocation, star.location)
                    playSoundFar("MPC_supernovaUnder", star.containingLocation, star.location)
                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.DURING

                    val containing = star.containingLocation
                    val explParams = ExplosionEntityPlugin.ExplosionParams(
                        shockwaveColor,
                        containing,
                        Vector2f(star.location),
                        star.radius * 1.1f,
                        12f
                    )
                    explParams.damage = ExplosionEntityPlugin.ExplosionFleetDamage.EXTREME
                    val expl = containing.addCustomEntity(
                        "MPC_supernovaExplosionInit",
                        null,
                        "MPC_supernovaExplosion",
                        Factions.NEUTRAL,
                        explParams
                    )
                    shockwave = expl.customPlugin as MPC_supernovaExplosion
                    supernovaGlow = star.containingLocation.addParticle(
                        Vector2f(star.location),
                        Misc.ZERO,
                        100000f,
                        1f,
                        0f,
                        10000f,
                        shockwaveColor
                    )
                    supernovaParticle = niko_MPC_reflectionUtils.get("p", supernovaGlow!!, ParticleController::class.java)
                    screenshake = MPC_supernovaCameraShake(this)
                    screenshake?.start()

                    star.changeTypeManual(
                        StarTypes.WHITE_DWARF,
                        Color.WHITE,
                        Color.WHITE,
                        MathUtils.getRandom()
                    )

                    interval.setInterval(DURING_PHASE_LENGTH, DURING_PHASE_LENGTH)

                    Global.getSector().campaignUI.addMessage("WARNING:::MASSIVE SPATIAL DISRUPTION DETECTED", Misc.getNegativeHighlightColor())

                }
                Stage.DURING -> {
                    supernovaGlow?.maxAge = 500f
                    screenshake?.stop()
                    screenshake = null

                    star.containingLocation.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "MPC_supernovaAmbience"

                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = Stage.ENDING
                    interval.setInterval(ENDING_PHASE_LENGTH, ENDING_PHASE_LENGTH)

                    getJumpPoints().forEach { it.memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] = true }
                }
                Stage.ENDING -> {
                    Global.getSector().memoryWithoutUpdate["\$MPC_supernovaActionStage"] = null
                    getJumpPoints().forEach { it.memoryWithoutUpdate.unset(JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY) }
                    delete()
                    return
                }
            }
        }

        val progress = getStageProgress()
        val corona = Misc.getCoronaFor(star) ?: return
        when (getCurrStage()!!) {
            Stage.BEFORE -> {
                val dist = (initRadius - MIN_STAR_SIZE)
                val remainder = (dist / initRadius)
                val inverted = (1 - (progress * remainder))
                star.radius = (initRadius * (inverted)).coerceAtLeast(MIN_STAR_SIZE)
                val diff = initRadius - star.radius
                //corona.params.bandWidthInEngine = (coronaBand * inverted).coerceAtLeast(MIN_CORONA_BAND)
                corona.params.middleRadius = (coronaRad * inverted).coerceAtLeast(MIN_STAR_SIZE)

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(progress)
                }

                //star.lightColorOverrideIfStar = Color(r, g, b, 255)

                star.containingLocation.addHitParticle(
                    star.location,
                    Misc.ZERO,
                    star.radius + (diff * 0.5f),
                    progress * 10f,
                    amount,
                    star.spec.glowColor
                )
            }
            Stage.DURING -> {
                val shockwaveDist = shockwave!!.getProgress() * 5f
                if (supernovaParticle is GenericTextureParticle) {
                    val casted = supernovaParticle as GenericTextureParticle
                    casted.setSize(shockwaveDist, shockwaveDist)
                }

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(1f)
                }
            }
            Stage.ENDING -> {

                if (star.containingLocation.isCurrentLocation) {
                    val state = AppDriver.getInstance().currentState as CampaignState
                    state.suppressMusic(1f - progress)
                }

            }
        }
    }

    fun getJumpPoints(): MutableSet<JumpPointAPI> {
        val points = HashSet<JumpPointAPI>()

        points.addAll(star.starSystem.jumpPoints as Collection<out JumpPointAPI>)
        for (point in Global.getSector().hyperspace.jumpPoints) {
            val casted = point as? JumpPointAPI ?: continue
            if (casted.destinationStarSystem == star.starSystem) points += casted
        }

        return points
    }

    fun getStageProgress(): Float {
        val curr = interval.elapsed
        val dur = interval.intervalDuration

        return (curr / dur)
    }

    fun detonate() {
        doExplodeAlwaysEffects()
    }

    private fun doExplodeAlwaysEffects() {
        Global.getSoundPlayer().playUISound( // TODO add this sound. make it mono too
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
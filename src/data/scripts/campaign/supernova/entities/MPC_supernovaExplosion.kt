package data.scripts.campaign.supernova.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.supernova.MPC_supernovaActionScript
import data.scripts.campaign.supernova.MPC_supernovaActionScript.Companion.SHIELD_BUBBLE_ACTIVATE_DIST
import data.scripts.campaign.supernova.MPC_supernovaActionScript.Companion.SHIELD_BUBBLE_DIST
import data.scripts.campaign.supernova.renderers.MPC_vaporizedShader
import data.scripts.campaign.supernova.terrain.SupernovaNebulaHandler
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.SHIELD_BUBBLE_PLANET
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.max
import kotlin.math.roundToInt
import data.utilities.niko_MPC_miscUtils.setArc
import org.magiclib.kotlin.setAlpha
import kotlin.math.roundToInt

class MPC_supernovaExplosion: ExplosionEntityPlugin() {

    companion object {
        const val SPEED = 500f

        const val DIST_TO_PLAY_LOOP = 5000f
        const val MIN_DIST_FOR_MAX_VOL = 1000f

        const val MIN_ASTEROID_SIZE = 10f
        const val MAX_ASTEROID_SIZE = 40f

        const val ASTEROID_DIVISOR = 10f

        fun createShieldBubble(sys: StarSystemAPI): PlanetAPI {
            val token = sys.jumpPoints.last().orbitFocus as SectorEntityToken
            val angle = VectorUtils.getAngle(sys.star.location, token.location)
            val dist = MathUtils.getDistance(sys.star.location, token.location)
            val planet = sys.addPlanet(
                "MPC_shieldPlanet",
                sys.star,
                "Anomaly",
                "MPC_shieldPlanet",
                angle,
                200f,
                dist,
                token.orbit.orbitalPeriod
            )
            planet.memoryWithoutUpdate["\$MPC_ignoresSupernova"] = true
            //planet.market.addCondition("MPC_shieldBubbleCond")
            sys.memoryWithoutUpdate[SHIELD_BUBBLE_PLANET] = planet

            val jumpPoint = sys.memoryWithoutUpdate[niko_MPC_ids.SUPERNOVA_SHIELD_JUMPPOINT] as JumpPointAPI
            jumpPoint.setCircularOrbit(planet, VectorUtils.getAngle(token.location, jumpPoint.location), 400f, jumpPoint.orbit.orbitalPeriod)

            sys.removeEntity(token)

            Global.getSoundPlayer().playSound(
                "MPC_shieldPlanetRaise",
                1f,
                1f,
                planet.location,
                Misc.ZERO
            )

            return planet
        }
    }

    lateinit var token: SectorEntityToken
    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        sprite = Global.getSettings().getSprite("misc", "nebula_particles")

        params = pluginParams as ExplosionParams?
        token = params.where.createToken(Vector2f(0f, 0f))

        val baseSize = params.radius * 30f
        maxParticleSize = baseSize * 3f

        val fullArea = (Math.PI * params.radius * params.radius).toFloat()
        val particleArea = (Math.PI * baseSize * baseSize).toFloat()

        val count = 500

        var durMult = 2f
        durMult = params.durationMult

        // new below
        shockwaveDuration = 10000f // will be manually deleted
        shockwaveSpeed = SPEED

        //baseSize *= 0.5f;
        for (i in 0..<count) {
            val size = baseSize * (1f + Math.random().toFloat())

            val randomColor = Color(
                Misc.random.nextInt(256),
                Misc.random.nextInt(256), Misc.random.nextInt(256), params.color.getAlpha()
            )
            var adjustedColor = Misc.interpolateColor(params.color, randomColor, 0.2f)
            adjustedColor = params.color
            val data = ParticleData(
                adjustedColor, size,
                shockwaveDuration, 60f
            )

            val r = Math.random().toFloat()
            val dist = params.radius * 0.2f * (0.1f + r * 0.9f)
            var dir = Math.random().toFloat() * 360f
            data.setOffset(dir, dist, dist)

            dir = Misc.getAngleInDegrees(data.offset)

            //			data.setVelocity(dir, baseSize * 0.25f, baseSize * 0.5f);
//			data.vel.scale(1f / durMult);
            data.swImpact = Math.random().toFloat()
            if (i > count / 2) data.swImpact = 1f

            particles.add(data)
        }

        var loc = Vector2f(params.loc)
        loc.x -= params.radius * 0.01f
        loc.y += params.radius * 0.01f

        val b = 1f
        params.where.addHitParticle(loc, Vector2f(), 40f, b, 1f * durMult, params.color)
        loc = Vector2f(params.loc)
        params.where.addHitParticle(loc, Vector2f(), 100f, 0.5f, 1f * durMult, Color.white)

        shockwaveAccel = baseSize * 70f / durMult

        //shockwaveRadius = -1500f;
        shockwaveRadius = 0f
        shockwaveRadius = -params.radius * 0.5f
        shockwaveSpeed = params.radius * 2f / durMult
        shockwaveDuration = params.radius * 2f / shockwaveSpeed
        shockwaveWidth = params.radius * 0.5f

        shockwaveDuration = 10000f // will be manually deleted
        shockwaveSpeed = SPEED

        for (particle in particles) {
            val accel = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(particle.offset))
            accel.scale(shockwaveSpeed)
            particle.vel.x += accel.x
            particle.vel.y += accel.y
        }
    }

    fun getProgress(): Float {
        var shockwaveDist = 0f
        for (p in particles) {
            shockwaveDist = max(shockwaveDist, p.offset.length())
        }
        return shockwaveDist
    }

    override fun getRenderRange(): Float {
        return Float.MAX_VALUE
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!params.where.isCurrentLocation) return
        val viewport = Global.getSector().viewport
        val viewportOffset = MathUtils.getDistance(params.loc, viewport.center)
        val ourDist = getProgress()

        if (ourDist >= SHIELD_BUBBLE_ACTIVATE_DIST) {
            val progress = ((ourDist - SHIELD_BUBBLE_ACTIVATE_DIST) / (SHIELD_BUBBLE_DIST - SHIELD_BUBBLE_ACTIVATE_DIST)).coerceAtMost(1f).coerceAtLeast(0f)
            if (params.where.memoryWithoutUpdate[niko_MPC_ids.SHIELD_BUBBLE_PLANET] == null) {
                createShieldBubble(params.where as StarSystemAPI)
            }
            val planet = params.where.memoryWithoutUpdate[niko_MPC_ids.SHIELD_BUBBLE_PLANET] as PlanetAPI

            // todo - make it so it fades in. this doesnt work
            /*planet.spec.
            planet.spec.planetColor.setAlpha((255 * progress).roundToInt())
            planet.spec.iconColor.setAlpha((255 * progress).roundToInt())*/
        }

        val distFromUs = (viewportOffset - ourDist).coerceAtLeast(0f)
        if (distFromUs <= DIST_TO_PLAY_LOOP) {
            playLoop()
        }

        val particleScaleMax = ourDist / 500f
        for (particle in particles) {
            particle.scale = particle.scale.coerceAtMost(particleScaleMax)
        }

        for (planet in params.where.planets.filter { !it.isStar && !it.memoryWithoutUpdate.getBoolean("\$MPC_ignoresSupernova") && !it.memoryWithoutUpdate.getBoolean("\$MPC_reactedToNova") }) {
            val planDist = MathUtils.getDistance(params.loc, planet.location)
            if ((planDist - planet.radius) <= ourDist) {
                explodePlanet(planet)
            }
        }

        for (entity in params.where.getCustomEntitiesWithTag("MPC_supernovaInhibitor")) {
            val entityDist = MathUtils.getDistance(params.loc, entity.location)
            if ((entityDist - entity.radius) <= ourDist) {
                params.where.removeEntity(entity)
            }
        }

        val sys = params.where as StarSystemAPI
        val star = sys.star
        val rad = star.radius * 3f
        if (ourDist <= rad) return
        val editors = SupernovaNebulaHandler.getEditors() ?: return
        for (editor in editors.values) {
            editor.setArc(
                100,
                0f,
                0f,
                rad,
                ourDist - 50f,
                0f,
                360f
            )
        }

        if (MPC_supernovaActionScript.getCurrStage() == null) {
            params.where.removeEntity(this.entity)
            return
        }
    }

    private fun explodePlanet(planet: PlanetAPI) {
        val explParams = ExplosionParams(
            planet.spec.planetColor,
            planet.containingLocation,
            planet.location,
            planet.radius * 1.5f,
            1f
        )
        explParams.damage = ExplosionFleetDamage.NONE

        val explosion = planet.containingLocation.addCustomEntity(
            "${planet.id}_explosion_MPC",
            null,
            Entities.EXPLOSION,
            Factions.NEUTRAL,
            explParams
        )

        val asteroidDivisor = ASTEROID_DIVISOR
        val numAsteroids = planet.radius / asteroidDivisor
        val angle = VectorUtils.getAngle(params.loc, planet.location)
        val min = (angle - 90f)
        val max = (angle + 90f)
        var asteroidsLeft = numAsteroids
        while (asteroidsLeft-- > 0) {
            val velTarget = Misc.normalizeAngle(MathUtils.getRandomNumberInRange(min, max))
            val velVector = Misc.getUnitVectorAtDegreeAngle(velTarget).scale(shockwaveSpeed) as Vector2f
            val offsetX = MathUtils.getRandomNumberInRange(-planet.radius, planet.radius)
            val offsetY = MathUtils.getRandomNumberInRange(-planet.radius, planet.radius)
            val randLoc = Vector2f(planet.location).translate(offsetX, offsetY)
            val asteroidRadius = MathUtils.getRandomNumberInRange(MIN_ASTEROID_SIZE, MAX_ASTEROID_SIZE)
            val asteroid = planet.containingLocation.addAsteroid(asteroidRadius)
            asteroid.setLocation(randLoc.x, randLoc.y)
            asteroid.velocity.set(velVector)
        }

        planet.containingLocation.removeEntity(planet)
    }

    private fun playLoop() {
        val ourDist = getProgress()
        val viewport = Global.getSector().viewport
        val angle = VectorUtils.getAngle(params.loc, viewport.center)
        val target = MathUtils.getPointOnCircumference(
            params.loc,
            ourDist,
            angle
        )
        val newLoc = MathUtils.getPointOnCircumference(viewport.center, 10f, VectorUtils.getAngle(viewport.center, params.loc))

        val distFromTarget = MathUtils.getDistance(target, viewport.center)
        val adjustedDist = (distFromTarget - MIN_DIST_FOR_MAX_VOL).coerceAtLeast(0f)
        val volume = 1 - (adjustedDist / DIST_TO_PLAY_LOOP)
        token.setLocation(newLoc.x, newLoc.y)
        Global.getSoundPlayer().playLoop(
            "MPC_supernovaWavefrontLoop",
            token,
            1f,
            volume,
            token.location,
            Misc.ZERO
        )
    }

    override fun applyDamageToFleet(fleet: CampaignFleetAPI?, damageMult: Float) {
        if (fleet == null) return
        if (!fleet.isPlayerFleet) {
            for (member in fleet.fleetData.membersListCopy) {
                fleet.removeFleetMemberWithDestructionFlash(member)
            }
            fleet.despawn()
        } else {
            LunaCampaignRenderer.addRenderer(MPC_vaporizedShader())

            class VaporizedScript: MPC_delayedExecutionNonLambda(IntervalUtil(6f, 6f), false, false) {
                override fun executeImpl() {
                    Global.getSector().campaignUI.showInteractionDialog(
                        RuleBasedInteractionDialogPluginImpl("MPC_vaporizedInit"),
                        Global.getSector().playerFleet
                    )
                }
            }
            class VaporizedScriptTwo: MPC_delayedExecutionNonLambda(IntervalUtil(1.1f, 1.1f), false, false) {
                override fun executeImpl() {
                    Global.getSector().playerFleet.setLocation(99999f, 99999f)
                }
            }

            VaporizedScriptTwo().start()
            VaporizedScript().start()
            Global.getSoundPlayer().playUISound("MPC_vaporized", 1f, 1f)

            Global.getSector().campaignUI.addMessage(
                "Your fleet is vaporized by the Supernova",
                Misc.getNegativeHighlightColor()
            )
        }
    }

}
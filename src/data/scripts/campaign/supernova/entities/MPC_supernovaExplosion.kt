package data.scripts.campaign.supernova.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.max
import kotlin.math.roundToInt

class MPC_supernovaExplosion: ExplosionEntityPlugin() {

    companion object {
        const val SPEED = 500f
    }

    override fun init(entity: SectorEntityToken?, pluginParams: Any?) {
        super.init(entity, pluginParams)

        sprite = Global.getSettings().getSprite("misc", "nebula_particles")

        params = pluginParams as ExplosionParams?

        val baseSize = params.radius * 0.08f
        maxParticleSize = baseSize * 2f

        val fullArea = (Math.PI * params.radius * params.radius).toFloat()
        val particleArea = (Math.PI * baseSize * baseSize).toFloat()

        val count = (fullArea / particleArea * 50f).roundToInt()

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

}
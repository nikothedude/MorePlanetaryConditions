package data.scripts.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

class MPC_interceptScriptTwo: MPC_interceptScript() {
    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        super.onFire(projectile, weapon, engine)
        if (projectile == null || engine == null) return
        if (projectile !is MissileAPI || projectile.ai !is GuidedMissileAI) return

        engine.addPlugin(MPC_proxFuseScript(projectile, projectile.ai as GuidedMissileAI, engine))
    }

    class MPC_proxFuseScript(val projectile: MissileAPI, val ai: GuidedMissileAI, val engine: CombatEngineAPI) : BaseEveryFrameCombatPlugin() {

        companion object {
            const val RANGE_TO_DETONATE = 50f
        }

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)
            if (engine.isPaused) {
                return
            }
            if (!engine.isEntityInPlay(projectile) || projectile.isFading || projectile.isFizzling) {
                engine.removePlugin(this)
                return
            }

            val target = ai.target ?: return
            val dist = MathUtils.getDistance(projectile, target)
            if (dist > RANGE_TO_DETONATE) return

            val explosion = DamagingExplosionSpec(
                0.1f,
                90f,
                RANGE_TO_DETONATE,
                projectile.damage.damage,
                projectile.damage.damage,
                CollisionClass.MISSILE_NO_FF,
                CollisionClass.MISSILE_NO_FF,
                1.5f,
                3f,
                0.5f,
                10,
                Color(185, 245, 255, 255),
                Color(185, 245, 255, 255)
            )

            explosion.damageType = DamageType.FRAGMENTATION
            explosion.isShowGraphic = true
            explosion.soundSetId = "MPC_heavyFlakExplosion"

            engine.spawnDamagingExplosion(explosion, projectile.source, projectile.location)
            engine.removeEntity(projectile)
            engine.removePlugin(this)

        }
    }
}
package data.scripts.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.lazylib.MathUtils

class MPC_pinprickScript: OnFireEffectPlugin {

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        if (projectile == null || engine == null) return
        if (projectile !is MissileAPI || projectile.ai !is GuidedMissileAI) return

        engine.addPlugin(MPC_pinprickMissileScript(projectile, projectile.ai as GuidedMissileAI, engine))
    }

    class MPC_pinprickMissileScript(val missile: MissileAPI, val ai: GuidedMissileAI, val engine: CombatEngineAPI): BaseEveryFrameCombatPlugin() {

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (!engine.isEntityInPlay(missile) || missile.isFading || missile.isFizzling || ai.target == null) {
                engine.removePlugin(this)
                return
            }

            val dist = MathUtils.getDistance(missile, ai.target)
            val collissionRadius = ai.target.collisionRadius

            if (dist <= (collissionRadius * 2.4f)) {
                //missile.missileAI = null
                missile.engineStats.maxTurnRate.modifyMult("MPC_passedTarget", 0f)
                engine.removePlugin(this)
                return
            }
        }
    }

}
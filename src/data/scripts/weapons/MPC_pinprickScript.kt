package data.scripts.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.lazylib.MathUtils

open class MPC_pinprickScript: OnFireEffectPlugin {

    open var distNeeded = 300f

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        if (projectile == null || engine == null) return
        if (projectile !is MissileAPI || projectile.ai !is GuidedMissileAI) return

        engine.addPlugin(MPC_loseLockOnMissScript(projectile, projectile.ai as GuidedMissileAI, engine, distNeeded))
    }
    class MPC_loseLockOnMissScript(
        val missile: MissileAPI,
        val ai: GuidedMissileAI,
        val engine: CombatEngineAPI,
        val distNeeded: Float
    ): BaseEveryFrameCombatPlugin() {

        var inFinalApproach = false

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (!engine.isEntityInPlay(missile) || missile.isFading || missile.isFizzling || ai.target == null) {
                engine.removePlugin(this)
                return
            }

            val dist = MathUtils.getDistance(missile, ai.target)
            if (dist <= distNeeded) {
                inFinalApproach = true
            }
            if (inFinalApproach && dist >= distNeeded + 10f) {
                missile.engineStats.maxTurnRate.modifyMult("MPC_passedTarget", 0f)
                missile.engineStats.deceleration.modifyMult("MPC_passedTarget", 0f)
                missile.engineStats.acceleration.modifyMult("MPC_passedTarget", 0f)
                engine.removePlugin(this)
                return
            }
        }
    }

}
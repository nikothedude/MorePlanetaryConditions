package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.OnHitEffectPlugin
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.loading.ProjectileSpecAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_settings
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.ext.json.getFloat
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getColor
import org.magiclib.kotlin.getObj

class MPC_cruiseMissileOnHit: OnHitEffectPlugin {

    companion object {
        fun doGraphics(point: Vector2f, pitch: Float) {

            Global.getSoundPlayer().playSound(
                "gate_explosion",
                pitch,
                1f,
                point,
                Misc.ZERO
            )

            if (niko_MPC_settings.graphicsLibEnabled) {
                val ripple = RippleDistortion(point, Misc.ZERO)
                ripple.intensity = 400f
                ripple.size = 1800f
                ripple.fadeInSize(0.8f)
                ripple.fadeOutIntensity(0.4f)

                DistortionShader.addDistortion(ripple)
            }
        }
    }

    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    ) {
        if (projectile == null || point == null) return
        if (engine == null || damageResult == null) return

        doGraphics(point, 1f)

        val overkill = damageResult.overMaxDamageToShields
        if (overkill > 0f) {
            engine.addPlugin(MPC_cruiseMissileSecondaryExplosionScript(projectile, point, overkill))
        }
    }

    class MPC_cruiseMissileSecondaryExplosionScript(
        val projectile: DamagingProjectileAPI,
        val point: Vector2f,
        val overkill: Float,
    ): BaseEveryFrameCombatPlugin() {
        val delay = IntervalUtil(0.4f, 0.4f)

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            delay.advance(amount)
            if (delay.intervalElapsed()) {
                doExplosion()
                engine.removePlugin(this)
            }
        }

        private fun doExplosion() {
            val engine = Global.getCombatEngine()
            val casted = projectile as MissileAPI
            val explosionSpec = casted.spec.explosionSpec.clone()
            explosionSpec.maxDamage = overkill
            explosionSpec.minDamage = overkill
            /*explosionSpec.damageType = DamageType.HIGH_EXPLOSIVE
            explosionSpec.isShowGraphic = true
            explosionSpec.isUseDetailedExplosion = true
            explosionSpec.detailedExplosionFlashColorFringe = spec.getColor("detailedExplosionFlashColorFringe")
            explosionSpec.detailedExplosionRadius = 1800f*/
            engine.spawnDamagingExplosion(explosionSpec, projectile.source, point, false)

            doGraphics(point, 0.5f)
        }
    }
}
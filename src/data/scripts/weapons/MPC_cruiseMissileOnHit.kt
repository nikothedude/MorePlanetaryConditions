package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.OnHitEffectPlugin
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_settings
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lwjgl.util.vector.Vector2f

class MPC_cruiseMissileOnHit: OnHitEffectPlugin {
    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    ) {
        Global.getSoundPlayer().playSound(
            "gate_explosion",
            1f,
            1f,
            point,
            Misc.ZERO
        )

        if (projectile == null || point == null) return

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
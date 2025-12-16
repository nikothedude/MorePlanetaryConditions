package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI
import data.scripts.weapons.MPC_shieldMissileScript.Companion.DRONE_DATA_KEY
import data.utilities.niko_MPC_mathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MPC_bullrushOnHit: OnHitEffectPlugin {

    companion object {
        const val EMP_ARCS = 6f
        const val DAMAGE_MULT = 0.2f
        const val EMP_MULT = 0.7f
    }

    override fun onHit(
        projectile: DamagingProjectileAPI?,
        target: CombatEntityAPI?,
        point: Vector2f?,
        shieldHit: Boolean,
        damageResult: ApplyDamageResultAPI?,
        engine: CombatEngineAPI?
    ) {
        if (projectile == null || target == null || point == null) return

        val drone = (projectile.customData[DRONE_DATA_KEY] as ShipAPI)
        val flux = drone.fluxTracker.fluxLevel
        val efficacy = MPC_bullrushScript.getEfficacy(flux)
        if (efficacy <= 0f) return
        val damage = projectile.damage

        val engine = Global.getCombatEngine()

        var arcsLeft = EMP_ARCS
        val targetHardFlux = (target as? ShipAPI)?.hardFluxLevel ?: 0f
        while (arcsLeft-- > 0f) {

            val pierce = niko_MPC_mathUtils.prob((targetHardFlux * 100f) / 2f)

            if (pierce) {
                engine.spawnEmpArcPierceShields(
                    projectile.source,
                    point,
                    target,
                    target,
                    damage.type,
                    damage.damage * DAMAGE_MULT,
                    (damage.fluxComponent * damage.modifier.modified) * EMP_MULT,
                    100000f,
                    "tachyon_lance_emp_impact",
                    50f * efficacy,
                    Color(25, 100, 155, 255),
                    Color(255, 255, 255, 255)
                )
            } else {
                 engine.spawnEmpArc(
                    projectile.source,
                    point,
                    target,
                    target,
                    damage.type,
                    damage.damage * DAMAGE_MULT,
                     (damage.fluxComponent * damage.modifier.modified) * EMP_MULT,
                    100000f,
                    "tachyon_lance_emp_impact",
                    50f * efficacy,
                    Color(25, 100, 155, 255),
                    Color(255, 255, 255, 255)
                )
            }
        }
    }
}
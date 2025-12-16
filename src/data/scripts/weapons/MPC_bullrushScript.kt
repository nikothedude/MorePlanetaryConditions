package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.input.InputEventAPI

class MPC_bullrushScript(): MPC_shieldMissileScript() {

    companion object {
        const val MAX_FLUX_FOR_EFFECT = 0.7f

        fun getEfficacy(fluxLevel: Float): Float = (fluxLevel / MAX_FLUX_FOR_EFFECT).coerceAtMost(1f)
    }

    override val id: String = "MPC_bullrush"

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI) {
        super.onFire(projectile, weapon, engine)

        Global.getCombatEngine().addPlugin(MPC_bullrushEveryframe(projectile as MissileAPI))
    }

    class MPC_bullrushEveryframe(val missile: MissileAPI): BaseEveryFrameCombatPlugin() {
        fun getDrone(): ShipAPI = (missile.customData[DRONE_DATA_KEY] as ShipAPI)

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            val drone = getDrone()
            val shield = drone.shield
            val fluxLevel = drone.fluxTracker.fluxLevel
            val efficacy = getEfficacy(fluxLevel)

            missile.damage.modifier.modifyMult("MPC_bullrushOverload", efficacy.coerceAtLeast(0.1f))

            drone.isJitterShields = true
            drone.setJitter(
                "MPC_bullrushOverload",
                drone.shield.innerColor,
                (efficacy * 3f),
                (9 * efficacy).toInt(),
                5f * efficacy
            )
            drone.setJitterUnder(
                "MPC_bullrushOverloadTwo",
                drone.shield.innerColor,
                (efficacy * 3f),
                (3 * efficacy).toInt(),
                2f * efficacy
            )
            missile.setJitter(
                "MPC_bullrushOverloadThree",
                drone.shield.innerColor,
                (efficacy * 3f),
                (9 * efficacy).toInt(),
                5f * efficacy
            )
        }

    }

    override fun getShieldType(): ShieldAPI.ShieldType {
        return ShieldAPI.ShieldType.FRONT
    }

    override fun getShieldArc(): Float {
        return 120f
    }

    override fun getFluxCapacity(): Float {
        return 1900f
    }

    override fun getFluxDissipation(): Float {
        return 200f
    }

    override fun getShieldRadius(missile: DamagingProjectileAPI) = missile.collisionRadius * 2.3f
}
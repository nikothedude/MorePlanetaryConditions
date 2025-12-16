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
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class MPC_towerScript() : MPC_shieldMissileScript() {

    override val id: String = "MPC_tower"

    override fun getShieldType(): ShieldAPI.ShieldType {
        return ShieldAPI.ShieldType.FRONT
    }

    override fun createShieldAndFlux(drone: ShipAPI, projectile: DamagingProjectileAPI): ShieldAPI {
        val shield = super.createShieldAndFlux(drone, projectile)

        shield.radius *= 2.5f
        drone.collisionRadius = shield.radius * 1.1f

        return shield
    }

    override fun getShieldArc(): Float {
        return 200f
    }

    override fun getFluxCapacity(): Float {
        return 20000f * getShieldEff()
    }

    override fun getShieldEff(): Float {
        return 0.5f
    }

    override fun getFluxDissipation(): Float {
        return 200f
    }

    override fun getHardFluxFraction(): Float {
        return 0.75f
    }

    override fun getShieldUnfoldMult(): Float {
        return 2f
    }

    override fun getShieldRadius(missile: DamagingProjectileAPI): Float {
        return missile.collisionRadius * 5f
    }

    override fun getShieldOuterColor(): Color? {
        return Color(255, 255, 255, 255)
    }

    override fun getShieldInnerColor(): Color? {
        return Color(255, 125, 125, 75)
    }

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI) {
        super.onFire(projectile, weapon, engine)

        Global.getCombatEngine().addPlugin(TowerMIRVScript(projectile as MissileAPI))
    }

    class TowerMIRVScript(val missile: MissileAPI): BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)
            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            val drone = missile.customData[DRONE_DATA_KEY] as ShipAPI
            if (!drone.fluxTracker.isOverloaded) return

            // SUICIDE TIME WOO

            missile.engineStats.acceleration.modifyFlat("MPC_towerSuicide", 100f)
            missile.engineStats.maxSpeed.modifyFlat("MPC_towerSuicide", 250f)
            missile.glowRadius = 15f
            missile.isRenderGlowAbove = false

            missile.damage.modifier.modifyFlat("MPC_towerSuicide", 3f)

            Global.getSoundPlayer().playSound(
                "reaper_fire",
                1f,
                1f,
                missile.location,
                Misc.ZERO
            )

            engine.removePlugin(this)
        }
    }
}
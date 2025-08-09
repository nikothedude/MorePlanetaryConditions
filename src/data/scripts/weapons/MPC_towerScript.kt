package data.scripts.weapons

import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
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
        return 15000f * getShieldEff()
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

    override fun getShieldOuterColor(): Color? {
        return Color(255, 255, 255, 255)
    }

    override fun getShieldInnerColor(): Color? {
        return Color(255, 125, 125, 75)
    }
}
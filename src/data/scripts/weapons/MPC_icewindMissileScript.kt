package data.scripts.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.combat.dem.DEMEffect

class MPC_icewindMissileScript: MPC_shieldMissileScript() {

    override val id: String = "MPC_icewindMissile"

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI) {
        super.onFire(projectile, weapon, engine)

        DEMEffect().onFire(projectile, weapon, engine)
    }

    override fun getShieldType(): ShieldAPI.ShieldType {
        return ShieldAPI.ShieldType.OMNI
    }

    override fun getShieldArc(): Float {
        return 120f
    }

    override fun getFluxCapacity(): Float {
        return 2000f
    }

    override fun getFluxDissipation(): Float {
        return 500f
    }
}
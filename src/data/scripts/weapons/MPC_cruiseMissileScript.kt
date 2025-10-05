package data.scripts.weapons

import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.WeaponAPI

class MPC_cruiseMissileScript: MPC_pinprickScript() {
    override var distNeeded: Float = 30f

    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        super.onFire(projectile, weapon, engine)

        val missile = projectile as? MissileAPI ?: return
        missile.empResistance = 10
    }
}
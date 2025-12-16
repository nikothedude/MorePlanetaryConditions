package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import java.awt.Color

class MPC_immortalSnailHmod: BaseHullMod() {

    companion object {
        val JITTER_COLOR = Color(255, 100, 185, 100)
        val JITTER_COLOR_UNDER = Color(255, 23, 255, 100)
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        super.advanceInCombat(ship, amount)

        if (ship == null) return

        for (weapon in ship.allWeapons) {
            weapon.ammo = weapon.maxAmmo // heeeeheeeee
            /*if (weapon.cooldownRemaining > 0f) {
                weapon.setRemainingCooldownTo(0f)
            }*/
        }
        ship.system.ammo = ship.system.maxAmmo
        ship.system.cooldownRemaining = 0f
        ship.setJitter(
            spec.id,
            JITTER_COLOR,
            3f,
            10,
            20f
        )
        ship.setJitterUnder(
            spec.id,
            JITTER_COLOR_UNDER,
            2f,
            6,
            15f
        )
        ship.isJitterShields = true

        if (ship.currentCR <= 0f) {
            ship.mutableStats.weaponMalfunctionChance.modifyMult(spec.id, 5f)
            ship.mutableStats.engineMalfunctionChance.modifyMult(spec.id, 5f)
            ship.mutableStats.shieldMalfunctionChance.modifyMult(spec.id, 5f)
            ship.mutableStats.criticalMalfunctionChance.modifyMult(spec.id, 5f)
        }
    }
}
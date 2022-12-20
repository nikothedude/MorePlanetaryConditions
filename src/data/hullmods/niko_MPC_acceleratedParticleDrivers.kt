package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class niko_MPC_acceleratedParticleDrivers: BaseHullMod() {

    val speedMult = 5f

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)

        if (stats == null || id == null) return

    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)

        if (ship == null || id == null) return
        for (weapon in ship.allWeapons) {
            if (!weapon.isBeam) return
            weapon.spec.speedStr
        }
    }

}
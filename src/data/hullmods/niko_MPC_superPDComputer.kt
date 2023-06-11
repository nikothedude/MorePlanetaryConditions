package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.utilities.niko_MPC_battleUtils.isPD

class niko_MPC_superPDComputer : BaseHullMod() {

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)

        val weapons: List<WeaponAPI> = ship!!.allWeapons
        val iter = weapons.iterator()
        while (iter.hasNext()) {
            val weapon = iter.next()
            if (weapon.isPD()) continue
            if (weapon.size == WeaponAPI.WeaponSize.SMALL) {
                weapon.setPD(true)
            } else {
                weapon.setPDAlso(true) //CHAOS
            }
        }
    }
}
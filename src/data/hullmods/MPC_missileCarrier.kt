package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI.HullSize

class MPC_missileCarrier: BaseHullMod() {
    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        if (index == 0) {
            return "cruise missile"
        }
        if (index == 1) {
            return "missile strike"
        }

        return null
    }
}
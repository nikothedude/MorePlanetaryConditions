package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI

class niko_MPC_fighterFocus: BaseHullMod() {

    val fighterBayBuff = 20f

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {

        if (stats == null || id == null) return
        stats.numFighterBays.modifyFlat(id, fighterBayBuff)

        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
    }

}
package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.econ.industries.MPC_coronaResistStructure

class niko_MPC_coronaResistShielding: BaseHullMod() {

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        if (hullSize == null || stats == null || id == null) return

        stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, MPC_coronaResistStructure.coronaResistance)
    }
}
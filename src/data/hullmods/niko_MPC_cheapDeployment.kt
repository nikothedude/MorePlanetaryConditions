package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats

class niko_MPC_cheapDeployment : BaseHullMod() {

    companion object {
        const val deploymentMult = 0f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, deploymentMult)
    }
}
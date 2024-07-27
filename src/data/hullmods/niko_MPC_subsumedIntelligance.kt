package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags

class niko_MPC_subsumedIntelligance: BaseHullMod() {

    companion object {
        const val TOP_SPEED_MULT = 1.3f
        const val TURN_RATE_MULT = 1.8f

        const val FLUX_CAPACITY_MULT = 2.2f
        const val DP_MULT = 1.4f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)

        if (stats == null || id == null) return

        stats.maxSpeed.modifyMult(id, TOP_SPEED_MULT)
        stats.maxTurnRate.modifyMult(id, TURN_RATE_MULT)
        stats.fluxCapacity.modifyMult(id, FLUX_CAPACITY_MULT)

        stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, DP_MULT)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
    }
}
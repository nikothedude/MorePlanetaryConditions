package data.scripts.shipsystems

import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData

class MPC_acceleratedOrdnanceFeeder : BaseShipSystemScript() {

    companion object {
        const val ROF_BONUS = 2.5f
        const val FLUX_REDUCTION = 75f
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        val mult = 1f + ROF_BONUS * effectLevel
        stats.ballisticRoFMult.modifyMult(id, mult)
        stats.ballisticWeaponFluxCostMod.modifyMult(id, 1f - FLUX_REDUCTION * 0.01f)
        stats.energyRoFMult.modifyMult(id, mult)
        stats.energyWeaponFluxCostMod.modifyMult(id, 1f - FLUX_REDUCTION * 0.01f)
        stats.missileRoFMult.modifyMult(id, mult)
        stats.missileWeaponFluxCostMod.modifyMult(id, 1f - FLUX_REDUCTION * 0.01f)

//		ShipAPI ship = (ShipAPI)stats.getEntity();
//		ship.blockCommandForOneFrame(ShipCommand.FIRE);
//		ship.setHoldFireOneFrame(true);
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        stats.ballisticRoFMult.unmodify(id)
        stats.ballisticWeaponFluxCostMod.unmodify(id)
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        val mult = 1f + ROF_BONUS * effectLevel
        val bonusPercent = ((mult - 1f) * 100f).toInt().toFloat()
        if (index == 0) {
            return StatusData("all weapon rate of fire +" + bonusPercent.toInt() + "%", false)
        }
        return if (index == 1) {
            StatusData("all weapon flux use -" + FLUX_REDUCTION.toInt() + "%", false)
        } else null
    }
}
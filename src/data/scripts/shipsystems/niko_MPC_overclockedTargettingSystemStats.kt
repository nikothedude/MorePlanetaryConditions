package data.scripts.shipsystems

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript

class niko_MPC_overclockedTargettingSystemStats: BaseShipSystemScript() {

    companion object publicStats {
        val rangeMult = 10f
    }
    val overloadTime = 12f

    override fun apply(stats: MutableShipStatsAPI?, id: String?, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        super.apply(stats, id, state, effectLevel)

        if (id == null || stats == null) return

        stats.energyWeaponRangeBonus.modifyMult(id, rangeMult*effectLevel)
        stats.ballisticWeaponRangeBonus.modifyMult(id, rangeMult*effectLevel)
        stats.missileWeaponRangeBonus.modifyMult(id, rangeMult*effectLevel)
    }

    override fun unapply(stats: MutableShipStatsAPI?, id: String?) {
        super.unapply(stats, id)

        if (id == null || stats == null) return

        stats.energyWeaponRangeBonus.unmodifyMult(id)
        stats.ballisticWeaponRangeBonus.unmodifyMult(id)
        stats.missileWeaponRangeBonus.unmodifyMult(id)

        if (Global.getCurrentState() != GameState.COMBAT) return

        val ship = stats.entity
        if (ship is ShipAPI) {
            Global.getCombatEngine().addPlugin(niko_MPC_overclockedTargettingSystemOverloadScript(ship, overloadTime))
        }
    }
}
package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods

class MPC_toggleableAutomationHandler: BaseHullMod() {
    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)

        val variant = stats!!.variant

        val enabled = id == "MPC_toggleAutomationEnabledHandler"
        val hasEnabled = variant.hasHullMod("MPC_toggleAutomationEnabled")
        val hasDisabled = variant.hasHullMod("MPC_toggleAutomationDisabled")

        if (enabled && !hasEnabled) {
            variant.removePermaMod(id)
            variant.addPermaMod("MPC_toggleAutomationDisabledHandler")
            variant.addMod("MPC_toggleAutomationDisabled")
        } else if (!enabled && !hasDisabled) {
            variant.removePermaMod(id)
            variant.addPermaMod("MPC_toggleAutomationEnabledHandler")
            variant.addMod("MPC_toggleAutomationEnabled")
        }
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String? {
        return super.getUnapplicableReason(ship)
    }
}
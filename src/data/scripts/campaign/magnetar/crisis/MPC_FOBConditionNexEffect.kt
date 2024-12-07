package data.scripts.campaign.magnetar.crisis

import exerelin.campaign.intel.groundbattle.plugins.BaseGroundBattlePlugin

class MPC_FOBConditionNexEffect: BaseGroundBattlePlugin() {

    companion object {
        const val id = "MPC_FOBConditionNexEffect"

        const val DMG_MULT = 0.35f
    }

    override fun apply() {
        super.apply()

        intel.getSide(true).damageTakenMod.modifyFlat(id, DMG_MULT)
        intel.getSide(false).damageTakenMod.modifyFlat(id, DMG_MULT)
    }

    override fun unapply() {
        super.unapply()

        intel.getSide(true).damageTakenMod.unmodify(id)
        intel.getSide(false).damageTakenMod.unmodify(id)
    }
}
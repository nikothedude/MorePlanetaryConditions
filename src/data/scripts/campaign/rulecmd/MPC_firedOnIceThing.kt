package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

class MPC_firedOnIceThing: BaseCommandPlugin() {

    companion object {
        const val CR_DRAIN = 0.3f
        const val DAMAGE = 65f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        Global.getSoundPlayer().playUISound("MPC_rapidEMP", 1f, 1f)
        damageShips()

        return true
    }

    private fun damageShips() {
        val playerFleet = Global.getSector().playerFleet ?: return
        for (member in playerFleet.fleetData.membersListCopy) {
            val repairTracker = member.repairTracker
            repairTracker.cr *= (1 - CR_DRAIN)
            member.status.applyDamage(DAMAGE)
        }
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited

class niko_MPC_escortFleet: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (ruleId == null || dialog == null || params == null || memoryMap == null) return false

        val interactionTarget = dialog.interactionTarget ?: return false
        val command = params[0].getString(memoryMap) ?: return false
        if (command == "marketInhabited") {
            val market = interactionTarget.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] as? MarketAPI ?: return false
            return market.isInhabited()
        } else if (command == "fleetIsEscort") {
            return interactionTarget.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] != null
        }

        return true
    }
}
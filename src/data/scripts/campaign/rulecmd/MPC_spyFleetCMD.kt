package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_spyFleetScript
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignmentDeliverResourcesToCache
import data.utilities.niko_MPC_ids

class MPC_spyFleetCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false
        val command = params[0].getString(memoryMap)
        val fleet = dialog.interactionTarget as CampaignFleetAPI
        val spyScript: MPC_spyFleetScript = (fleet.scripts.firstOrNull { it is MPC_spyFleetScript } ?: return false) as MPC_spyFleetScript
        val assignment = spyScript.assignment

        when (command) {
            "isDeliveryFleet" -> {
                return (assignment is MPC_spyAssignmentDeliverResourcesToCache)
            }
            "playerSawDeliverCargo" -> {
                if (assignment !is MPC_spyAssignmentDeliverResourcesToCache) return false

                return assignment.playerSawCache
            }
        }

        return true
    }
}
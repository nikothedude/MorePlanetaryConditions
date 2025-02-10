package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.derelictEscort.MPC_derelictEscortAssignmentAI
import data.scripts.campaign.econ.conditions.derelictEscort.niko_MPC_derelictEscort
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getEscortFleetList
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero

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

        when (command) {
            "marketInhabited" -> {
                val market = interactionTarget.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] as? MarketAPI ?: return false
                return market.isInhabited()
            }
            "fleetIsEscort" -> {
                return interactionTarget.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] != null
            }
            "isEscortingPlayer" -> {
                val market = interactionTarget.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] as? MarketAPI ?: return false
                val list = market.getEscortFleetList() ?: return false
                if (list[Global.getSector().playerFleet] == interactionTarget) return true
                return false
            }
            "getTimeLeft" -> {
                val assigmentAI = MPC_derelictEscortAssignmentAI.get(interactionTarget as CampaignFleetAPI) ?: return false
                Global.getSector().memoryWithoutUpdate["\$MPC_escortTimeLeft"] = (assigmentAI.interval.intervalDuration - assigmentAI.interval.elapsed).roundNumTo(1).trimHangingZero()
            }
            "isReturning" -> {
                return (interactionTarget as? CampaignFleetAPI)?.currentAssignment?.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
            }
            "dismissFleet" -> {
                val assigmentAI = MPC_derelictEscortAssignmentAI.get(interactionTarget as CampaignFleetAPI) ?: return false
                assigmentAI.abortAndReturnToBase()
            }
        }

        return true
    }
}
package data.scripts.campaign.magnetar.crisis.contribution

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids

data class MPC_factionContribution(
    val factionId: String,
    val fleetMultIncrement: Float,
    val removeContribution: ((IAIIC: FactionAPI) -> Unit)?,
    val removeNextAction: Boolean = false,
    val requireMilitary: Boolean = false,
    val repOnRemove: Float? = null
) {

    fun onRemoved(intel: MPC_IAIICFobIntel, becauseFactionDead: Boolean, dialog: InteractionDialogAPI? = null) {
        if (removeNextAction) {
            intel.removeNextAction()
        }
        val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
        if (IAIIC != null) {
            removeContribution?.let { it(IAIIC) }
            intel.removeBlueprintFunctions.add { removeContribution }
        }

        val reason = if (becauseFactionDead) MPC_changeReason.FACTION_DIED else MPC_changeReason.PULLED_OUT
        val data = MPC_factionContributionChangeData(this, reason, true)
        if (dialog != null) {
            intel.sendUpdate(data, dialog.textPanel)
        } else {
            intel.sendUpdateIfPlayerHasIntel(data, false)
        }

        if (becauseFactionDead) return

        if (repOnRemove != null) {
            val ourFac = Global.getSector().getFaction(factionId)

            if (IAIIC != null && ourFac != null) {
                ourFac.setRelationship(niko_MPC_ids.IAIIC_FAC_ID, repOnRemove)
            }
        }
    }

    fun remove(dialog: InteractionDialogAPI?, becauseFactionDead: Boolean) {
        val intel = MPC_IAIICFobIntel.get() ?: return
        intel.removeContribution(this, becauseFactionDead, dialog)
    }

    fun getStringifiedFleetsize(): String {
        return "${(fleetMultIncrement)}x"
    }
}
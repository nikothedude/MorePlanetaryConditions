package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.RepLevel
import data.utilities.niko_MPC_ids

data class MPC_factionContribution(
    val factionId: String,
    val fleetMult: Float,
    val requireMilitary: Boolean = false,
    val repOnRemove: Float? = null
) {

    fun onRemoved(becauseFactionDead: Boolean) {
        if (becauseFactionDead) return

        if (repOnRemove != null) {
            val IAIIC = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID) ?: return
            val ourFac = Global.getSector().getFaction(factionId) ?: return

            ourFac.setRelationship(niko_MPC_ids.IAIIC_FAC_ID, repOnRemove)
        }
    }
}
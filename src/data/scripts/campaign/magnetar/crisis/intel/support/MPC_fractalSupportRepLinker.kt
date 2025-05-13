package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids

class MPC_fractalSupportRepLinker(
    val factionId: String,
    val supportIntel: MPC_fractalCrisisSupport
): BaseCampaignEventListener(false) {

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        if (faction == null || faction != niko_MPC_ids.IAIIC_FAC_ID) return
        if (supportIntel.state == MPC_fractalCrisisSupport.State.SUSPENDED_DUE_TO_HOSTILITIES) return

        val ourFac = Global.getSector().getFaction(factionId) ?: return
        ourFac.setRelationship(faction, Global.getSector().playerFaction.getRelationship(faction))
    }
}
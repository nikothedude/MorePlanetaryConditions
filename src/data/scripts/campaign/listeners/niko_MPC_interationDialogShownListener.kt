package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import data.scripts.campaign.plugins.niko_MPC_satelliteFleetInteractionDialogPlugin

class niko_MPC_interationDialogShownListener: BaseCampaignEventListener(false) {

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI?) {
        super.reportShownInteractionDialog(dialog)

        /*if (dialog == null) return

        if (dialog.plugin is FleetInteractionDialogPluginImpl) {
            if (dialog.plugin.)
            dialog.plugin = niko_MPC_satelliteFleetInteractionDialogPlugin()
        }*/
    }

}
package data.scripts.campaign.listeners

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI

class niko_MPC_pickFleetAIListener: BaseCampaignEventListener(false) {

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        super.reportFleetSpawned(fleet)
    }

}
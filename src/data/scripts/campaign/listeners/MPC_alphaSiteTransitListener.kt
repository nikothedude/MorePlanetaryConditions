package data.scripts.campaign.listeners

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI
import com.fs.starfarer.api.campaign.SectorEntityToken

class MPC_alphaSiteTransitListener: BaseCampaignEventListener(false) {

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        super.reportFleetJumped(fleet, from, to)

        if (fleet != Global.getSector().playerFleet) return
        if (to == null || to.destination.containingLocation?.id != "Unknown Location") return

        Global.getSector().memoryWithoutUpdate["\$MPC_playerWentToAlphaSite"] = true
    }

}
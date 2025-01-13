package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetActionTextProvider
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI

class MPC_IAIICPatrolAssignmentAI(fleet: CampaignFleetAPI, route: RouteManager.RouteData): RouteFleetAssignmentAI(fleet, route), FleetActionTextProvider {
    override fun getActionText(fleet: CampaignFleetAPI?): String {
        TODO("Not yet implemented")
    }
}
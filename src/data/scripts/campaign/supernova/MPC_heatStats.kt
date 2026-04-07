package data.scripts.campaign.supernova

import com.fs.starfarer.api.campaign.CampaignFleetAPI

class MPC_heatStats(
    val fleet: CampaignFleetAPI,
    /// 0-1
    var heatLevel: Float
) {

    companion object {
        const val MAX_HEAT_KELVIN = 3565f
        const val RESTING_HEAT_KELVIN = 30f
    }

}
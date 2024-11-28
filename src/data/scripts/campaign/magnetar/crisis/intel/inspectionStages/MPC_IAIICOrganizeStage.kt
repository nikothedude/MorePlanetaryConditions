package data.scripts.campaign.magnetar.crisis.intel.inspectionStages

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.raid.OrganizeStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel

class MPC_IAIICOrganizeStage(raid: RaidIntel?, market: MarketAPI?, durDays: Float) : OrganizeStage(raid, market,
    durDays
) {

    override fun getForcesString(): String? {
        return "The inspection task force"
    }

    override fun getRaidString(): String? {
        return "inspection"
    }
}

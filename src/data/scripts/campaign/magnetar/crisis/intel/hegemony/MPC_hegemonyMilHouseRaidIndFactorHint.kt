package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TTCRIndustryDisruptedFactorHint

class MPC_hegemonyMilHouseRaidIndFactorHint() : MPC_hegemonyMilHouseRaidIndFactor(0, "Industries on Hegemony colonies disrupted") {

    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return !hasOtherFactorsOfClass(intel, MPC_hegemonyMilHouseRaidIndFactor::class.java)
    }

}
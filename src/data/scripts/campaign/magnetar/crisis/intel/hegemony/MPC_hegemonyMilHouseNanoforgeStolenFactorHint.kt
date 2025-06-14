package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel

class MPC_hegemonyMilHouseNanoforgeStolenFactorHint : MPC_hegemonyMilHouseNanoforgeStolenFactor(0, "Industries on Hegemony colonies disrupted") {

    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return !hasOtherFactorsOfClass(intel, MPC_hegemonyMilHouseNanoforgeStolenFactor::class.java)
    }
}
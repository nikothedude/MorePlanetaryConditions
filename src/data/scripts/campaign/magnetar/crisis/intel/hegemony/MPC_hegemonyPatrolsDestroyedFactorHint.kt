package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TTCRTradeFleetsDestroyedFactor

class MPC_hegemonyPatrolsDestroyedFactorHint(): MPC_hegemonyPatrolsDestroyedFactor(0) {

    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return isActive() && !hasOtherFactorsOfClass(intel, MPC_hegemonyPatrolsDestroyedFactor::class.java)
    }
}
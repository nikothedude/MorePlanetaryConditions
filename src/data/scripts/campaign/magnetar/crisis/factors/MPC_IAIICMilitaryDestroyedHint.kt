package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TTCRCommerceRaidersDestroyedFactor

class MPC_IAIICMilitaryDestroyedHint() : MPC_IAIICMilitaryDestroyedFactor(0) {

    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return !hasOtherFactorsOfClass(intel, MPC_IAIICMilitaryDestroyedFactor::class.java)
    }
}
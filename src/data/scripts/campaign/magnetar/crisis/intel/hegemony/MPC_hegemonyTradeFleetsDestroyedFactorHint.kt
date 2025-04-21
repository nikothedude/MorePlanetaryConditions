package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel

class MPC_hegemonyTradeFleetsDestroyedFactorHint: MPC_hegemonyTradeFleetsDestroyedFactor(0) {

    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return !hasOtherFactorsOfClass(intel, MPC_hegemonyTradeFleetsDestroyedFactor::class.java)
    }

}
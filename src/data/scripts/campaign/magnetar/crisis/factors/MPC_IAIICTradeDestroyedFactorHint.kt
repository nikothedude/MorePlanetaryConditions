package data.scripts.campaign.magnetar.crisis.factors

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel

class MPC_IAIICTradeDestroyedFactorHint() : MPC_IAIICTradeDestroyedFactor(0) {
    init {
        timestamp = 0
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return !hasOtherFactorsOfClass(intel, MPC_IAIICTradeDestroyedFactorHint::class.java)
    }
}
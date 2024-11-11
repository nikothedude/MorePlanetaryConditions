package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel

class MPC_hegemonyFractalCoreCauseOne(intel: HostileActivityEventIntel?) : MPC_hegemonyFractalCoreCause(intel) {
    override var preDefeat: Boolean = true
}
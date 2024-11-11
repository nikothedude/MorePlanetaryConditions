package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel

// put on the 2nd factor thats only active one the first hegemony crisis ends
class MPC_hegemonyFractalCoreCauseTwo(intel: HostileActivityEventIntel?) : MPC_hegemonyFractalCoreCause(intel) {
    override var preDefeat: Boolean = false
}
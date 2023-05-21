package data.scripts.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel

abstract class baseNikoEventIntelPlugin: BaseEventIntel() {

    open fun delete() {
        ended = true
        hidden = true

        endImmediately()

        Global.getSector().intelManager.removeIntel(this)
        Global.getSector().listenerManager.removeListener(this)
    }
}
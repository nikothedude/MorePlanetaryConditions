package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel

abstract class baseNikoEventFactor: BaseEventFactor() {
    abstract fun delete()
}
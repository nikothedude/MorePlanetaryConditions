package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventFactor
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import kotlin.time.measureTime

abstract class baseNikoEventFactor(
    open val ourIntel: baseNikoEventIntelPlugin
): BaseEventFactor() {

    open fun init(): baseNikoEventFactor {
        addSelfToIntel()
        return this
    }

    fun addSelfToIntel() {
        if (!duplicatesAllowed() && ourIntel.hasFactorOfClass(javaClass)) {
            return delete()
        }
        ourIntel.addFactor(this)
    }

    open fun duplicatesAllowed(): Boolean = false
    abstract fun delete()
}
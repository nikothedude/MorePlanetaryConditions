package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain

abstract class overgrownNanoforgeIntelStage(
    val brain: overgrownNanoforgeSpreadingBrain,
    val intel: baseOvergrownNanoforgeIntel
) {

    abstract fun getName(): String
    open fun getDesc(): String? = null

    abstract fun stageReached()
    open fun getTooltip(data: EventStageData): TooltipCreator? {
        val desc = getDesc() ?: return null
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                val opad = 10f
                if (tooltip == null) return
                tooltip.addTitle(getName())
                tooltip.addPara(desc, opad)
                data.addProgressReq(tooltip, opad)
            }
        }
    }
    open fun getIconId(): String? = null
}

class overgrownNanoforgeIntelDummyStartingStage(brain: overgrownNanoforgeSpreadingBrain, intel: baseOvergrownNanoforgeIntel):
    overgrownNanoforgeIntelStage(brain, intel) {
    override fun getName(): String = "Start"
    override fun stageReached() { return }
}
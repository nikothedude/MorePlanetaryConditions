package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.scripts.campaign.intel.baseNikoEventStage
import org.jetbrains.annotations.Contract

abstract class overgrownNanoforgeIntelStage(
    override val intel: baseOvergrownNanoforgeIntel
): baseNikoEventStage(intel) {
}

class overgrownNanoforgeIntelDummyStartingStage(intel: baseOvergrownNanoforgeIntel):
    overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Start"
    override fun stageReached() { return }

    override fun getThreshold(): Int = 0
    override fun isOneOffEvent(): Boolean = false

    override fun hideIconWhenComplete(): Boolean = false
    override fun keepIconBrightWhenComplete(): Boolean = false
}
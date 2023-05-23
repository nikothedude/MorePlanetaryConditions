package data.scripts.campaign.intel

import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.jetbrains.annotations.Contract

abstract class baseNikoEventStage(
    open val intel: baseNikoEventIntelPlugin
) {

    /** The title of the tooltip. */
    abstract fun getName(): String
    /** The body of the tooltip. */
    open fun getDesc(): String? = null

    abstract fun stageReached()
    open fun getTooltip(data: BaseEventIntel.EventStageData): TooltipMakerAPI.TooltipCreator? {
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
    open fun getIconId(): String = intel.icon

    /** Should always return this. */
    @Contract("_ -> this")
    open fun init(): baseNikoEventStage {
        addSelfToIntel()
        return this
    }

    open fun delete() {
        intel.removeStage(this)
    }

    open fun addSelfToIntel() {
        if (!duplicatesAllowed() && intel.hasStageOfClass(this.javaClass)) {
            return delete()
        }
        intel.addStage(this, getThreshold(), isOneOffEvent(), getIconSize())
        val data = intel.getDataFor(this) ?: return
        modifyStageData(data)
    }

    open fun modifyStageData(data: BaseEventIntel.EventStageData) {
        data.keepIconBrightWhenLaterStageReached = keepIconBrightWhenComplete()

        data.hideIconWhenPastStageUnlessLastActive = hideIconWhenComplete()
        data.sendIntelUpdateOnReaching = doWeReportWhenReached()
    }
    open fun doWeReportWhenReached(): Boolean = true
    /** If false, we check to see if intel has a stage of our class. If yes, we don't apply ourselves.*/
    fun duplicatesAllowed(): Boolean = false
    open fun hideIconWhenComplete(): Boolean = true
    open fun keepIconBrightWhenComplete(): Boolean = false
    open fun isOneOffEvent(): Boolean = false

    abstract fun getThreshold(): Int
    fun updateThreshold() {
        val data = intel.getDataFor(this) ?: return
        data.progress = getThreshold()
    }

    open fun getIconSize(): BaseEventIntel.StageIconSize? {
        return BaseEventIntel.StageIconSize.MEDIUM
    }
}
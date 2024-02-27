package data.scripts.campaign.intel

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.jetbrains.annotations.Contract
import java.awt.Color

interface baseNikoEventStageInterface<T: baseNikoEventIntelPlugin> {
    /** The title of the tooltip. */
    fun getName(): String
    /** The body of the tooltip. */
    fun getDesc(): String? = null

    /**
     * If the intel with this stage is a subtype of [baseNikoEventIntelPlugin], this will be called when [intel]'s progress
     * surpasses [getThreshold].
     */
    fun stageReached(intel: T)
    /**
     * Adds the hover-over tooltip to the stage icon. By default, adds title as name, para as desc, and adds a progress req.
     */
    fun getTooltip(data: BaseEventIntel.EventStageData): TooltipMakerAPI.TooltipCreator? {
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
    /**
     * Returns the string ID of our icon.
     */
    fun getIconId(intel: T): String = intel.icon

    /**
     * Should always return this.
     * Should be used for calling non-final functions important to our initialization.
     */
    @Contract("_ -> this")
    fun init(intel: T): baseNikoEventStageInterface<T> {
        addSelfToIntel(intel)
        return this
    }

    fun delete(intel: T) {
        intel.removeStage(this)
    }

    fun addSelfToIntel(intel: T) {
        if (!duplicatesAllowed() && intel.hasStageOfClass(this.javaClass)) {
            return delete(intel)
        }
        intel.addStage(this, getThreshold(intel), isOneOffEvent(), getIconSize())
        val data = intel.getDataFor(this) ?: return
        modifyStageData(data)
    }

    /**
     * Modifies the stagedata for our corresponding stagedata, if it exists.
     */
    fun modifyStageData(data: BaseEventIntel.EventStageData) {
        data.keepIconBrightWhenLaterStageReached = keepIconBrightWhenComplete()

        data.hideIconWhenPastStageUnlessLastActive = hideIconWhenComplete()
        data.sendIntelUpdateOnReaching = doWeReportWhenReached()
    }
    /**
     * Used in [modifyStageData]
     */
    fun doWeReportWhenReached(): Boolean = true
    /** If false, we check to see if intel has a stage of our class. If yes, we don't apply ourselves.*/
    fun duplicatesAllowed(): Boolean = false
    /**
     * Used in [modifyStageData]
     */
    fun hideIconWhenComplete(): Boolean = true
    /**
     * Used in [modifyStageData]
     */
    fun keepIconBrightWhenComplete(): Boolean = false

    /**
     * Used in [addSelfToIntel]
     */
    fun isOneOffEvent(): Boolean = false

    /** Returns the threshold of progress our intel must surpass for [stageReached] to be called. */
    fun getThreshold(intel: T): Int

    /**
     * Updates the progress req. for our data. Called whenever [intel] changes max progress.
     */
    fun updateThreshold(intel: T) {
        val data = intel.getDataFor(this) ?: return
        data.progress = getThreshold(intel)
    }

    /**
     * Returns the size of our icon.
     */
    fun getIconSize(): BaseEventIntel.StageIconSize? {
        return BaseEventIntel.StageIconSize.MEDIUM
    }

    fun modifyIntelUpdateWhenStageReached(
        info: TooltipMakerAPI,
        mode: IntelInfoPlugin.ListInfoMode?,
        tc: Color?,
        initPad: Float,
        intel: T
    ): Boolean {

        info.addPara("Stage reached: ${getName()}", initPad, Misc.getTextColor())
        return true
    }
}
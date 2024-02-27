package data.compatability

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeGrowthIntel
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import org.jetbrains.annotations.Contract
import java.awt.Color

@Deprecated("doesnt work on current SS version. remove after 3.0.2")
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

@Deprecated("doesnt work on current SS version. remove after 3.0.2")
abstract class baseNikoEventStage(
    open val intel: baseNikoEventIntelPlugin
) {

    /** The title of the tooltip. */
    abstract fun getName(): String
    /** The body of the tooltip. */
    open fun getDesc(): String? = null

    /**
     * If the intel with this stage is a subtype of [baseNikoEventIntelPlugin], this will be called when [intel]'s progress
     * surpasses [getThreshold].
     */
    abstract fun stageReached()
    /**
     * Adds the hover-over tooltip to the stage icon. By default, adds title as name, para as desc, and adds a progress req.
     */
    open fun getTooltip(data: BaseEventIntel.EventStageData): Any? {
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
    open fun getIconId(): String = intel.icon

    /**
     * Should always return this.
     * Should be used for calling non-final functions important to our initialization.
     */
    @Contract("_ -> this")
    open fun init(): baseNikoEventStage {
        //addSelfToIntel()
        return this
    }

    open fun delete() {
        //intel.removeStage(this)
    }

    /*open fun addSelfToIntel() {
        if (!duplicatesAllowed() && intel.hasStageOfClass(this.javaClass)) {
            return delete()
        }
        intel.addStage(this, getThreshold(), isOneOffEvent(), getIconSize())
        val data = intel.getDataFor(this) ?: return
        modifyStageData(data)
    }*/

    /**
     * Modifies the stagedata for our corresponding stagedata, if it exists.
     */
    open fun modifyStageData(data: BaseEventIntel.EventStageData) {
        data.keepIconBrightWhenLaterStageReached = keepIconBrightWhenComplete()

        data.hideIconWhenPastStageUnlessLastActive = hideIconWhenComplete()
        data.sendIntelUpdateOnReaching = doWeReportWhenReached()
    }
    /**
     * Used in [modifyStageData]
     */
    open fun doWeReportWhenReached(): Boolean = true
    /** If false, we check to see if intel has a stage of our class. If yes, we don't apply ourselves.*/
    fun duplicatesAllowed(): Boolean = false
    /**
     * Used in [modifyStageData]
     */
    open fun hideIconWhenComplete(): Boolean = true
    /**
     * Used in [modifyStageData]
     */
    open fun keepIconBrightWhenComplete(): Boolean = false

    /**
     * Used in [addSelfToIntel]
     */
    open fun isOneOffEvent(): Boolean = false

    /** Returns the threshold of progress our intel must surpass for [stageReached] to be called. */
    abstract fun getThreshold(): Int

    /**
     * Updates the progress req. for our data. Called whenever [intel] changes max progress.
     */
    fun updateThreshold() {
        val data = intel.getDataFor(this) ?: return
        data.progress = getThreshold()
    }

    /**
     * Returns the size of our icon.
     */
    open fun getIconSize(): BaseEventIntel.StageIconSize? {
        return BaseEventIntel.StageIconSize.MEDIUM
    }

    open fun modifyIntelUpdateWhenStageReached(
        info: TooltipMakerAPI,
        mode: IntelInfoPlugin.ListInfoMode?,
        tc: Color?,
        initPad: Float
    ): Boolean {

        info.addPara("Stage reached: ${getName()}", initPad, Misc.getTextColor())
        return true
    }
}

class overgrownNanoforgeFinishGrowthStage(override val intel: overgrownNanoforgeGrowthIntel)
    : overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Growth Finished"
    override fun getDesc(): String = "Once reached, the growth will become permanent and begin applying its effects."

    override fun stageReached() {
        intel.growingComplete()
    }
    override fun getThreshold(): Int = intel.maxProgress
}
package data.scripts.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.*
import org.jetbrains.annotations.Contract

abstract class baseNikoEventIntelPlugin: BaseEventIntel() {

    @Contract("_ -> this")
    open fun init(hidden: Boolean = true): baseNikoEventIntelPlugin {
        initializeProgress()

        addInitialStages()
        addInitialFactors()

        isHidden = hidden

        Global.getSector().intelManager.addIntel(this, true)
        return this
    }

    open fun initializeProgress() { return }

    open fun addInitialStages() {
        addStartStage()
        addEndStage()
    }

    open fun addStartStage() { return }
    open fun addEndStage() { return }

    open fun addInitialFactors() { return }

    open fun delete() {
        ended = true
        hidden = true

        endImmediately()

        Global.getSector().intelManager.removeIntel(this)
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return
        if (isStageActiveAndLast(stageId)) {
            addMiddleDescriptionText(info, width, stageId)
        }
    }

    open fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        addBasicDescription(info, width, stageId)
        return
    }

    open fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) { return }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)
        if (stage == null) return

        val id = stage.id
        if (id is baseNikoEventStage) id.stageReached()
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId == null) return null
        val data = getDataFor(stageId) ?: return null

        val id = data.id
        if (id is baseNikoEventStage) return id.getTooltip(data)

        return null
    }

    override fun getStageIconImpl(stageId: Any?): String {
        val defaultReturn = icon
        if (stageId == null) return defaultReturn
        val data = getDataFor(stageId) ?: return defaultReturn

        val id = data.id
        if (id is baseNikoEventStage) return id.getIconId() ?: defaultReturn

        return defaultReturn
    }

    fun removeStage(stage: baseNikoEventStage) {
        removeStages(setOf(stage))
    }

    fun removeStages(stagesToRemove: Set<baseNikoEventStage>) {
        val stagesCopy = stages.toSet()
        for (iteratedStage in stagesCopy) {
            val stageId = iteratedStage.id
            if (stagesToRemove.contains(stageId) && (stageId is baseNikoEventStage)) { //shouldve smart casted - but it didnt
                stageId.delete()
            }
        }
    }

    fun hasFactorOfClass(clazz: Class<baseNikoEventFactor>): Boolean {
        for (factor in factors) {
            if (factor.javaClass.isAssignableFrom(clazz)) return true
        }
        return false
    }

    fun hasStageOfClass(clazz: Class<baseNikoEventStage>): Boolean {
        for (stage in stages) {
            if (stage.javaClass.isAssignableFrom(clazz)) return true
        }
        return false
    }

    override fun setMaxProgress(maxProgress: Int) {
        super.setMaxProgress(maxProgress)

        updateStageThresholds()
    }

    open fun updateStageThresholds() {
        for (stage in stages) {
            val id = stage.id
            if (id !is baseNikoEventStage) continue
            id.updateThreshold()
        }
    }
}
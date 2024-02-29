package data.scripts.campaign.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.EventFactor
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.compatability.baseNikoEventStage
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.*
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeIntel
import data.utilities.niko_MPC_debugUtils
import org.jetbrains.annotations.Contract
import java.lang.Exception

abstract class baseNikoEventIntelPlugin: BaseEventIntel() {

    /**
     *  Exists to make it possible to call non-final methods on construction of this.
     *  Should always be called if an instance of this class is created.
     *
     *  @param hidden Default: True. isHidden is set to this.
     *
     *  @return this
     * */
    @Contract("_ -> this")
    open fun init(hidden: Boolean = true): baseNikoEventIntelPlugin {
        initializeProgress()

        addInitialStages()
        addInitialFactors()

        isHidden = hidden

        Global.getSector().intelManager.addIntel(this, true)
        return this
    }

    enum class enumsConvertedStatus {
        NO_NEED, // instantiated post 3.0.1
        NO,
        YES;
    }

    var enumsConverted: enumsConvertedStatus? = enumsConvertedStatus.NO_NEED

    open fun readResolve(): Any? {
        if (enumsConverted == null || enumsConverted == enumsConvertedStatus.NO) {
            try {
                convertToEnum()
                enumsConverted = enumsConvertedStatus.YES
            } catch (ex: Exception) {
                niko_MPC_debugUtils.log.error(ex)
            }
        }
        return this
    }

    private fun convertToEnum(): Boolean {
        var needToRegenerateStages = false
        for (stage: EventStageData in stages) {
            if (stage.id !is baseNikoEventStage) continue
            val stages = stages
            val stagesCopy = stages.toSet()
            for (iteratedStage in stagesCopy) {
                val stageId = iteratedStage.id
                if (stages.contains(stageId)) {
                    needToRegenerateStages = true
                    break
                }
            }
            if (needToRegenerateStages) {
                stages.clear()
                addInitialStages()
            }
        }
        return needToRegenerateStages
    }

    /**
     * Called in [init]. Sets max progress and current progress on initialization of the intel.
     */
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
        if (id is baseNikoEventStageInterface<*>) {
            val castedId = id as baseNikoEventStageInterface<baseNikoEventIntelPlugin>
            castedId.stageReached(this)
        }
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId == null) return null
        val data = getDataFor(stageId) ?: return null

        val id = data.id
        if (id is baseNikoEventStageInterface<*>) return id.getTooltip(data)

        return null
    }

    override fun getStageIconImpl(stageId: Any?): String {
        val defaultReturn = icon
        if (stageId == null) return defaultReturn
        val data = getDataFor(stageId) ?: return defaultReturn

        val id = data.id
        if (id is baseNikoEventStageInterface<*>) {
            val castedId = id as baseNikoEventStageInterface<baseNikoEventIntelPlugin>
            return castedId.getIconId(this)
        }
        return defaultReturn
    }

    fun removeStage(stage: baseNikoEventStageInterface<*>) {
        removeStages(setOf(stage))
    }

    fun removeStages(stagesToRemove: Set<baseNikoEventStageInterface<*>>) {
        val stagesCopy = stages.toSet()
        for (iteratedStage in stagesCopy) {
            val stageId = iteratedStage.id
            if (stagesToRemove.contains(stageId)) {
                stages.remove(stageId)
            }
            /*if (stagesToRemove.contains(stageId) && (stageId is baseNikoEventStage)) { //shouldve smart casted - but it didnt
                stageId.delete()
            }*/
        }
    }

    fun hasFactorOfClass(clazz: Class<EventFactor>): Boolean {
        for (factor in factors) {
            if (factor.javaClass.isAssignableFrom(clazz)) return true
        }
        return false
    }

    fun hasStageOfClass(clazz: Class<baseNikoEventStageInterface<*>>): Boolean {
        for (stage in stages) {
            if (stage.id.javaClass.isAssignableFrom(clazz)) return true
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
            if (id !is baseNikoEventStageInterface<*>) continue
            val castedId = id as baseNikoEventStageInterface<baseNikoEventIntelPlugin>
            castedId.updateThreshold(this)
        }
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode?) {
        val c = getTitleColor(mode)
        // i would love to do this modularly, alex, but youve fucking. forced my hand with a LOCAL VARIABLE
        if (isLargeIntel()) {
            info.setParaSmallInsignia()
        }
        info.addPara(name, c, 0f)
        if (isLargeIntel()) {
            info.setParaFontDefault()
        }

        addBulletPoints(info, mode)
    }

    /**
     * If true, the name of this intel, as shown in the intel tab (Not in the panel itself) will be large and bold, like
     * hostile activity.
     * */
    open fun isLargeIntel(): Boolean = false
}
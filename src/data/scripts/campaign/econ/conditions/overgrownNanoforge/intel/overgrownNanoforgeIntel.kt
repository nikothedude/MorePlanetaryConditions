package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import data.utilities.niko_MPC_ids.INTEL_OVERGROWN_NANOFORGES
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_NOT_SPREADING_PROGRESS

class overgrownNanoforgeIntel(
    val ourNanoforgeHandler: overgrownNanoforgeIndustryHandler,
    hidden: Boolean = true
): baseNikoEventIntelPlugin() {

    var spreading: Boolean = false
    var paramsToUse: overgrownNanoforgeRandomizedSourceParams? = null
    fun generateSourceParams(): overgrownNanoforgeRandomizedSourceParams {
        val chosenSourceType = getSourceType()
        return overgrownNanoforgeRandomizedSourceParams(ourNanoforgeHandler, chosenSourceType)
    }
    private fun getSourceType(): overgrownNanoforgeSourceTypes {
        return overgrownNanoforgeSourceTypes.adjustedPick() ?: overgrownNanoforgeSourceTypes.STRUCTURE
    }

    val spreadIntervalTimer = IntervalUtil(niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_TIME_BETWEEN_SPREADS, niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_TIME_BETWEEN_SPREADS)

    var baseFactor: overgrownNanoforgeBaseIntelFactor? = null
        get() {
            if (field == null) field = createBaseFactor()
            return field
        }

    private fun createBaseFactor(): overgrownNanoforgeBaseIntelFactor {
        return overgrownNanoforgeBaseIntelFactor(getBaseProgress(), this)
    }

    private fun getBaseProgress(): Int {
        return 1
    }

    enum class overgrownStages {
        START,
        JUNK_SPAWNED,
    }

    init {
        isHidden = hidden
        Global.getSector().intelManager.addIntel(this, false)

        stopSpreading() //TODO: this WILL fucking explode.
    }

    override fun advanceImpl(amount: Float) {
        if (!spreading) {
            val days = Misc.getDays(amount)
            spreadIntervalTimer.advance(days)
            if (spreadIntervalTimer.intervalElapsed()) {
                startSpreading()
            }
        }
        super.advanceImpl(amount)
    }

    fun startSpreading() {
        setMaxProgress(OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS)
        setProgress(getStartingProgress())

        addStage(overgrownStages.START, 0, false)
        addStage(overgrownStages.JUNK_SPAWNED, OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS, true, StageIconSize.LARGE)

        addFactor(baseFactor)
        addFactor(overgrownNanoforgeIntelFactorUndiscovered(this))
        addFactor(overgrownNanoforgeIntelFactorTooManyStructures(this))

        paramsToUse = generateSourceParams()

        Global.getSector().listenerManager.addListener(this)
        spreading = true
    }

    fun stopSpreading() {
        setMaxProgress(OVERGROWN_NANOFORGE_NOT_SPREADING_PROGRESS)
        setProgress(0)

        stages.clear() // TODO: bad idea?

        val iterator = factors.iterator()
        while (iterator.hasNext()) {
            val castedFactor = iterator.next() as? baseOvergrownNanoforgeEventFactor ?: continue
            if (castedFactor.shouldBeRemovedWhenSpreadingStops()) iterator.remove()
        }

        Global.getSector().listenerManager.removeListener(this)
        paramsToUse = null
        spreading = false
    }

    private fun getStartingProgress(): Int = 10

    fun init() {

    }

    override fun getSmallDescriptionTitle(): String {
        return super.getSmallDescriptionTitle()
    }

    override fun hasLargeDescription(): Boolean {
        return super.hasLargeDescription()
    }

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        super.createLargeDescription(panel, width, height)
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (isStageActiveAndLast(stageId)) {
            info?.addPara("blah blah blah If progress is reverted to 0, the growth will be " +
                    "removed and the process will begin again.", 0f)
        }
    }

    override fun getName(): String {
        return "Overgrown Nanoforge on ${ourNanoforgeHandler.market.name}"
    }

    override fun getIcon(): String? {
        return super.getIcon()
    }

    override fun getStageIconImpl(stageId: Any?): String {
        return Global.getSector().getFaction(Factions.PIRATES).crest;
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId == null) return null
        val data = getDataFor(stageId) ?: return null

        when (data.id) {
            overgrownStages.START -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                        val opad: Float = 10f
                        if (tooltip == null) return
                        tooltip.addTitle("Cull growth")
                        tooltip.addPara("When progress is reduced to this level, the growth will be removed and restart" +
                                    " the growing process.", opad)
                        data.addProgressReq(tooltip, opad)
                    }
                }
            }
            overgrownStages.JUNK_SPAWNED -> {
                return object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                        val opad: Float = 10f
                        if (tooltip == null) return
                        tooltip.addTitle("Growth settlement")
                        tooltip.addPara("The growing structure will become permanent, and begin applying all it's effects.", opad)
                        data.addProgressReq(tooltip, opad)
                    }
                }
            }
        }
        return null
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        val tags = super.getIntelTags(map)
        tags += INTEL_OVERGROWN_NANOFORGES
        return tags
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode?) {
        val c = getTitleColor(mode)
        // i would love to do this modularly, alex, but youve fucking. forced my hand with a LOCAL VARIABLE
        info.addPara(name, c, 0f)
        addBulletPoints(info, mode)
    }

    override fun getStages(): MutableList<EventStageData> {
        return super.getStages()
    }

    override fun isImportant(): Boolean {
        return true
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
    }

   /* fun getSpreader(): overgrownNanoforgeJunkSpreader {
        return ourNanoforgeHandler.junkSpreader
    } */

    private fun toggleVisibility(hidden: Boolean?) {
        if (hidden == false) {
            Global.getSector().intelManager.addIntel(this, false)
        } else {
            Global.getSector().intelManager.removeIntel(this)
        }
    }

    // ALEX WHY THE FUCK DO YOU DETERMINE SOMETHING NOT BEING HIDDEN AS THE BOOLEAN HIDDEN FIELD BEING NOT NULL
    // ITS A BOOLEAN
    // A BOOLEAN
    override fun setHidden(hidden: Boolean) {
        super.setHidden(hidden)
  //      toggleVisibility(isHidden)
    }

    fun getMarket(): MarketAPI {
        return ourNanoforgeHandler.market
    }

}
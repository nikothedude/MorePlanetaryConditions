package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.TableRowClickData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.viewMode
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.*
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthReasons
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengths
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import data.utilities.niko_MPC_ids.INTEL_OVERGROWN_NANOFORGES
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT
import lunalib.lunaExtensions.addLunaProgressBar
import lunalib.lunaUI.elements.LunaProgressBar
import org.lazywizard.lazylib.MathUtils
import java.awt.Color
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt

abstract class baseOvergrownNanoforgeIntel(
    val brain: overgrownNanoforgeSpreadingBrain,
    hidden: Boolean = true
): baseNikoEventIntelPlugin() {

    protected var creditDeltaRemainder: Float = 0f
    var growthManipulation: Float = 0f
        set(value) {
            val budget = getGrowthBudget()
            val clampedValue = MathUtils.clamp(value, -budget, budget)
            if (field != clampedValue) {
                field = clampedValue

                growthManipulationMeter?.get()?.changeBoundaries(-budget, budget)
                growthManipulationMeter?.get()?.changeValue(field)
                overallManipulationMeter?.get()?.changeValue(brain.getOverallGrowthManipulation())

                //testVar?.get()?.updateUIForItem(this)

                //manipulationInput?.get()?.text = field.toString()
                individualCostGraphic?.get()?.let { updateCreditCostContents(it, "Current monthly cost: ", calculateCreditCost()) }
                overallCostGraphic?.get()?.let { updateCreditCostContents(it, "Overall monthly cost: ", calculateOverallCreditCost()) }
            }
        }

    var uiUpdater: overgrownNanoforgeIntelInputScanner? = null

    /*@Transient
    var testVar: WeakReference<IntelUIAPI>? = null*/

    @Transient
    var individualCostGraphic: WeakReference<LabelAPI>? = null
    @Transient
    var overallCostGraphic: WeakReference<LabelAPI>? = null

    @Transient
    var overallManipulationMeter: WeakReference<LunaProgressBar>? = null
        set(value) {
            field = value
            field?.get()?.changeValue(brain.getOverallGrowthManipulation())
            if (field != null) uiUpdater?.start()
        }
    @Transient
    var growthManipulationMeter: WeakReference<LunaProgressBar>? = null
        set(value) {
            field = value
            field?.get()?.changeValue(growthManipulation)
            if (field != null) uiUpdater?.start()
        }

    @Transient
    var manipulationInput: WeakReference<TextFieldAPI>? = null
        set(value) {
            field = value
            field?.get()?.text = growthManipulation.toString()
            if (field != null) uiUpdater?.start()
        }
    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        if (panel == null) return
        when (getViewingMode()) {
            viewMode.SPECIFICS -> {
                val outer = panel.createUIElement(width, height, true) ?: return

                createSpecificsPanel(outer, panel)
                panel.addUIElement(outer).inTL(0f, 0f)
            }
            viewMode.DEFAULT -> super.createLargeDescription(panel, width, height)
        }
    }

    private fun createSpecificsPanel(outer: TooltipMakerAPI, panel: CustomPanelAPI) {
        val specifics = cullingStrengthReasons.values()

        val opad = 10f

        outer.addSectionHeading("Specifics", Alignment.MID, opad)

        val padOne = 3f
        for (entry in specifics) {
            outer.addPara("${entry.getName()}: ${entry.getDesc()}", entry.getBaseColor(), padOne)

            val specificInfo = entry.getSpecificInfo()
            if (specificInfo != null) {
                outer.addPara(specificInfo, padOne)
            }

            if (entry != specifics.last()) outer.addSpacer(20f)
        }
        outer.addButton("Return", viewMode.DEFAULT, 100f, 20f, 10f)
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return
        if (isStageActiveAndLast(stageId)) {
            addMiddleDescriptionText(info, width, stageId)
        }
    }

    open fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        info.addPara("blah blah blah If progress is reverted to 0, the growth will be " +
                "removed and the process will begin again.", 0f)

        val colonyMarker = addColonyMarkerAndMap(info)
        val strengthTable = addColonyCullingStrengthInfo(info, colonyMarker)

        info.addSpacer(10f)

        addSuppressionInfo(info, width, stageId, colonyMarker, strengthTable)
    }

    init {
        isHidden = hidden
    }

    open fun init() {
        addStages()
        addFactors()

        initializeProgress()

        Global.getSector().intelManager.addIntel(this, true)
        uiUpdater = overgrownNanoforgeIntelInputScanner(this)
    }

    abstract fun initializeProgress()

    open fun addStages() {
        addStartStage()
        addEndStage()
    }

    open fun addStartStage() {
        addStage(overgrownNanoforgeIntelDummyStartingStage(this), 0, false)
    }
    open fun addEndStage() { return }

    open fun addFactors() {
        addCountermeasuresFactor()
    }

    open fun addCountermeasuresFactor() {
        addFactorWrapped(overgrownNanoforgeIntelFactorCountermeasures(this))
    }

    private fun hasFactorOfClass(clazz: Class<baseOvergrownNanoforgeEventFactor>): Boolean {
        for (factor in factors) {
            if (factor.javaClass.isAssignableFrom(clazz)) return true
        }
        return false
    }

    fun addFactorWrapped(factor: baseOvergrownNanoforgeEventFactor) {
        if (hasFactorOfClass(factor.javaClass)) {
            return factor.delete()
        }
        addFactor(factor)
    }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)
        if (stage == null) return

        val id = stage.id
        if (id is overgrownNanoforgeIntelStage) id.stageReached()
    }

    override fun getStageTooltipImpl(stageId: Any?): TooltipMakerAPI.TooltipCreator? {
        if (stageId == null) return null
        val data = getDataFor(stageId) ?: return null

        val id = data.id
        if (id is overgrownNanoforgeIntelStage) return id.getTooltip(data)

        return null
    }

    override fun getStageIconImpl(stageId: Any?): String {
        val defaultReturn = icon
        if (stageId == null) return defaultReturn
        val data = getDataFor(stageId) ?: return defaultReturn

        val id = data.id
        if (id is overgrownNanoforgeIntelStage) return id.getIconId() ?: defaultReturn

        return defaultReturn
    }

    open fun addColonyMarkerAndMap(info: TooltipMakerAPI): UIPanelAPI {
        val systemW = 230f
        val table = info.beginTable(factionForUIColors, 20f, "Affected Colony", systemW)
        info.makeTableItemsClickable()
        info.addTableHeaderTooltip(0, "The planet undergoing a growth event.")

        val colonyName = getMarket().name
        val system: StarSystemAPI? = getMarket().starSystem
        val systemName = system?.nameWithNoType ?: "Unknown"

        val entry = "$systemName - $colonyName"

        info.addRowWithGlow(Alignment.LMID, Misc.getBasePlayerColor(), entry)

        if (system != null) {
            info.addTooltipToAddedRow(object : BaseFactorTooltip() {
                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                    val w = tooltip.widthSoFar
                    val h = (w / 1.6f).roundToInt().toFloat()
                    tooltip.addSectorMap(w, h, system, 0f)
                    tooltip.addPara("Click to open map", Misc.getGrayColor(), 5f)
                }
            }, TooltipLocation.LEFT, false)
        }
        info.setIdForAddedRow(getMarket())

        val opad = 5f
        info.addTable("None", -1, opad)
        info.addSpacer(3f)

        return table
    }

    open fun addColonyCullingStrengthInfo(info: TooltipMakerAPI, colonyMarket: UIPanelAPI): UIPanelAPI {
        val systemW = 230f
        val reasonsForCullingStrength = cullingStrengthReasons.getReasons(getMarket())
        val score = cullingStrengthReasons.getScoreFromReasons(reasonsForCullingStrength)
        val cullingStrength = cullingStrengths.getStrengthFromScore(score)

        val table = info.beginTable(factionForUIColors, 20f, "Culling Strength", systemW)
        info.makeTableItemsClickable()
        info.addTableHeaderTooltip(0, "The culling \"strength\" of the colony.")

        info.addRowWithGlow(Alignment.MID, cullingStrength.color, "${cullingStrength.ourName} - $score")

        info.addTooltipToAddedRow(object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {

                tooltip.addSectionHeading("Click to see specifics", Alignment.MID, 5f)

                tooltip.addPara("The \"Culling Strength\" of a market is a measurement of how effective the local government is " +
                        "at manipulating the rate of growth exhibited by an overgrown nanoforge. High strength allows for " +
                        "growths to be culled quickly and efficiently, while low strength may be insufficient to cull any growths at all.", 5f)

                tooltip.addPara("The threshold for \"medium\" strength is %s points.", 5f, Misc.getHighlightColor(), cullingStrengths.MEDIUM.scoreThreshold.toString())
                tooltip.addPara("The culling strength score of %s is %s.", 5f, Misc.getHighlightColor(), getMarket().name, score.toString())

                val padOne = 10f
                for (entry in reasonsForCullingStrength) {
                    val reason = entry.key
                    val entryScore = entry.value
                    val highlightColor = reason.getHighlightColor(entryScore)
                    tooltip.addPara("${reason.getName()}: $entryScore", highlightColor, padOne)
                }
            }
        }, TooltipLocation.LEFT, false)

        info.setIdForAddedRow(viewMode.SPECIFICS)

        val opad = 5f
        info.addTable("None", -1, opad)
        info.prev.position.rightOfMid(colonyMarket, 5f)

        return table
    }

    private fun addSuppressionInfo(
        info: TooltipMakerAPI,
        width: Float,
        stageId: Any?,
        colonyMarker: UIPanelAPI,
        strengthTable: UIPanelAPI
    ) {

        val intensityProgressBarLocalVal = createGrowthManipulationMeter(info, width)
        intensityProgressBarLocalVal.position.belowLeft(colonyMarker, 20f)
        growthManipulationMeter = WeakReference(intensityProgressBarLocalVal)

        val overallSuppressionBarLocalVal = info.addLunaProgressBar(
            calculateOverallCreditCost(),
            brain.minGrowthManipulation,
            brain.maxGrowthManipulation,
            width,
            20f,
            Misc.getBasePlayerColor()
        )
        overallManipulationMeter = WeakReference(overallSuppressionBarLocalVal)
        overallSuppressionBarLocalVal.changePrefix("Overall growth manipulation percent: ")
        overallSuppressionBarLocalVal.position.belowLeft(intensityProgressBarLocalVal.elementPanel, 5f)

        info.addSpacer(10f)

        val localIntensityInputVal = info.addTextField(90f, 5f)
        manipulationInput = WeakReference(localIntensityInputVal)
        localIntensityInputVal.position.rightOfMid(intensityProgressBarLocalVal.elementPanel, 5f)

        val canManipulateGrowth = playerCanManipulateGrowth()
        if (!canManipulateGrowth) {
            localIntensityInputVal.maxChars = 0
            localIntensityInputVal.textLabelAPI.opacity = 0.5f
            info.addTooltipToPrevious(createCantInteractWithInputTooltip(), TooltipLocation.LEFT)
        }
        generateCostGraphic(info, overallSuppressionBarLocalVal)
    }

    private fun createGrowthManipulationMeter(info: TooltipMakerAPI, width: Float = 0f): LunaProgressBar {
        val previousMeter = growthManipulationMeter?.get()
        //previousMeter?.let { info.removeComponent(it.elementPanel) }

        val defaultScore = previousMeter?.getValue() ?: 0f
        val newWidth = previousMeter?.width ?: width
        val height = previousMeter?.height ?: 20f

        val budget = getGrowthBudget()
        val max = budget
        val min = -budget

        val bar = info.addLunaProgressBar(
            defaultScore,
            min,
            max,
            newWidth,
            height,
            Misc.getBasePlayerColor()
        )
        info.addTooltipToPrevious(object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)
                if (tooltip == null) return

                val standardMult = (OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT * 0.1f)
                val extraMult = (OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT * 0.1f)

                tooltip.addPara("This statistic represents the percent of culling strength being used to either suppress or cultivate nanoforge growth.", 2f)
                tooltip.addPara("Each percent of strength used will increase the monthly cost of the nanoforge by (%s) credits.", 2f, Misc.getHighlightColor(), "$standardMult * culling strength")
                tooltip.addPara("Surpassing %s percent usage in either direction will add an extra (%s) credits to the cost.", 2f, Misc.getHighlightColor(), OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD.toString(), "$extraMult * culling strength")
            }
        }, TooltipLocation.BELOW)
        bar.changePrefix("Growth manipulation percent: ")

        return bar
    }

    open fun generateCostGraphic(info: TooltipMakerAPI, orientingBar: LunaProgressBar) {
        val localCost = calculateCreditCost()
        val DGSlocalCost = Misc.getDGSCredits(localCost)
        val individualCostGraphicLocalVal = info.addPara("Current monthly cost: %s credits", 5f, getHighlightForCreditCost(localCost), DGSlocalCost)

        individualCostGraphicLocalVal.position.belowMid(orientingBar.elementPanel, 5f)
        individualCostGraphic = WeakReference(individualCostGraphicLocalVal)

        updateCreditCostContents(individualCostGraphicLocalVal, "Current monthly cost: ", localCost)

        val overallCost = calculateOverallCreditCost()
        val DGSoverallCost = Misc.getDGSCredits(overallCost)
        val overallCostGraphicLocalVal = info.addPara("Overall monthly cost: %s credits", 5f, getHighlightForCreditCost(overallCost), DGSoverallCost)
        overallCostGraphic = WeakReference(overallCostGraphicLocalVal)

        updateCreditCostContents(overallCostGraphicLocalVal, "Overall monthly cost: ", overallCost)
    }

    private fun updateCreditCostContents(label: LabelAPI, base: String, credits: Float) {
        val DGScredits = Misc.getDGSCredits(credits)
        val beginning = base.length
        val concat = (base + DGScredits)
        val end = concat.length
        val finalMessage = (concat)

        label.text = finalMessage
        label.setHighlight(beginning, end)
    }

    /** If false, should grey out manipulation inputs. */
    open fun playerCanManipulateGrowth(): Boolean {
        return getMarket().isPlayerOwned
    }

    fun createCantInteractWithInputTooltip(): TooltipMakerAPI.TooltipCreator {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)
                if (tooltip == null) return

                for (reason in getCantInteractWithInputReasons()) {
                    tooltip.addPara(reason, 5f)
                }
            }
        }
    }

    open fun getCantInteractWithInputReasons(): MutableSet<String> {
        val reasons = HashSet<String>()
        if (!getMarket().isPlayerOwned) reasons += "You are not in control of ${getMarket().name}'s government, disallowing you from interacting from the statistics."

        return reasons
    }

    open fun getHighlightForCreditCost(cost: Float): Color {
        return if (cost > 0f) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
    }

    fun calculateOverallCreditCost(): Float {
        return brain.calculateOverallCreditCost()
    }

    fun calculateCreditCost(): Float {
        val absIntensity = abs(growthManipulation)
        val standardCost = (absIntensity * OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT)
        val extraCost = (((absIntensity - OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD).coerceAtLeast(0f)) * OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT)

        return (standardCost + extraCost)*getOverallCullingStrength(getMarket())
    }

    fun getOverallCullingStrength(market: MarketAPI): Float {
        return brain.getOverallCullingStrength()
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        super.buttonPressConfirmed(buttonId, ui)

        if (buttonId == null || ui == null) return
        if (buttonId is viewMode) {
            brain.viewingMode = buttonId
            ui.recreateIntelUI()
        }
    }

    override fun tableRowClicked(ui: IntelUIAPI, data: TableRowClickData) {
        val id = data.rowId
        if (id is MarketAPI) {
            val location = id.starSystem ?: return
            if (location.doNotShowIntelFromThisLocationOnMap == true) return
            val focus: SectorEntityToken? = id.primaryEntity ?: location.hyperspaceAnchor

            ui.showOnMap(focus)
        } else if (id is viewMode) {
            brain.viewingMode = id

            ui.recreateIntelUI()
        }
    }

    fun getViewingMode(): viewMode = brain.viewingMode
    fun getMarket(): MarketAPI = brain.getMarket()

    fun getGrowthBudget(): Float {
        return (brain.getManipulationBudget(this))
    }

    override fun reportPlayerClickedOn() {
        super.reportPlayerClickedOn()

        brain.viewingMode = viewMode.DEFAULT
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

    fun getNaturalGrowthInt(): Int {
        var rate = 0
        getNaturalGrowthFactors().forEach { rate += it.getProgress(this) }
        return rate
    }

    fun getNaturalGrowthFactors(): MutableSet<baseOvergrownNanoforgeEventFactor> {
        val naturalFactors: MutableSet<baseOvergrownNanoforgeEventFactor> = HashSet()
        for (factor in factors) {
            if (factor !is baseOvergrownNanoforgeEventFactor) continue
            if (factor.isNaturalGrowthFactor()) naturalFactors += factor
        }
        return naturalFactors
    }

    override fun isEventProgressANegativeThingForThePlayer(): Boolean {
        return true
    }

    override fun getBarColor(): Color {
        var color = Misc.getNegativeHighlightColor()
        color = Misc.interpolateColor(color, Color.black, 0.25f)
        return color
    }

    fun shouldDeductCredits(): Boolean {
        return playerCanManipulateGrowth()
    }

    override fun getIcon(): String {
        return "graphics/icons/cargo/nanoforge_decayed.png"
    }
}

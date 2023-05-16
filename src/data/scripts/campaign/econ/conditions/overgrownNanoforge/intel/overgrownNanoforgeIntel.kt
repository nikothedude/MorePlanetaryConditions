package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.TableRowClickData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.*
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipLocation
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.scripts.campaign.intel.baseNikoEventIntelPlugin
import data.utilities.niko_MPC_ids.INTEL_OVERGROWN_NANOFORGES
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_NOT_SPREADING_PROGRESS
import lunalib.lunaExtensions.addLunaProgressBar
import lunalib.lunaUI.elements.LunaProgressBar
import org.lazywizard.lazylib.MathUtils
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

class overgrownNanoforgeIntel(
    val ourNanoforgeHandler: overgrownNanoforgeIndustryHandler,
    hidden: Boolean = true
): baseNikoEventIntelPlugin() {

    enum class viewMode {
        DEFAULT,
        SPECIFICS,
    }

    val strengthHolder: overgrownNanoforgeIntelStrengthHolder = overgrownNanoforgeIntelStrengthHolder(this)

    @Transient
    var externalSupportInput: TextFieldAPI? = null
        set(value) {
            field = value
            field?.text = externalSupportRating.toString()
            if (field != null) inputScanner.start()
        }
    var externalSupportRating: Float = 0f
        set(value) {
            val sanitizedValue = MathUtils.clamp(value, getMinimumSuppressionIntensity(), getMaximumSuppressionIntensity())
            field = sanitizedValue

            externalSupportProgressBar?.changeValue(field)
        }

    @Transient
    var externalSupportProgressBar: LunaProgressBar? = null

    @Transient
    var intensityProgressBar: LunaProgressBar? = null
    @Transient
    var intensityInput: TextFieldAPI? = null
        set(value) {
            field = value
            field?.text = suppressionIntensity.toString()
            if (field != null) inputScanner.start()
        }
    val inputScanner: overgrownNanoforgeIntelInputScanner = overgrownNanoforgeIntelInputScanner(this)
    var suppressionCreditCost: Float = 0f
    var suppressionIntensity: Float = getDefaultSuppressionIntensity()
        set(value) {
            val sanitizedValue = MathUtils.clamp(value, getMinimumSuppressionIntensity(), getMaximumSuppressionIntensity())
            field = sanitizedValue

            intensityProgressBar?.changeValue(field)
            suppressionCreditCost = calculateSuppressionCost()
        }


    private fun getDefaultSuppressionIntensity(): Float {
        return 0f
    }

    var spreading: Boolean = false
    var paramsToUse: overgrownNanoforgeRandomizedSourceParams? = null
    @Transient
    var viewingMode = viewMode.DEFAULT
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
        CULL,
        JUNK_SPAWNED,
    }

    enum class spreadingStates {
        SPREADING {
            override fun apply(intel: overgrownNanoforgeIntel) {
                Global.getSector().campaignUI.addMessage("Spread started at ${intel.getMarket().name}")

                intel.setMaxProgress(OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS)
                intel.setProgress(intel.getStartingProgress())

                intel.getDataFor(overgrownStages.CULL).hideIconWhenPastStageUnlessLastActive = false
                intel.getDataFor(overgrownStages.CULL).keepIconBrightWhenLaterStageReached = false
                intel.getDataFor(overgrownStages.CULL).isRepeatable = true

                intel.addFactor(baseFactor)
                intel.addFactor(overgrownNanoforgeIntelFactorUndiscovered(intel))
                intel.addFactor(overgrownNanoforgeIntelFactorTooManyStructures(intel))

                intel.paramsToUse = intel.generateSourceParams()

                Global.getSector().listenerManager.addListener(intel)
            }
            override fun unapply(intel: overgrownNanoforgeIntel) {
                super.unapply(intel)

                Global.getSector().campaignUI.addMessage("Spread stopped at ${intel.getMarket().name}")

                //intel.stages.clear() // TODO: bad idea?

                val iterator = intel.factors.iterator()
                while (iterator.hasNext()) {
                    val castedFactor = iterator.next() as? baseOvergrownNanoforgeEventFactor ?: continue
                    if (castedFactor.shouldBeRemovedWhenSpreadingStops()) iterator.remove()
                }
            }
        },
        IN_COOLDOWN{
            override fun apply(intel: overgrownNanoforgeIntel) {
                super.unapply(intel)
                intel.setMaxProgress(OVERGROWN_NANOFORGE_NOT_SPREADING_PROGRESS)

                Global.getSector().campaignUI.addMessage("Spread stopped at ${intel.getMarket().name}")

                //intel.stages.clear() // TODO: bad idea?

                Global.getSector().listenerManager.removeListener(intel)
                intel.paramsToUse = null
                intel.baseFactor = null
            }
        };

        abstract fun apply(intel: overgrownNanoforgeIntel)
        open fun unapply(intel: overgrownNanoforgeIntel) {
            intel.setProgress(0)
        }
    }
    var spreadingState: spreadingStates = spreadingStates.IN_COOLDOWN
        set(value: spreadingStates) {
            if (value != field) {
                field.unapply(this)
                value.apply(this)
            }
            field = value
        }

    init {
        isHidden = hidden
        Global.getSector().intelManager.addIntel(this, false)

        stopSpreading() //TODO: this WILL fucking explode.

        addStage(overgrownStages.START, 0, false)
        addStage(overgrownStages.CULL, 0, true, StageIconSize.LARGE)
        addStage(overgrownStages.JUNK_SPAWNED, OVERGROWN_NANOFORGE_MAX_INTEL_PROGRESS, true, StageIconSize.LARGE)

        addFactor(overgrownNanoforgeIntelFactorCountermeasures(this))
    }

    override fun advanceImpl(amount: Float) {
        if (!spreading) {
            val days = Misc.getDays(getAdjustedAmount(amount))
            spreadIntervalTimer.advance(days)
            if (spreadIntervalTimer.intervalElapsed()) {
                startSpreading()
            }
        }
        super.advanceImpl(amount)
    }

    fun getAdjustedAmount(amount: Float): Float {
        if (getMarket().exceedsMaxStructures()) return 0f
        var amount: Float = amount
        if (isHidden) {
            if (!niko_MPC_settings.OVERGROWN_NANOFORGE_PROGRESS_WHILE_UNDISCOVERED) return 0f
            amount *= 0.1f
        }
        return amount
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)

        if (haveWeBeenCulled()) {
            culled()
        }
    }

    fun culled() {
        stopSpreading()
        if (shouldReportCulled()) {
            reportCulled()
        }
    }

    fun shouldReportCulled(): Boolean {
        return (!isHidden)
    }

    private fun reportCulled() {
        sendUpdateIfPlayerHasIntel(getDataFor(overgrownStages.CULL), textPanelForStageChange)
    }

    fun haveWeBeenCulled(): Boolean {
        return (spreading && progress <= 0)
    }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)
        if (stage == null) return

        when (stage.id) {
            overgrownStages.JUNK_SPAWNED -> {
                junkSpawned()
            }
        }
    }

    private fun junkSpawned() {
        if (getMarket().exceedsMaxStructures()) {
            niko_MPC_debugUtils.displayError("intel for ${getMarket().name} spawned junk despite market exceeding max structures")
            culled()
        }
        paramsToUse.createJunk()
        stopSpreading()
        if (shouldReportSpreaded()) reportSpreaded()
    }

    fun shouldReportSpreaded(): Boolean {
        return (playerControlsMarket())
    }

    fun reportSpreaded() {
        Global.getSector().campaignUI.addMessage("junk spreaded on ${getMarket().name}")  
    }

    fun startSpreading() {
        spreadingState = spreadingStates.SPREADING
    }

    fun stopSpreading() {
        spreadingState = spreadingStates.IN_COOLDOWN
    }

    private fun getStartingProgress(): Int = 10

    // vvvvvvv PANELS, DESCRIPTIONS, TOOLTIPS

    override fun createLargeDescription(panel: CustomPanelAPI?, width: Float, height: Float) {
        if (panel == null) return
        when (viewingMode) {
            viewMode.SPECIFICS -> {
                val outer = panel.createUIElement(width, height, true) ?: return

                createSpecificsPanel(outer, panel)
                panel.addUIElement(outer).inTL(0f, 0f)
            }
            viewMode.DEFAULT -> super.createLargeDescription(panel, width, height)
        }
    }

    override fun reportPlayerClickedOn() {
        super.reportPlayerClickedOn()

        viewingMode = viewMode.DEFAULT
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI?, width: Float, stageId: Any?) {
        if (info == null) return
        if (isStageActiveAndLast(stageId)) {
            addMiddleDescriptionText(info, width, stageId)
        }
    }

    private fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {

        info.addPara("blah blah blah If progress is reverted to 0, the growth will be " +
                "removed and the process will begin again.", 0f)

        val colonyMarker = addColonyMarkerAndMap(info)

        val strengthTable = addColonyCullingStrengthInfo(info, colonyMarker)

        info.addSpacer(10f)

        addSuppressionInfo(info, width, stageId, colonyMarker, strengthTable)

        addParamsInfo(info, width, stageId)
    }

    private fun addColonyCullingStrengthInfo(info: TooltipMakerAPI, colonyMarket: UIPanelAPI): UIPanelAPI {
        val systemW = 230f
        val reasonsForCullingStrength = getCullingStrengthReasonsForMarket(getMarket())
        var score = 0f
        reasonsForCullingStrength.values.forEach { score += it }

        val cullingStrength = getCullingStrengthFromScore(score)

        val table = info.beginTable(factionForUIColors, 20f, "Culling Strength", systemW)
        val pos = table.position
        info.makeTableItemsClickable()
        info.addTableHeaderTooltip(0, "The culling \"strength\" of the colony.")

        info.addRowWithGlow(Alignment.MID, cullingStrength.color, "${cullingStrength.ourName} - $score")

        info.addTooltipToAddedRow(object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {

                tooltip.addSectionHeading("Click to see specifics", Alignment.MID, 5f)

                tooltip.addPara("The \"Culling Strength\" of a market is a measurement of how effective the local government is " +
                        "at manipulating the rate of growth exhibited by an overgrown nanoforge. High strength allows for " +
                        "growths to be culled quickly and efficiently, while low strength may be insufficient to cull any growths at all.", 5f)

                tooltip.addPara("The threshold for \"medium\" strength is %s points.", 5f, Misc.getHighlightColor(), cullingStrengthValues.cullingStrength.MEDIUM.scoreThreshold.toString())
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

    private fun createSpecificsPanel(outer: TooltipMakerAPI, panel: CustomPanelAPI) {
        val specifics = cullingStrengthReasons.values()

        val opad = 10f

        outer.addSectionHeading("Specifcs", Alignment.MID, opad)

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

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        super.buttonPressConfirmed(buttonId, ui)

        if (buttonId == null || ui == null) return
        if (buttonId is viewMode) {
            toggleViewmode(buttonId, ui)
        }
    }

    private fun addSuppressionInfo(
        info: TooltipMakerAPI,
        width: Float,
        stageId: Any?,
        colonyMarker: UIPanelAPI,
        strengthTable: UIPanelAPI
    ) {
        val height = 20f
        intensityProgressBar = info.addLunaProgressBar(
            suppressionIntensity,
            getMinimumSuppressionIntensity(),
            getMaximumSuppressionIntensity(),
            width,
            height,
            Misc.getBasePlayerColor())
        intensityProgressBar!!.changePrefix("Suppression intensity: ")
        intensityProgressBar!!.position.belowLeft(colonyMarker, 20f)

        info.addTooltipToPrevious(object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)
                if (tooltip == null) return

                val standardMult = (OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT * 0.01f)
                val extraMult = (OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT * 0.01f)

                tooltip.addPara("This statistic represents the percent of culling strength being used to either suppress or cultivate nanoforge growth.", 2f)
                tooltip.addPara("Each percent of strength used will increase the monthly cost of the nanoforge by (%s) credits.", 2f, Misc.getHighlightColor(), "$standardMult * culling strength")
                tooltip.addPara("Surpassing %s percent usage in either direction will add an extra (%s) credits to the cost.", 2f, Misc.getHighlightColor(), "$extraMult * culling strength")
            }
        }, TooltipLocation.BELOW)

        info.addSpacer(10f)

        /*externalSupportProgressBar = info.addLunaProgressBar(
            externalSupportRating,
            getMinimumExternalSupport(),
            getMaximumExternalSupport(),
            width,
            height,
            Misc.getBasePlayerColor()
        )
        externalSupportProgressBar!!.changePrefix("External Support: ")

        info.addTooltipToPrevious(object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean, tooltipParam: Any?) {
                super.createTooltip(tooltip, expanded, tooltipParam)
                if (tooltip == null) return

                tooltip.addPara("This statistic represents how much external cash is being injec", 5f)
            }
        }, TooltipLocation.BELOW) */

        if (playerControlsMarket()) {
            intensityInput = info.addTextField(60f, 5f)
            intensityInput!!.position.rightOfMid(intensityProgressBar!!.elementPanel, 5f)

            //externalSupportInput = info.addTextField(60f, 5f)
            //externalSupportInput!!.position.rightOfMid(externalSupportProgressBar!!.elementPanel, 5f)
        }
        generateCostGraphic(info)
    }

    private fun generateCostGraphic(info: TooltipMakerAPI) {
        info.addPara("Current cost: %s credits", 5f, getHighlightForSuppressionCost(), suppressionCreditCost)
    }

    fun calculateSuppressionCost(): Float {
        val overallStrength = getOverallCullingStrength(getMarket())
        val absIntensity = abs(suppressionIntensity)
        val standardCost = (absIntensity * OVERGROWN_NANOFORGE_SUPPRESSION_RATING_TO_CREDITS_MULT)
        val extraCost = (((absIntensity - OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_THRESHOLD).coerceAtLeast(0f)) * OVERGROWN_NANOFORGE_SUPPRESSION_EXTRA_COST_MULT)

        return (standardCost + extraCost)*getOverallCullingStrength(getMarket())
    }

    private fun getHighlightForSuppressionCost(): Color {
        return if (suppressionCreditCost > 0f) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
    }

    fun getMinimumExternalSupport(): Float = 0f
    fun getMaximumExternalSupport(): Float = 150f

    fun playerControlsMarket(): Boolean = getMarket().isPlayerOwned

    private fun getMinimumSuppressionIntensity(): Float = -150f
    private fun getMaximumSuppressionIntensity(): Float = 150f

    private fun addColonyMarkerAndMap(info: TooltipMakerAPI): UIPanelAPI {
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

    override fun tableRowClicked(ui: IntelUIAPI, data: TableRowClickData) {
        val id = data.rowId
        if (id is MarketAPI) {
            val location = id.starSystem ?: return
            if (location.doNotShowIntelFromThisLocationOnMap == true) return
            val focus: SectorEntityToken? = id.primaryEntity ?: location.hyperspaceAnchor

            ui.showOnMap(focus)
        } else if (id is viewMode) {

            toggleViewmode(id)

            ui.recreateIntelUI()
        }
    }

    private fun toggleViewmode(newViewMode: viewMode, ui: IntelUIAPI? = null) {
        viewingMode = newViewMode

        ui?.recreateIntelUI()
    }

        /** This is where we tell the player information about the to-be-made structure. */
    private fun addParamsInfo(info: TooltipMakerAPI, width: Float, stageId: Any?) {

    }

    // ^^^^^^^^^ PANELS, DESCRIPTIONS, TOOLTIPS

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
            overgrownStages.CULL -> {
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

    fun getNaturalGrowthInt(): Int {
        var rate = 0
        getNaturalGrowthFactors().forEach { rate += it.getProgress(this) }
        return rate
    }

    fun getNaturalGrowthFactors(): MutableSet<baseOvergrownNanoforgeEventFactor> {
        return mutableSetOf(baseFactor!!)
    }

    override fun isEventProgressANegativeThingForThePlayer(): Boolean {
        return true
    }

    fun getSuppressionRating(): Int {
        return -suppressionIntensity.toInt()
    }

    companion object cullingStrengthValues {

        const val THRESHOLD_MULT_MINIMAL = 0.25f

        const val THRESHOLD_MULT_VERY_LOW = 0.5f
        const val THRESHOLD_MULT_LOW = 0.75f
        const val THRESHOLD_MULT_MEDIUM = 1f
        const val THRESHOLD_MULT_HIGH = 1.25f
        const val THRESHOLD_MULT_VERY_HIGH = 1.50f
        const val THRESHOLD_MULT_EXTREME = 1.75f
        const val MEDIUM_SCORE_THRESHOLD = 200f

        const val STABILITY_ANCHOR = 8

        const val STABILITY_DIFFERENCE_INCREMENT = 15f

        const val PATROL_TAG_SCORE = 50f
        const val MILITARY_TAG_SCORE = 80f
        const val COMMAND_TAG_SCORE = 120f

        class specificCullingStrengthReason(
            val name: String,
            val score: Float,
            val reason: String? = null,
        )

        enum class cullingStrengthReasons {
            //TODO: return to this, maybe have a crew/marine/heavy machinery req idfk
            /* 
            SHORTAGES {
                override fun getName(): String = "Shortages"

                override fun getDesc(): String = "Insufficient supply, such as crew or heavy machinery, can thwart culling efforts."

                override fun shouldShow(market: MarketAPI): Boolean {
                    return false
                }
                override fun getScoreForMarket(market: MarketAPI): Float {
                    return 0f
                }
            }, */
            STABILITY {
                override fun getDesc(): String {
                    return "For each point of stability above or below $STABILITY_ANCHOR, culling strength will increase " +
                            "or decrease by $STABILITY_DIFFERENCE_INCREMENT respectively."
                }
                override fun getName(): String = "Stability"
                override fun shouldShow(market: MarketAPI): Boolean = true
                override fun getScoreForMarket(market: MarketAPI): Float {
                    if (!market.isInhabited()) return 0f
                    return ((market.stability.modifiedValue - STABILITY_ANCHOR) * STABILITY_DIFFERENCE_INCREMENT)
                }

                override fun getBaseColor(): Color = Misc.getHighlightColor()
            },
            GROUND_FORCE_PRESENCE {
                val tagMap = HashMap<String, Float>()
                val coreMap = HashMap<String, Float>()
                val improvementMult = 1.2f
                init {
                    tagMap[Industries.TAG_PATROL] = PATROL_TAG_SCORE
                    tagMap[Industries.TAG_MILITARY] = MILITARY_TAG_SCORE
                    tagMap[Industries.TAG_COMMAND] = COMMAND_TAG_SCORE

                    coreMap[Commodities.ALPHA_CORE] = 1.5f
                    coreMap[Commodities.BETA_CORE] = 1.3f
                    coreMap[Commodities.GAMMA_CORE] = 1.1f
                }
                override fun getDesc(): String {
                    return "For each piece of military infrastructure on the planet, culling strength will increase in " +
                            "relation to it's capability. This can be affected by installed items, shortages, improvements, " +
                            "and more."
                }
                override fun getName(): String = "Ground force presence"
                override fun shouldShow(market: MarketAPI): Boolean = true
                override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()

                override fun getScoreForMarket(market: MarketAPI): Float {
                    var totalScore = 0f
                    for (industry in market.industries) {

                        var industryScore = 0f
                        industry.spec.tags.forEach { it -> tagMap[it]?.let { industryScore += it } }

                        if (industryScore == 0f) continue

                        var totalMult = 1f
                        if (industry.isImproved) totalMult += improvementMult - 1
                        coreMap[industry.aiCoreId]?.let { totalMult += it - 1 }

                        val finalScore = industryScore * totalMult

                        totalScore += finalScore
                    }
                    return totalScore
                }

                override fun getSpecificInfo(): String {
                    return "Patrol HQs, Military Bases, and High Commands increase score by " +
                            "${tagMap[Industries.TAG_PATROL]}, ${tagMap[Industries.TAG_MILITARY]}, and ${tagMap[Industries.TAG_COMMAND]} respectively." +
                            "\n     When improved: Contribution multiplied by $improvementMult." +
                            "\n     When a AI core is installed, Contribution multiplied by ${coreMap[Commodities.GAMMA_CORE]}, ${coreMap[Commodities.BETA_CORE]}, and ${coreMap[Commodities.ALPHA_CORE]} respectively." +
                            "\nModded industries, provided they are tagged correctly, will also contribute to this score."
                }
            },
            ADMINISTRATOR_ABILITY {
                val skillMap: MutableMap<String, Float> = HashMap()
                val scorePerLevel = 5f
                init {
                    skillMap[Skills.PLANETARY_OPERATIONS] = 50f
                    skillMap[Skills.TACTICAL_DRILLS] = 10f
                    skillMap[Skills.HYPERCOGNITION] = 80f
                }

                override fun getDesc(): String {
                    return "Various administrative skills may increase the government's ability to manipulate growth."
                }

                override fun getSpecificInfo(): String {
                    return "The following skills affect culling strength:" +
                            "\n     Tactical drills - ${skillMap[Skills.TACTICAL_DRILLS]}" +
                            "\n     Hypercognition - ${skillMap[Skills.HYPERCOGNITION]}" +
                            "\nAdministrator level increases strength by $scorePerLevel per level."
                }

                override fun getName(): String {
                    return "Administrator Ability"
                }

                override fun shouldShow(market: MarketAPI): Boolean {
                    return true
                }

                override fun getScoreForMarket(market: MarketAPI): Float {
                    if (!market.isInhabited()) return 0f
                    var amount = 0f

                    val admin = market.admin ?: return amount
                    val stats = admin.stats

                    for (entry in skillMap.keys) {
                        if (stats.hasSkill(entry)) amount += skillMap[entry]!!
                    }

                    amount += getScoreForLevel(admin)

                    return amount
                }
                fun getScoreForLevel(admin: PersonAPI): Float {
                    val level = admin.stats?.level ?: return 0f

                    return scorePerLevel * level
                }
                override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()
            },
            EXTERNAL_SUPPORT {
                override fun getName(): String = "External Support"

                override fun getDesc(): String = "By injecting credits, resources, and power into the colony, strength may be artificially raised, albiet inefficiently."

                override fun getScoreForMarket(market: MarketAPI): Float {
                    var rating = market.getOvergrownNanoforgeIndustryHandler()?.intel?.externalSupportRating ?: return 0f
                    rating *= 5f

                    return rating
                }
                override fun getBaseColor(): Color = Misc.getPositiveHighlightColor()
            };

            abstract fun getName(): String
            abstract fun getDesc(): String
            open fun getSpecificInfo(): String? = null
            open fun shouldShow(market: MarketAPI): Boolean = true
            abstract fun getScoreForMarket(market: MarketAPI): Float
            fun getHighlightColor(score: Float): Color {
                if (score > 0f) return Misc.getPositiveHighlightColor()
                if (score < 0f) return Misc.getNegativeHighlightColor()
                return Misc.getDarkHighlightColor()
            }

            open fun getBaseColor(): Color = Misc.getGrayColor()
        }

        enum class cullingStrength(
            val color: Color,
            val ourName: String,
            val scoreThreshold: Float
        ) {
            NONE(Misc.getNegativeHighlightColor(), "Minimal", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_MINIMAL),
            VERY_LOW(Misc.getNegativeHighlightColor(), "Very Low", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_VERY_LOW),
            LOW(Misc.getNegativeHighlightColor(), "Low", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_LOW),
            MEDIUM(Misc.getHighlightColor(), "Medium", MEDIUM_SCORE_THRESHOLD),
            HIGH(Misc.getPositiveHighlightColor(), "High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_HIGH),
            VERY_HIGH(Misc.getPositiveHighlightColor(), "Very High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_VERY_HIGH),
            EXTREMELY_HIGH(Misc.getPositiveHighlightColor(), "Extremely High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_EXTREME);
        }

        fun getCullingStrengthReasonsForMarket(market: MarketAPI): MutableMap<cullingStrengthReasons, Float> {
            val map: MutableMap<cullingStrengthReasons, Float> = EnumMap(cullingStrengthReasons::class.java)
            for (entry in cullingStrengthReasons.values()) {
                if (!entry.shouldShow(market)) continue

                val score = entry.getScoreForMarket(market)
                map[entry] = score
            }
            return map
        }

        fun getOverallCullingStrength(market: MarketAPI): Float {
            var strength = 0f
            getCullingStrengthReasonsForMarket(market).values.forEach { strength += it }
            return strength
        }

        fun getCullingStrengthFromScore(score: Float): cullingStrength {
            var highestThreshold = cullingStrength.NONE
            for (entry in cullingStrength.values()) {
                if (entry.scoreThreshold > score) continue
                if (entry.scoreThreshold < highestThreshold.scoreThreshold) continue

                highestThreshold = entry
            }
            return highestThreshold
        }

        fun getCullingStrengthReasonsWithDescription(market: MarketAPI): MutableSet<cullingStrengthReasons> {
            val set: MutableSet<cullingStrengthReasons> = HashSet()
            for (entry in cullingStrengthReasons.values()) {
                val desc = entry.getSpecificInfo() ?: continue
                set += entry
            }
            return set
        }
    }
}

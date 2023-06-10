package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.spreadingStates
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownSpreadingParams
import data.utilities.niko_MPC_marketUtils.maxStructureAmount
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_SCORE_ESTIMATION_VARIANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_SCORE_ESTIMATION_VARIANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE
import org.lazywizard.lazylib.MathUtils
import kotlin.math.roundToInt

class overgrownNanoforgeGrowthIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    override val ourHandler: overgrownNanoforgeJunkHandler,
    val params: overgrownSpreadingParams,
) : baseOvergrownNanoforgeManipulationIntel(brain, ourHandler) {

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getInitialProgress())
    }

    private fun getInitialProgress(): Int {
        return ((getStartingProgressPercent()/100f)*getMaxProgress()).roundToInt()
    }

    /** Treated as a percent of max progress. */
    private fun getStartingProgressPercent(): Float {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN,
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX
        )
    }
    var knowExactEffects: Boolean = false
    var estimatedScore: String = "Error"
        get() {
            if (estimatedScoreNeedsUpdate()) field = updateEstimatedScore()
            return field
        }

    var lastProgressCheckedForEstimatedScore: Int = -5

    fun estimatedScoreNeedsUpdate(): Boolean {
        return (lastProgressCheckedForEstimatedScore != getProgress())
    }

    fun updateEstimatedScore(): String {
        val score = ourHandler.getBaseBudget() ?: return "Error"
        if (knowExactEffects) return "$score"
        val anchor = getScoreDiscoveryProgressPercent()
        lastProgressCheckedForEstimatedScore = getProgress()
        val percentComplete = progressFraction * 100
        if (percentComplete <= OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE) return "Unknown"
        val percentToAnchor = (percentComplete/anchor) * 100f

        if (percentToAnchor >= 100) return "$score"

        val discoveryMult = 100 / percentToAnchor

        val minVar = MathUtils.getRandomNumberInRange(0.8f, 1.2f)
        val maxVar = MathUtils.getRandomNumberInRange(0.8f, 1.2f)

        val min = (score*(1 / discoveryMult)*minVar).coerceAtLeast(0f)
        val max = score*(1 * discoveryMult)*maxVar

        return "$min - $max"
    }

    private fun getScoreDiscoveryProgressPercent(): Int {
        return params.percentThresholdToTotalScoreKnowledge
    }

    override fun addEndStage() {
        overgrownNanoforgeFinishGrowthStage(this).init()
    }

    override fun addInitialStages() {
        super.addInitialStages()

        growthDiscoveryStages.TARGET.getChildren(this).forEach { it.init() }
        growthDiscoveryStages.EFFECTS.getChildren(this).forEach { it.init() }
    }

    fun growingComplete() {
        params.spread()

        delete()
    }

    override fun delete() {
        super.delete()
        brain.spreadingState = spreadingStates.PREPARING
    }

    override fun addParamsInfo(info: TooltipMakerAPI, width: Float, stageId: Any?): UIComponentAPI {
        val prevTable = super.addParamsInfo(info, width, stageId)

        val targetWidth = 180f
        val scoreWidth = 300f

        info.beginTable(factionForUIColors, 20f,
    "Target", targetWidth,
        )
        info.addTableHeaderTooltip(0, "The structure this growth is targeting. Guaranteed to be nothing if " +
                "the market does not have ${maxStructureAmount} visible structures. If this growth completes with an active target, " +
                "the target structure will be destroyed and replaced by the growth.")

        val targetData = getTargetData()

        val baseAlignment = Alignment.LMID
        val baseColor = Misc.getBasePlayerColor()

        info.addRowWithGlow(
            baseAlignment, baseColor, targetData.name)
        targetData.industry?.let { info.setIdForAddedRow(it) }

        val opad = 5f
        info.addTable("None", -1, opad)
        info.addSpacer(3f)

        info.beginTable(factionForUIColors, 20f,
            "Overall score", scoreWidth)
        info.addTableHeaderTooltip(0, "An estimation of the overall score of the growth. " +
                "A higher score means more intense effects, both negative and positive.")
        info.addRowWithGlow(
            Alignment.MID, baseColor, estimatedScore
        )
        info.addTable("None", -1, opad)

        return info.prev
    }

    override fun getFormattedPositives(): String {
        if (knowExactEffects) return super.getFormattedPositives()

        return "Unknown"
    }

    override fun getFormattedNegatives(): String {
        if (knowExactEffects) return super.getFormattedNegatives()

        return "Unknown"
    }

    override fun tableRowClicked(ui: IntelUIAPI, data: IntelInfoPlugin.TableRowClickData) {
        super.tableRowClicked(ui, data)

        val id = data.rowId
        if (id is Industry) {
            val market = id.market
            // TODO open market screen
        }
    }

    private fun getTargetData(): targetData {
        if (params.nameKnown) return targetData(params.ourIndustryTarget, params.getIndustryName())

        val data = targetData(null, params.getIndustryName())
        return data
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        params.updateIndustryTarget()
    }

    override fun getName(): String {
        return "Growth on ${getMarket().name}"
    }

    override fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addBasicDescription(info, width, stageId)

        info.addPara("This specific ${ourHandler.getCurrentName()} is %s and is %s. As such, its" +
                " abilities and characteristics are %s, and they will %s until it is %s.", 5f,
        Misc.getHighlightColor(), "not established", "still growing", "unknown", "not take effect", "fully grown")
        info.addPara("As the ${ourHandler.getCurrentName()} grows, more details will reveal themselves, such as " +
                "the effects of the structure, the target it seeks to destroy, or the overall score of it.", 5f)
    }

    override fun culled() {
        super.culled()

        delete() // needed since the junk handler has no ref to us
    }

    override fun getSpreadingAdjective(): String = "spreading"
}

class targetData(
    var industry: Industry?,
    val name: String = industry?.currentName ?: "None"
) {

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
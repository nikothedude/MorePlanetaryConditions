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
    hidden: Boolean = true
) : baseOvergrownNanoforgeManipulationIntel(brain, ourHandler, hidden) {

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getInitialProgress())
    }

    private fun getInitialProgress(): Int {
        return ((getStartingProgressPercent()/100f)*getMaxProgress()).roundToInt()
    }

    private fun getStartingProgressPercent(): Float {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MIN,
            OVERGROWN_NANOFORGE_GROWTH_STARTING_PROGRESS_PERCENT_MAX
        )
    }

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
        val anchor = params.percentThresholdToTotalScoreKnowledge
        val percentComplete = progressFraction

        val percentToAnchor = (percentComplete/anchor) * 100

        lastProgressCheckedForEstimatedScore = getProgress()

        val score = ourHandler.getBaseBudget() ?: return "Error"
        if (percentToAnchor >= 100) return score.toString()
        if (percentToAnchor <= OVERGROWN_NANOFORGE_THRESHOLD_FOR_UNKNOWN_SCORE) return "Unknown"

        val variance = MathUtils.getRandomNumberInRange(0.7f, 1.3f)

        val min = (score*(OVERGROWN_NANOFORGE_MIN_SCORE_ESTIMATION_VARIANCE * percentToAnchor)*variance).coerceAtLeast(0f)
        val max = score*(OVERGROWN_NANOFORGE_MAX_SCORE_ESTIMATION_VARIANCE * percentToAnchor)*variance

        return "$min - $max"
    }


    override fun addEndStage() {
        addStage(overgrownNanoforgeFinishGrowthStage(this), getMaxProgress())
    }

    override fun addStages() {
        super.addStages()

        growthDiscoveryStages.TARGET.getChildren(this).forEach { addStage(it, it.getThreshold(), true) }
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
        val scoreWidth = 100f

        info.beginTable(factionForUIColors, 20f,
    "Target", targetWidth,
            "Overall Score", scoreWidth,
        )

        val targetData = getTargetData()

        val baseAlignment = Alignment.LMID
        val baseColor = Misc.getBasePlayerColor()

        info.addRowWithGlow(
            baseAlignment, baseColor, targetData.name,
            baseAlignment, baseColor, estimatedScore)
        targetData.industry?.let { info.setIdForAddedRow(it) }

        val opad = 5f
        info.addTable("None", -1, opad)
        info.addSpacer(3f)

        return info.prev
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

    override fun addTextAboveColonyMarker(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addTextAboveColonyMarker(info, width, stageId)

        info.addPara("This specific ${ourHandler.getCurrentName()} is %s and is %s. As such, its" +
                " abilities and characteristics are %s, and they will %s until it is %s.", 5f,
        Misc.getHighlightColor(), "not established", "still growing", "unknown", "not take effect", "fully grown")
    }

    override fun culled() {
        super.culled()

        delete() // needed since the junk handler has no ref to us
    }
}

class targetData(
    var industry: Industry?,
    val name: String = industry?.currentName ?: "None"
) {

}


class overgrownNanoforgeFinishGrowthStage(override val intel: overgrownNanoforgeGrowthIntel)
    : overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        intel.growingComplete()
    }

}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorStructureRegeneration
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelStage
import kotlin.math.max

open class baseOvergrownNanoforgeManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    open val ourHandler: overgrownNanoforgeHandler,
    hidden: Boolean = true,
    ) : baseOvergrownNanoforgeIntel(brain, hidden) {

    override fun addStartStage() {
        super.addStartStage()
        addStage(overgrownNanoforgeIntelCullStage(this), 0, false)
    }

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getMaxProgress())
    }

    override fun addFactors() {
        addRegenFactor()
        super.addFactors()
    }

    open fun addRegenFactor() {
        addFactorWrapped(overgrownNanoforgeIntelFactorStructureRegeneration(this))
    }

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)

        addParamsInfo(info, width, stageId)
    }

    override fun addTextAboveColonyMarker(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addTextAboveColonyMarker(info, width, stageId)

        info.addPara("This panel represents the integrity/growth status of a specific growth.", 5f)
        info.addPara("If the ${ourHandler.getCurrentName()}'s integrity reaches %s, it will be %s.", 5f,
            Misc.getHighlightColor(), "zero", "removed from the market")
    }

    open fun addParamsInfo(info: TooltipMakerAPI, width: Float, stageId: Any?): UIComponentAPI {
        val positiveEffects: String = getFormattedPositives()
        val negativeEffects: String = getFormattedNegatives()
        val positiveWidth = (info.computeStringWidth(positiveEffects)) + 30f
        val negativeWidth = (info.computeStringWidth(negativeEffects)) + 30f
        val sizePerEffect = 15f
        val size = (sizePerEffect * (max(positiveEffects.lines().size, negativeEffects.lines().size)))
        info.beginTable(
            factionForUIColors, size,
            "Positives", positiveWidth,
            "Negatives", negativeWidth,
        )
        info.makeTableItemsClickable()

        val baseAlignment = Alignment.LMID
        val baseColor = Misc.getBasePlayerColor()

        info.addRowWithGlow(
            baseAlignment, baseColor, positiveEffects,
            baseAlignment, baseColor, negativeEffects
        )

        val opad = 5f
        info.addTable("None", -1, opad)
        val table = info.prev
        info.addSpacer(3f)

        return table
    }

    open fun getFormattedPositives(): String {
        return ourHandler.getFormattedPositives()
    }

    open fun getFormattedNegatives(): String {
        return ourHandler.getFormattedNegatives()
    }

    open fun culled() {
        if (shouldReportCulled()) reportCulled()
        
        ourHandler.culled()
    }

    fun shouldReportCulled(): Boolean = getMarket().isPlayerOwned
    fun reportCulled() {
        Global.getSector().campaignUI.addMessage("thing culled lol")
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        if (areWeCulled()) {
            culled()
        }
    }

    fun areWeCulled(): Boolean {
        return (getProgress() <= 0)
    }

    override fun getName(): String {
        return "${ourHandler.getCurrentName()} on ${getMarket().name}"
    }

    override fun notifyPlayerAboutToOpenIntelScreen() {
        super.notifyPlayerAboutToOpenIntelScreen()
    }
}

class overgrownNanoforgeIntelCullStage(
    override val intel: baseOvergrownNanoforgeManipulationIntel
    ): overgrownNanoforgeIntelStage(intel) {

    override fun getName(): String = "Culled"
    override fun getDesc(): String = "et9iujpafwuijo"

    override fun stageReached() {
        intel.culled()
    }

}
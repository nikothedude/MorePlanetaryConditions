package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.CommMessageAPI
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeSpreadingBrain
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.overgrownNanoforgeIntelFactorStructureRegeneration
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffectDescData
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures
import data.utilities.niko_MPC_miscUtils.formatStringsToLines
import java.awt.Color
import kotlin.math.max

open class baseOvergrownNanoforgeManipulationIntel(
    brain: overgrownNanoforgeSpreadingBrain,
    open val ourHandler: overgrownNanoforgeHandler,
    ) : baseOvergrownNanoforgeIntel(brain) {

    override fun initializeProgress() {
        setMaxProgress(ourHandler.cullingResistance)
        setProgress(getMaxProgress())
    }

    override fun addInitialFactors() {
        addRegenFactor()
        super.addInitialFactors()
    }

    open fun addRegenFactor() {
        overgrownNanoforgeIntelFactorStructureRegeneration(this).init()
    }

    override fun addMiddleDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addMiddleDescriptionText(info, width, stageId)

        addParamsInfo(info, width, stageId)
    }

    override fun addBasicDescription(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        super.addBasicDescription(info, width, stageId)

        info.addPara("This panel represents the integrity/growth status of a specific growth.", 5f)
        info.addPara(
            "If the ${ourHandler.getCurrentName()}'s integrity reaches %s, it will be %s.", 5f,
            Misc.getHighlightColor(), "zero", "removed from the market"
        )
    }

    open fun addParamsInfo(info: TooltipMakerAPI, width: Float, stageId: Any?): UIComponentAPI {

        val positiveEffects: MutableList<overgrownNanoforgeEffectDescData> = getPositiveEffectDescData()
        val negativeEffects: MutableList<overgrownNanoforgeEffectDescData> = getNegativeEffectDescData()

        val positiveDesc = formatStringsToLines(positiveEffects.map { it.getFullString() })
        val negativeDesc = formatStringsToLines(negativeEffects.map { it.getFullString() })

        val positiveLines = positiveDesc.lines().size
        val negativeLines = negativeDesc.lines().size

        val positiveWidth = (info.computeStringWidth(positiveDesc)) + 30f
        val negativeWidth = (info.computeStringWidth(negativeDesc)) + 30f
        val sizePerEffect = 15f
        val size = (sizePerEffect * (max(positiveLines, negativeLines)))

        val baseColor = Misc.getBasePlayerColor()

        for (reason in ourHandler.getCategoryDisabledReasons()) {
            info.addPara(reason.key, 5f, Misc.getHighlightColor(), *(reason.value))
        }

        info.beginTable(
            factionForUIColors, size,
            "Positives", positiveWidth,
            "Negatives", negativeWidth,
        )
        info.makeTableItemsClickable()

        val baseAlignment = Alignment.LMID

        val positiveLabel = info.createLabel(positiveDesc, baseColor)
        val negativeLabel = info.createLabel(negativeDesc, baseColor)

        info.addRowWithGlow(
            baseAlignment, baseColor, positiveLabel,
            baseAlignment, baseColor, negativeLabel
        )

        val opad = 5f
        info.addTable("None", -1, opad)
        val table = info.prev
        info.addSpacer(3f)

        return table
    }

    open fun getPositiveEffectDescData(): MutableList<overgrownNanoforgeEffectDescData> {
        return ourHandler.getPositiveEffectDescData()
    }

    open fun getNegativeEffectDescData(): MutableList<overgrownNanoforgeEffectDescData> {
        return ourHandler.getNegativeEffectDescData()
    }

    /** Called when our progress goes to 0 or below. Should destroy the handler and structure as well as this. */
    open fun culled() {
        if (shouldReportCulled()) reportCulled()

        ourHandler.culled()
    }

    fun shouldReportCulled(): Boolean = playerCanManipulateGrowth()
    open fun reportCulled() {
        brain.intelCulled(this)

        sendCulledMessage()
    }

    open fun sendCulledMessage() {
        val linkedIntel = getIntelToLinkWhenCulled()
        val messageIntel = getCulledIntel()

        if (linkedIntel != null) {
            Global.getSector().campaignUI.addMessage(
                messageIntel,
                CommMessageAPI.MessageClickAction.INTEL_TAB,
                linkedIntel
            )
        } else {
            Global.getSector().campaignUI.addMessage(messageIntel)
        }
    }

    protected fun getCulledIntel(): MessageIntel {
        val intel = instantiateMessageIntelForCulled()
        return modifyCulledMessageIntel(intel)
    }

    protected open fun modifyCulledMessageIntel(intel: MessageIntel): MessageIntel {
        intel.icon = icon

        return intel
    }

    private fun instantiateMessageIntelForCulled(): MessageIntel {
        val text = getTextForCulled()
        val color = getCulledTextBaseColor()
        val highlights = getCulledTextHighlights()
        val highlightColors = getCulledTextHighlightColors()
        val messageIntel = MessageIntel(
            text,
            color,
            highlights,
            *highlightColors
        )
        return messageIntel
    }

    open fun getCulledTextBaseColor(): Color {
        return Misc.getTextColor()
    }

    protected open fun getTextForCulled(): String {
        return "%s on %s culled."
    }

    open fun getCulledTextHighlights(): Array<String> {
        return arrayOf(ourHandler.getCurrentName(), getMarket().name)
    }

    private fun getCulledTextHighlightColors(): Array<Color> {
        return arrayOf(Misc.getHighlightColor())
    }

    open fun getIntelToLinkWhenCulled(): baseOvergrownNanoforgeIntel? {
        return brain.spreadingIntel
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

    open fun getSpreadingAdjective(): String = "regenerating"
}
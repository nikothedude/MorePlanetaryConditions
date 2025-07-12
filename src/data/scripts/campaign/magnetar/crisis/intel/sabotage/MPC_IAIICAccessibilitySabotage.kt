package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero


class MPC_IAIICAccessibilitySabotage(market: MarketAPI, params: MPC_IAIICSabotageParams) : MPC_IAIICSabotage(market, params) {

    companion object {
        const val BASE_ACCESSIBILITY_MALUS = -0.2f
        const val MIN_PERCENT_OF_COLONIES = 0.4f
        const val MAX_COLONIES = 5f
        const val BASE_TIME_DAYS = 65f
        const val TIME_VARIATION = 14f
    }

    override val baseName: String = "Accessibility"

    override fun apply() {
        market.accessibilityMod.modifyFlat(id, getMalus(), name)
    }

    override fun unapply() {
        market.accessibilityMod.unmodify(id)
    }

    override fun createDesc(info: TooltipMakerAPI) {
        val label = info.addPara(
            "%s: Accessibility reduced by %s (%s)",
            5f,
            Misc.getHighlightColor(),
            name, "${(-getMalus() * 100f).roundNumTo(1).trimHangingZero()}%", getTimeLeftString()
        )
        label.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
    }

    private fun getMalus(): Float {
        return BASE_ACCESSIBILITY_MALUS * params.mult
    }

    override fun getPossibleNames(): MutableMap<String, Float> {
        return hashMapOf(
            Pair("Misleading Travel-Buoys", 1f),
            Pair("Damaged Fueljacks", 5f),
            Pair("STC Malware Infection", 5f),
            Pair("Bureaucracy DOS", 5f),
            Pair("Convoy Insecurity", 1f)
        )
    }
}
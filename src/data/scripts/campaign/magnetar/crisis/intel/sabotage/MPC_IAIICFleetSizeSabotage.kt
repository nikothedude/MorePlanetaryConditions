package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_IAIICFleetSizeSabotage(market: MarketAPI, params: MPC_IAIICSabotageParams): MPC_IAIICSabotage(market, params) {

    companion object {
        const val BASE_SIZE_MALUS = -0.3f
        const val MIN_PERCENT_OF_COLONIES = 0.33f
        const val MAX_COLONIES = 5f
        const val BASE_TIME_DAYS = 65f
        const val TIME_VARIATION = 14f
    }

    override val baseName: String = "Fleet Size"

    override fun apply() {
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, getMalus(), name)
    }

    override fun unapply() {
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
    }

    override fun createDesc(info: TooltipMakerAPI) {
        val label = info.addPara(
            "%s: Fleet size reduced by %s (%s)",
            5f,
            Misc.getHighlightColor(),
            name, "${(-getMalus() * 100f).roundNumTo(1).trimHangingZero()}%", getTimeLeftString()
        )
        label.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
    }

    private fun getMalus(): Float {
        return BASE_SIZE_MALUS * params.mult
    }

    override fun getPossibleNames(): MutableMap<String, Float> {
        return hashMapOf(
            Pair("Military Partisans", 5f),
            Pair("Merc Ambushes", 5f),
            Pair("Spacecraft Embezzlement", 5f),
        )
    }
}
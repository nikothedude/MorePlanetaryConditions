package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.ceil

class MPC_IAIICUnrestSabotage(market: MarketAPI, params: MPC_IAIICSabotageParams) : MPC_IAIICSabotage(market, params) {

    // unrest is a weird one
    // you can stabilize the colony for 200k, removing an unrest point
    // unrest is also long term and only decays at a rate of 1 per 3 months
    companion object {
        const val BASE_STABILITY_MALUS = 2.25f
        const val MIN_PERCENT_OF_COLONIES = 0.2f
        const val MAX_COLONIES = 5f
    }

    override val baseName: String = "Unrest"

    override fun apply() {
        RecentUnrest.get(market)?.add(getMalus(), name)
        remove()
    }

    private fun getMalus(): Int {
        return ceil(BASE_STABILITY_MALUS * params.mult).toInt()
    }

    override fun unapply() {
        return
    }

    override fun createDesc(info: TooltipMakerAPI) {
        val label = info.addPara(
            "%s: Unrest increased by %s",
            5f,
            Misc.getHighlightColor(),
            name, "${getMalus()}%"
        )
        label.setHighlightColors(Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
    }

    override fun getPossibleNames(): MutableMap<String, Float> {
        return hashMapOf(
            Pair("Government Kidnappings", 5f),
            Pair("Anti-AI Protests", 5f),
            Pair("Partisan Instigators", 5f)
        )
    }
}
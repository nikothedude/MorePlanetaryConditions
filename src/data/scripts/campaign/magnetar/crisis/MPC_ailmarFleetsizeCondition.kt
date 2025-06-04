package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.crisis.MPC_ailmarFleetsizeScript.Companion.getDaysLeft
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import org.magiclib.kotlin.getFactionMarkets


class MPC_ailmarFleetsizeCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val SIZE_MULT = 0.8f
        const val AILMAR_SIZE_MULT = 3f
        const val DURATION = 365f

        fun getAilmar(): MarketAPI? = Global.getSector().economy.getMarket("ailmar")
        fun isAilmar(market: MarketAPI) = market.id == "ailmar"
    }

    val interval = IntervalUtil(DURATION, DURATION)

    override fun apply(id: String) {
        super.apply(id)

        if (!showIcon()) return

        if (isAilmar(market)) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, AILMAR_SIZE_MULT, name)
        } else if (market.isPlayerOwned) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(id, SIZE_MULT, name)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
    }

    override fun getName(): String? {
        if (isAilmar(market)) return "Donated Dockyards" else return super.name
    }

    override fun showIcon(): Boolean {
        if (isAilmar(market)) return true
        if (market.isPlayerOwned) return true
        if (Global.getSector().getFaction(Factions.PLAYER).getFactionMarkets().isEmpty()) return false
        return false
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (isAilmar(market)) {
            tooltip.addPara(
                "Fleet size increased by %s",
                5f,
                Misc.getHighlightColor(),
                "${AILMAR_SIZE_MULT.trimHangingZero()}x"
            )
        } else {
            tooltip.addPara(
                "Fleet size decreased by %s",
                5f,
                Misc.getHighlightColor(),
                "${SIZE_MULT.trimHangingZero()}x"
            )
        }

        tooltip.addSpacer(5f)
        tooltip.addPara(
            "%s days left",
            5f,
            Misc.getHighlightColor(),
            "${getDaysLeft()}"
        )
    }

    override fun isTransient(): Boolean = false
}
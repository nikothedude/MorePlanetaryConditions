package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition

class MPC_FOBCondition: niko_MPC_baseNikoCondition() {
    companion object {
        const val GROUND_DEFENSE_MULT = 1.25f
        const val ACCESSABILITY_MALUS = -10f
        const val STABILITY_BONUS = 1f

        const val MAX_DISRUPTED_DURATION_DAYS = 30f
        const val MAX_MARKET_SIZE = 5
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.accessibilityMod.modifyPercent(id, ACCESSABILITY_MALUS, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, GROUND_DEFENSE_MULT, name)
        market.stability.modifyFlat(id, STABILITY_BONUS, name)

        market.industries.forEach {
            if (it.disruptedDays > MAX_DISRUPTED_DURATION_DAYS) {
                it.setDisrupted(MAX_DISRUPTED_DURATION_DAYS)
            }
        }

        if (market.size >= MAX_MARKET_SIZE) {
            market.population.weight.modifyMult(id, 0f, "Maximum size reached")
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        if (id == null) return
        val market = getMarket() ?: return
        market.accessibilityMod.unmodify(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        market.stability.unmodify(id)

        market.population.weight.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "Colony size limited to %s",
            5f,
            Misc.getNegativeHighlightColor(),
            "$MAX_MARKET_SIZE"
        )
        tooltip.addPara(
            "%s accessibility",
            5f,
            Misc.getNegativeHighlightColor(),
            "${ACCESSABILITY_MALUS.toInt()}%"
        )

        tooltip.addPara(
            "%s ground defense rating",
            5f,
            Misc.getHighlightColor(),
            "${GROUND_DEFENSE_MULT}x"
        )
        tooltip.addPara(
            "%s stability",
            5f,
            Misc.getHighlightColor(),
            "${STABILITY_BONUS.toInt()}"
        )
        tooltip.addPara(
            "Industries can be disrupted for no longer than %s",
            5f,
            Misc.getHighlightColor(),
            "${MAX_DISRUPTED_DURATION_DAYS.toInt()} days"
        )
    }
}
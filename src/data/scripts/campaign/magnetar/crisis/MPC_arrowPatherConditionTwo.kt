package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition

class MPC_arrowPatherConditionTwo: niko_MPC_baseNikoCondition() {

    companion object {
        const val FLEET_SIZE_MULT = 1.6f
        const val GROUND_DEFENSE_INCREMENT = 100f
    }

    override fun apply(id: String) {
        super.apply(id)
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(modId, FLEET_SIZE_MULT, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(modId, GROUND_DEFENSE_INCREMENT, name)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(modId)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(modId)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        tooltip.addPara(
            "%s fleet size",
            10f,
            Misc.getHighlightColor(),
            "+${FLEET_SIZE_MULT}x"
        )

        tooltip.addPara(
            "%s ground defense rating",
            10f,
            Misc.getHighlightColor(),
            "+${GROUND_DEFENSE_INCREMENT.toInt()}"
        )
    }
}
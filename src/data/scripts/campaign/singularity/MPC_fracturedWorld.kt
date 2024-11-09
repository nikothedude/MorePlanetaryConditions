package data.scripts.campaign.singularity

import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.magnetar.niko_MPC_magnetarCondition
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_fracturedWorld: niko_MPC_baseNikoCondition() {

    companion object {
        const val HAZARD_INCREMENT = 3f
        const val ACCESSABILITY_DECREMENT = -1f

        const val TOTAL_ORE_BONUS = 5f
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.hazard.modifyFlat(id, HAZARD_INCREMENT, name)
        market.accessibilityMod.modifyFlat(id, ACCESSABILITY_DECREMENT, name)

        market.industries.filter { it.spec.hasTag(Industries.MINING) }.forEach {
            it.getSupply(Commodities.ORE).quantity.modifyFlat(id, TOTAL_ORE_BONUS, name)
            it.getSupply(Commodities.RARE_ORE).quantity.modifyFlat(id, TOTAL_ORE_BONUS, name)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        val market = getMarket() ?: return
        market.hazard.unmodify(id)
        market.accessibilityMod.unmodify(id)

        market.industries.filter { it.spec.hasTag(Industries.MINING) }.forEach {
            it.getSupply(Commodities.ORE).quantity.unmodify(id)
            it.getSupply(Commodities.RARE_ORE).quantity.unmodify(id)
        }
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        tooltip.addPara(
            "%s hazard rating",
            10f,
            Misc.getHighlightColor(),
            "+${(HAZARD_INCREMENT * 100).toInt()}%"
        )

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            "${(ACCESSABILITY_DECREMENT * 100).toInt()}%"
        )

        tooltip.addPara(
            "%s ore and rare ore production (mining)",
            10f,
            Misc.getHighlightColor(),
            "+${(TOTAL_ORE_BONUS.toInt())}"
        )
    }
}
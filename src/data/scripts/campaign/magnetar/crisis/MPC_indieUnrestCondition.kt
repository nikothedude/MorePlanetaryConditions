package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_mathUtils.roundNumTo
import kotlin.math.ceil
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_indieUnrestCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val ACCESSIBILITY_MALUS = 0.5f
        const val STABILITY_MALUS = 2f
    }

    override fun apply(id: String) {
        super.apply(id)

        val stabMalus = getStabilityMalus()
        val accessMalus = getAccessMalus()

        if (market == null) return
        market.accessibilityMod.modifyFlat(id, -accessMalus, name)
        market.stability.modifyFlat(id, -stabMalus, name)
    }

    private fun getAccessMalus(): Float {
        var localMalus = ACCESSIBILITY_MALUS
        if (market.size <= 3) localMalus *= 0.5f
        return ((localMalus) * MPC_indieUnrestScript.getUnrestLevel()).roundNumTo(2)
    }

    private fun getStabilityMalus(): Float {
        return ceil(STABILITY_MALUS * MPC_indieUnrestScript.getUnrestLevel())
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.accessibilityMod.unmodify(id)
        market.stability.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        val stabMalus = getStabilityMalus()
        val accessMalus = getAccessMalus()

        tooltip.addPara(
            "%s stability",
            5f,
            Misc.getNegativeHighlightColor(),
            "-${stabMalus.toInt()}"
        )

        tooltip.addPara(
            "%s accessibility",
            5f,
            Misc.getNegativeHighlightColor(),
            "-${(accessMalus * 100f).trimHangingZero()}%"
        )

        tooltip.addPara(
            "The exact length of this disruption cannot be strictly estimated, but its likely it will begin winding down after a %s.",
            5f,
            Misc.getHighlightColor(),
            "month"
        )
    }

}
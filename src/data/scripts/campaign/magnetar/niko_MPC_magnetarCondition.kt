package data.scripts.campaign.magnetar

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.econ.conditions.terrain.magfield.niko_MPC_hyperMagneticField

class niko_MPC_magnetarCondition: niko_MPC_baseNikoCondition() {

    companion object {
        const val HAZARD_INCREMENT = 1.5f
        const val ACCESSABILITY_DECREMENT = -0.75f
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.hazard.modifyFlat(id, HAZARD_INCREMENT, name)
        market.accessibilityMod.modifyFlat(id, ACCESSABILITY_DECREMENT, name)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        val market = getMarket() ?: return
        market.hazard.unmodify(id)
        market.accessibilityMod.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "While traditional logic would stipulate a %s could help, the magnetar's %s would quickly %s",
            10f,
            Misc.getHighlightColor(),
            "planetary shield", "ionized pulses", "destroy it"
        )

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
    }
}
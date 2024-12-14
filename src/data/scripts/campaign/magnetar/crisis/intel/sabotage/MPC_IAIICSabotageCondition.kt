package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition

class MPC_IAIICSabotageCondition: niko_MPC_baseNikoCondition() {

    companion object {
        fun get(market: MarketAPI): MPC_IAIICSabotageCondition {
            if (!market.hasCondition("MPC_IAIICSabotageCondition")) {
                market.addCondition("MPC_IAIICSabotageCondition")
            }
            return market.getCondition("MPC_IAIICSabotageCondition").plugin as MPC_IAIICSabotageCondition
        }

        fun MarketAPI.addSabotage(sabotage: MPC_IAIICSabotage) {
            get(this).sabotage += sabotage
            sabotage.apply()
        }
        fun MarketAPI.removeSabotage(sabotage: MPC_IAIICSabotage) {
            sabotage.unapply()
            get(this).sabotage -= sabotage
        }
    }

    val sabotage: MutableSet<MPC_IAIICSabotage> = HashSet()
    override fun advance(amount: Float) {
        super.advance(amount)

        val days = Misc.getDays(amount)
        for (sabotageInstance in sabotage.toList()) {
            sabotageInstance.adjustTimeLeft(-days)
        }
    }

    override fun apply(id: String) {
        super.apply(id)

        sabotage.forEach { it.apply() }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        sabotage.forEach { it.unapply() }
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addSectionHeading("Active sabotage", Alignment.MID, 10f)
        tooltip.addSpacer(5f)
        sabotage.forEach {
            it.createDesc(tooltip)
        }
    }

    override fun showIcon(): Boolean {
        return sabotage.isNotEmpty()
    }

    override fun isTransient(): Boolean = false
}
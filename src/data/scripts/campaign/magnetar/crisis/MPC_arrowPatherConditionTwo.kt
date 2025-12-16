package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_mathUtils.roundNumTo
import sound.int

class MPC_arrowPatherConditionTwo: niko_MPC_baseNikoCondition() {

    companion object {
        const val FLEET_SIZE_MULT = 1.7f
        const val GROUND_DEFENSE_INCREMENT = 300f
        const val TEMP_GROUND_DEFENSE_MULT = 5f
    }

    override fun isTransient(): Boolean {
        return false
    }

    var defenseActive = true
    val interval = IntervalUtil(90f, 90f) // days

    override fun advance(amount: Float) {
        super.advance(amount)

        if (defenseActive) {
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                defenseActive = false
            }
        }
    }

    override fun showIcon(): Boolean {
        return isOwnedByPath()
    }

    override fun apply(id: String) {
        super.apply(id)

        if (!isOwnedByPath()) return

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult(modId, FLEET_SIZE_MULT, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(modId, GROUND_DEFENSE_INCREMENT, name)
        if (defenseActive) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult("${modId}_mult", TEMP_GROUND_DEFENSE_MULT, name)
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(modId)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(modId)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify("${modId}_mult")

    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        if (!isOwnedByPath()) {

            tooltip.addPara(
                "Owned by an opposing polity, the arrow pathers sit, and wait, for their inevitable return...",
                10f
            )

            return
        }

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

        if (defenseActive) {
            val daysLeft = (interval.intervalDuration - interval.elapsed).roundNumTo(1)
            tooltip.addPara(
                "The arrow pathers are particularly wary of you, and have massively ramped up security in the wake of your donation. For the next %s, defense rating is also increased by %s.",
                10f,
                Misc.getNegativeHighlightColor(),
                "$daysLeft days", "${TEMP_GROUND_DEFENSE_MULT.toInt()}x"
            )
        }
    }

    fun isOwnedByPath(): Boolean = market.factionId == Factions.LUDDIC_PATH
}
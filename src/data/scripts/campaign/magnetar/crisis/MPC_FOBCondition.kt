package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition

class MPC_FOBCondition: niko_MPC_baseNikoCondition() {
    companion object {
        const val GROUND_DEFENSE_MULT = 3f
        const val ACCESSABILITY_MALUS = -50f
        const val STABILITY_BONUS = 4f

        const val FLEET_SIZE_MULT = 250f
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.accessibilityMod.modifyPercent(id, ACCESSABILITY_MALUS)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, GROUND_DEFENSE_MULT)
        market.stability.modifyFlat(id, STABILITY_BONUS)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        if (id == null) return
        val market = getMarket() ?: return
        market.accessibilityMod.unmodify(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        market.stability.unmodify(id)
    }
}
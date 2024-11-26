package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats

class MPC_planetaryOperations {

    companion object {
        var LEVEL_1_BONUS = 50
        var STABILITY_BONUS = 1f
    }


    class Market1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyMult(id, 1f + LEVEL_1_BONUS * 0.01f, "Planetary operations")
            market.stability.modifyFlat(id, STABILITY_BONUS, "Planetary operations")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id)
            market.stability.unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+" + LEVEL_1_BONUS + "% effectiveness of ground defenses, and +" + STABILITY_BONUS.toInt() + " stability"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }
}
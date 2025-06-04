package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.utilities.niko_MPC_stringUtils.toPercent
import data.utilities.niko_MPC_mathUtils.trimHangingZero

class MPC_battlemindCampaign {

    companion object {
        const val GROUND_DEFENSE_MULT = 2f
    }

    class Level1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, GROUND_DEFENSE_MULT, "Battlemind")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.hazard.unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+${GROUND_DEFENSE_MULT.trimHangingZero()}x effectiveness of ground defenses"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level2 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            return
        }

        override fun unapply(market: MarketAPI, id: String) {
            return
        }

        override fun getEffectDescription(level: Float): String {
            return "Spawns exotic patrols that patrol in-system and in hyperspace"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }
}
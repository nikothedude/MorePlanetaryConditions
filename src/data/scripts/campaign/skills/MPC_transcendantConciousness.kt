package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.skills.Hypercognition
import kotlin.math.roundToInt

class MPC_transcendantConciousness {


    companion object {
        const val HAZARD_REDUCTION = -0.25f
    }

    class Level1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.hazard.modifyFlat(id, HAZARD_REDUCTION, "Transcendent Consciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.hazard.unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "$HAZARD_REDUCTION% hazard rating"
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
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                .modifyFlat(id, Hypercognition.FLEET_SIZE / 100f, "Hypercognition")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            //return "" + (int)Math.round(FLEET_SIZE) + "% larger fleets";
            return "+" + Math.round(Hypercognition.FLEET_SIZE) + "% fleet size"
        }

        override fun getEffectPerLevelDescription(): String {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level3 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyMult(id, 1f + Hypercognition.DEFEND_BONUS * 0.01f, "Hypercognition")
        }

        override fun unapply(market: MarketAPI, id: String) {
            //market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyPercent(id);
            market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+" + Hypercognition.DEFEND_BONUS + "% effectiveness of ground defenses"
        }

        override fun getEffectPerLevelDescription(): String {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level4 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stability.modifyFlat(id, Hypercognition.STABILITY_BONUS, "Hypercognition")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stability.unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+" + Hypercognition.STABILITY_BONUS.toInt() + " stability"
        }

        override fun getEffectPerLevelDescription(): String {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

}
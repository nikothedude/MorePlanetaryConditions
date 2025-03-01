package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats
import data.utilities.niko_MPC_stringUtils.toPercent

class MPC_transcendantConciousness {


    companion object {
        const val HAZARD_REDUCTION = -0.25f
        //const val UPKEEP_MULT = 0.25f
        const val FLEET_SIZE_MOD = 0.20f
        const val ACCESSIBILITY_BONUS = 0.10f
        const val NUM_INDUSTRIES_INCREASE = 1f
    }

    class Level1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.hazard.modifyFlat(id, HAZARD_REDUCTION, "Transcendent Consciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.hazard.unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "${(HAZARD_REDUCTION*100).toInt()}% hazard rating"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    /*class Level2 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.upkeepMult.modifyMult(id, UPKEEP_MULT, "Transcendent Conciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.upkeepMult.unmodify(id)

        }

        override fun getEffectDescription(level: Float): String {
            return "${toPercent(UPKEEP_MULT)} less upkeep for all buildings"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }*/

    class Level3 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat(id, FLEET_SIZE_MOD, "Transcendent Consciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+${toPercent(FLEET_SIZE_MOD)} fleet size"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level4 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.accessibilityMod.modifyFlat(id, ACCESSIBILITY_BONUS, "Transcendent Consciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.accessibilityMod.unmodifyFlat(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+${(ACCESSIBILITY_BONUS * 100).toInt()}% accessibility"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

    class Level5: MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).modifyFlat(id, NUM_INDUSTRIES_INCREASE, "Transcendent Consciousness")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+${NUM_INDUSTRIES_INCREASE.toInt()} max industries"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }
}
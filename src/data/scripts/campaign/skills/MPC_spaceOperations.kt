package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats

class MPC_spaceOperations {

    companion object {
        const val SHIP_QUALITY_PERCENT = 0.25f
        fun getPatrol(market: MarketAPI): MilitaryBase? = (market.getIndustry(Industries.MILITARYBASE) ?: market.getIndustry(Industries.HIGHCOMMAND) ?: market.getIndustry(Industries.PATROLHQ)) as? MilitaryBase
    }

    class Market1: MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.stats.dynamic.getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).modifyFlat(id, 1f)
            market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyFlat(id, SHIP_QUALITY_PERCENT, "Space Operations")
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.stats.dynamic.getStat(Stats.COMBAT_FLEET_SPAWN_RATE_MULT).unmodify(id)
            market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "${100f + SHIP_QUALITY_PERCENT}% ship quality, as well as hastened patrol respawns"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.GOVERNED_OUTPOST
        }
    }
}
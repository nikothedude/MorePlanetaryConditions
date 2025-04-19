package data.scripts.campaign.skills

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.LevelBasedEffect.ScopeDescription
import com.fs.starfarer.api.characters.MarketSkillEffect
import com.fs.starfarer.api.impl.campaign.ids.Stats
import kotlin.math.roundToInt

class MPC_fleetLogistics {

    companion object {
        const val ACCESS_1 = 0.15f
        const val FLEET_SIZE = 25f
    }

    class Market1 : MarketSkillEffect {
        override fun apply(market: MarketAPI, id: String, level: Float) {
            market.accessibilityMod.modifyFlat(id, ACCESS_1, "Fleet logistics")
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT)
                .modifyFlat(id, FLEET_SIZE / 100f, "Fleet logistics")

            market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).modifyFlat(id, 1f)
        }

        override fun unapply(market: MarketAPI, id: String) {
            market.accessibilityMod.unmodifyFlat(id)
            market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodifyFlat(id)

            market.stats.dynamic.getMod(Stats.MAX_INDUSTRIES).unmodify(id)
        }

        override fun getEffectDescription(level: Float): String {
            return "+" + (ACCESS_1 * 100f).roundToInt() + "% accessibility, and " + FLEET_SIZE.roundToInt() + "% larger fleets"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): ScopeDescription {
            return ScopeDescription.GOVERNED_OUTPOST
        }
    }

}
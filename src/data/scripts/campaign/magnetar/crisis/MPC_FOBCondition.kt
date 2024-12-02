package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketImmigrationModifier
import com.fs.starfarer.api.impl.campaign.econ.impl.PopulationAndInfrastructure
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.population.PopulationComposition
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_stringUtils

class MPC_FOBCondition: niko_MPC_baseNikoCondition(), MarketImmigrationModifier {
    companion object {
        const val ACCESSABILITY_MALUS = -10f
        const val STABILITY_BONUS = 3f

        const val MAX_DISRUPTED_DURATION_DAYS = 30f
        const val MAX_MARKET_SIZE = 5

        const val PATROL_LIGHT_INCREMENT = 4f
        const val PATROL_MED_INCREMENT = 2f
        const val PATROL_HEAVY_INCREMENT = 1f

        const val FS_MARKET_SIZE_INCREMENT = 2
    }

    override fun modifyIncoming(market: MarketAPI?, incoming: PopulationComposition?) {
        if (market == null || incoming == null) return

        if (market.size >= MAX_MARKET_SIZE) {
            incoming.weight.modifyMult(modId, 0f, "Maximum size reached")
        }
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.addTransientImmigrationModifier(this)
        market.accessibilityMod.modifyPercent(id, ACCESSABILITY_MALUS, name)
        market.stability.modifyFlat(id, STABILITY_BONUS, name)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyFlat(id, getDefenseRatingBonus(), "$name (Extra market size)")
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyPercent(id, getFleetSizeBonus(), "$name (Extra market size)")

        market.stats.dynamic.getMod(Stats.PATROL_NUM_LIGHT_MOD).modifyFlat(id, PATROL_LIGHT_INCREMENT, name)
        market.stats.dynamic.getMod(Stats.PATROL_NUM_MEDIUM_MOD).modifyFlat(id, PATROL_MED_INCREMENT, name)
        market.stats.dynamic.getMod(Stats.PATROL_NUM_HEAVY_MOD).modifyFlat(id, PATROL_HEAVY_INCREMENT, name)

        market.industries.forEach {
            if (it.disruptedDays > MAX_DISRUPTED_DURATION_DAYS) {
                it.setDisrupted(MAX_DISRUPTED_DURATION_DAYS)
            }
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        if (id == null) return
        val market = getMarket() ?: return
        market.removeTransientImmigrationModifier(this)
        market.accessibilityMod.unmodify(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
        market.stability.unmodify(id)
        market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).unmodify(id)

        market.stats.dynamic.getMod(Stats.PATROL_NUM_LIGHT_MOD).unmodify(id)
        market.stats.dynamic.getMod(Stats.PATROL_NUM_MEDIUM_MOD).unmodify(id)
        market.stats.dynamic.getMod(Stats.PATROL_NUM_HEAVY_MOD).unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "%s stability",
            10f,
            Misc.getHighlightColor(),
            "+${STABILITY_BONUS.toInt()}"
        )
        tooltip.addPara(
            "%s for fleet size & ground defense calculations",
            5f,
            Misc.getHighlightColor(),
            "+$FS_MARKET_SIZE_INCREMENT market size"
        )
        tooltip.addPara(
            "%s launched from this colony",
            5f,
            Misc.getHighlightColor(),
            "+${PATROL_LIGHT_INCREMENT.toInt()}/${PATROL_MED_INCREMENT.toInt()}/${PATROL_HEAVY_INCREMENT.toInt()} light/medium/heavy patrols"
        )
        tooltip.addPara(
            "Industries can be disrupted for no longer than %s",
            5f,
            Misc.getHighlightColor(),
            "${MAX_DISRUPTED_DURATION_DAYS.toInt()} days"
        )

        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getNegativeHighlightColor(),
            "${ACCESSABILITY_MALUS.toInt()}%"
        )
        tooltip.addPara(
            "Colony size limited to %s",
            5f,
            Misc.getNegativeHighlightColor(),
            "$MAX_MARKET_SIZE"
        )
    }

    fun getEffectiveFleetSizeMult(): Float {
        return FleetFactoryV3.getNumShipsMultForMarketSize(market.size.toFloat() + FS_MARKET_SIZE_INCREMENT)
    }

    fun getFleetSizeBonus(): Float {
        return (getEffectiveFleetSizeMult() - FleetFactoryV3.getNumShipsMultForMarketSize(market.size.toFloat())) * 100f
    }

    fun getEffectiveDefenseRating(): Float {
        return PopulationAndInfrastructure.getBaseGroundDefenses(market.size + FS_MARKET_SIZE_INCREMENT)
    }

    fun getDefenseRatingBonus(): Float {
        return getEffectiveDefenseRating() - PopulationAndInfrastructure.getBaseGroundDefenses(market.size)
    }
}
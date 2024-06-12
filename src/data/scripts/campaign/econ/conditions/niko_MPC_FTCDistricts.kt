package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus
import data.utilities.niko_MPC_marketUtils.isInhabited

class niko_MPC_FTCDistricts: niko_MPC_baseNikoCondition() {

    companion object {
        val sameFactionSpeedBonus = 1f
        val sameFactionSensorBonus = 350f

        val baseSlipstreamDetectionRadius = 3f

        val groundDefenseMult = 1.1f
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val market = getMarket() ?: return
        if (market.isInHyperspace) return
        if (!market.isInhabited()) return
        val containingLocation = market.containingLocation ?: return

        val id = modId
        for (fleet in containingLocation.fleets) {
            if (fleet.isInHyperspaceTransition) continue
            val reqRep = fleet.getRepLevelForArrayBonus()
            if (fleet.faction.getRelationshipLevel(market.faction) >= reqRep) {
                val burnMod = fleet.stats.fleetwideMaxBurnMod.getFlatBonus(id + "_burnBonus")
                if (burnMod == null || burnMod.value <= sameFactionSpeedBonus) {
                    fleet.stats.addTemporaryModFlat(
                        0.25f, id + "_burnBonus",
                        "${market.name} $name", sameFactionSpeedBonus,
                        fleet.stats.fleetwideMaxBurnMod
                    )
                }

                val sensorMod = fleet.stats.sensorRangeMod.getFlatBonus(id + "_sensorBonus")
                if (sensorMod == null || sensorMod.value <= sameFactionSensorBonus) {
                    fleet.stats.addTemporaryModFlat(
                        0.25f, id + "_sensorBonus",
                        "${market.name} $name", sameFactionSensorBonus,
                        fleet.stats.sensorRangeMod
                    )
                }
            }
        }
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return

        if (market.isPlayerOwned) {
            val intel = HyperspaceTopographyEventIntel.get()
            if (intel != null && intel.isStageActive(HyperspaceTopographyEventIntel.Stage.SLIPSTREAM_DETECTION)) {
                market.stats.dynamic.getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).modifyFlat(
                    id, baseSlipstreamDetectionRadius, name
                )
            }
        }
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).modifyMult(id, groundDefenseMult, name)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        if (id == null) return
        val market = getMarket() ?: return

        market.stats.dynamic.getMod(Stats.SLIPSTREAM_REVEAL_RANGE_LY_MOD).unmodifyFlat(id)
        market.stats.dynamic.getMod(Stats.GROUND_DEFENSES_MOD).unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "%s slipstream detection radius",
            10f,
            Misc.getHighlightColor(),
            "+${baseSlipstreamDetectionRadius.toInt()} ly"
        )

        tooltip.addPara(
            "%s defense rating",
            10f,
            Misc.getHighlightColor(),
            "${groundDefenseMult}x"
        )

        tooltip.addPara(
            "Friendly/Trade fleets in system get %s and %s",
            10f,
            Misc.getHighlightColor(),
            "+${sameFactionSpeedBonus.toInt()} max burn", "+${sameFactionSensorBonus.toInt()} sensor range"
        )

    }
}
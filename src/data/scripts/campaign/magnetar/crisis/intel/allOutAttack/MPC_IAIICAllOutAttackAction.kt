package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.group.FGRaidAction
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils

class MPC_IAIICAllOutAttackAction(params: FGRaidParams?, raidDays: Float) : FGRaidAction(params, raidDays) {
    override fun performRaid(fleet: CampaignFleetAPI?, market: MarketAPI?) {

        if (bombardCount < 0f) {
            return tryBombardFirst(fleet, market)
        }

        raidCount.add(market)

        var faction = intel.faction
        if (fleet != null) {
            faction = fleet.faction
        }

        var raidStr = intel.route.extra.strengthModifiedByDamage / intel.approximateNumberOfFleets *
                Misc.FP_TO_GROUND_RAID_STR_APPROX_MULT
        if (fleet != null) {
            raidStr = MarketCMD.getRaidStr(fleet)
        }
        var industry: Industry? = null
        var index = raidCount.getCount(market) - 1
        if (index < 0) index = 0
        if (params.disrupt != null && index < params.disrupt.size) {
            var count = 0
            for (industryId in params.disrupt) {
                if (market!!.hasIndustry(industryId)) {
                    if (count >= index) {
                        industry = market!!.getIndustry(industryId)
                        break
                    }
                    count++
                }
            }
            //				String industryId = params.disrupt.get(index);
//				industry = market.getIndustry(industryId);
        }
        if (intel is GenericRaidFGI && (intel as GenericRaidFGI).hasCustomRaidAction()) {
            (intel as GenericRaidFGI).doCustomRaidAction(fleet, market, raidStr)
            Misc.setFlagWithReason(
                market!!.memoryWithoutUpdate, MemFlags.RECENTLY_RAIDED,
                faction.id, true, 30f
            )
            Misc.setRaidedTimestamp(market)
        } else if (industry != null) {
            val durMult = Global.getSettings().getFloat("punitiveExpeditionDisruptDurationMult")
            MarketCMD(market!!.primaryEntity).doIndustryRaid(faction, raidStr, industry, durMult)
        } else {
            MarketCMD(market!!.primaryEntity).doGenericRaid(
                faction,
                raidStr, params.maxStabilityLostPerRaid.toFloat(), params.raidsPerColony > 1
            )
        }
    }

    private fun tryBombardFirst(fleet: CampaignFleetAPI?, market: MarketAPI?) {
        val cost = MarketCMD.getBombardmentCost(market, fleet).toFloat()
        var bombardStr = intel.route.extra.strengthModifiedByDamage / intel.approximateNumberOfFleets *
                Misc.FP_TO_BOMBARD_COST_APPROX_MULT
        if (fleet != null) {
            val fleetsInRadius = intel.fleets.filter { MathUtils.getDistance(fleet, it) <= 1000f } + fleet
            bombardStr = 0f
            fleetsInRadius.forEach { bombardStr += it.cargo.maxFuel * 0.5f } // EDIT - Takes from nearby fleets, it just fails otherwise for some reason
        }
        if (cost <= bombardStr) {
            MarketCMD(market!!.primaryEntity).doBombardment(
                intel.faction,
                MarketCMD.BombardType.TACTICAL
            )
            bombardCount++
        } else {
            Misc.setFlagWithReason(
                market!!.memoryWithoutUpdate, MemFlags.RECENTLY_BOMBARDED,
                intel.faction.id, true, 30f
            )
        }
    }
}
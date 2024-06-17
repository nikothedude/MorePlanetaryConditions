package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_marketUtils.applyDeficitToProductionStatic
import data.utilities.niko_MPC_stringUtils.toPercent

class niko_MPC_carnivorousFauna: niko_MPC_baseNikoCondition() {

    companion object {

        const val HAZARD_INCREASE_UNSUPPRESSED = 0.50f

        // remember: adequete farmland has an output of 3 food
        val INDUSTRY_TO_HAZARD_MULT = hashMapOf(
            Pair(Industries.TAG_PATROL, 0.2f),
            Pair(Industries.TAG_MILITARY, 0.5f),
            Pair(Industries.TAG_COMMAND, 0.8f)
        )
        val INDUSTRY_TO_BASE_FOOD_PROD = hashMapOf(
            Pair(Industries.TAG_PATROL, 2),
            Pair(Industries.TAG_MILITARY, 3),
            Pair(Industries.TAG_COMMAND, 4)
        )
        val INDUSTRY_TO_BASE_LUXURY_PROD = hashMapOf(
            Pair(Industries.TAG_PATROL, 1),
            Pair(Industries.TAG_MILITARY, 2),
            Pair(Industries.TAG_COMMAND, 3)
        )
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.hazard.modifyFlat(id, getHazardIncrease(), name)
        applyCommodityProduction()
    }

    private fun applyCommodityProduction() {
        for (industry in market.industries) {
            var shouldCheckDeficits = false

            val foodAmount = getFoodOutputForIndustry(industry)
            if (foodAmount > 0) {
                industry.supply(modId, Commodities.FOOD, foodAmount, name)
                shouldCheckDeficits = true
            }
            val luxuryGoodsAmount = getLuxuryGoodsForIndustry(industry)
            if (luxuryGoodsAmount > 0) {
                industry.supply(modId, Commodities.LUXURY_GOODS, luxuryGoodsAmount, name)
                shouldCheckDeficits = true
            }
            if (shouldCheckDeficits) {
                checkDeficits(industry)
            }
        }

    }

    private fun checkDeficits(industry: Industry) {
        val deficits = getSupplyAndFuelDeficit(industry) ?: return
        if (deficits.one == null) {
            deficits.one = Commodities.FUEL // in this case, we have no deficits so this works to unapply (i think)
        }
        val index = 9

        industry.applyDeficitToProductionStatic(index, deficits, Commodities.FOOD, Commodities.LUXURY_GOODS)
    }

    private fun getSupplyAndFuelDeficit(industry: Industry): com.fs.starfarer.api.util.Pair<String, Int>? {
        return industry.getMaxDeficit(Commodities.SUPPLIES, Commodities.FUEL)
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        if (id == null) return

        market.hazard.unmodify(id)
        unapplyCommodityProduction()
    }

    private fun unapplyCommodityProduction() {
        for (industry in market.industries) {
            industry.getSupply(Commodities.FOOD).quantity.unmodify(modId)
            industry.getSupply(Commodities.LUXURY_GOODS).quantity.unmodify(modId)
        }
    }

    private fun getHazardIncrease(): Float {
        val market = getMarket() ?: return 0f

        var hazard = HAZARD_INCREASE_UNSUPPRESSED

        for (industry in market.industries) {
            var effectMult = 1f
            effectMult -= getHazardMultDecrementForIndustry(industry)
            hazard *= effectMult
        }

        return hazard.coerceAtLeast(0f)
    }

    private fun getHazardMultDecrementForIndustry(industry: Industry): Float {
        var decrement = 0f
        if (!industry.isFunctional) return decrement

        for (tag in industry.spec.tags) {
            val result = INDUSTRY_TO_HAZARD_MULT[tag]
            if (result != null) {
                decrement += result
                break // improperly tagged buildings should not get huge reductions
            }
        }

        val deficit = industry.getMaxDeficit(*(industry.allDemand.map { it.commodityId }.toTypedArray()))
        if (decrement > 0 && deficit != null && deficit.one != null) {
            val decrementMult = 1 - (deficit.two / industry.getDemand(deficit.one).quantity.modifiedValue)
            decrement *= decrementMult
        }

        return decrement
    }

    private fun getFoodOutputForIndustry(industry: Industry): Int {
        if (!industry.isFunctional) return 0
        var amount = 0

        for (tag in industry.spec.tags) {
            val result = INDUSTRY_TO_BASE_FOOD_PROD[tag]
            if (result != null) {
                amount = result
                break
            }
        }

        val anchorPoint = 3
        val sizeBonus = (market.size - anchorPoint).coerceAtMost(0)

        return (amount + sizeBonus).coerceAtLeast(0)
    }

    private fun getLuxuryGoodsForIndustry(industry: Industry): Int {
        if (!industry.isFunctional) return 0
        var amount = 0

        for (tag in industry.spec.tags) {
            val result = INDUSTRY_TO_BASE_LUXURY_PROD[tag]
            if (result != null) {
                amount = result
                break
            }
        }

        val anchorPoint = 5
        val sizeBonus = (market.size - anchorPoint).coerceAtMost(0)

        return (amount + sizeBonus).coerceAtLeast(0)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        tooltip.addPara(
            "%s hazard rating (reduced by military bases, scaling with upgrade level, dependant on deficits, currently at %s)",
            10f,
            Misc.getHighlightColor(),
            "+${toPercent(HAZARD_INCREASE_UNSUPPRESSED)}", toPercent(getHazardIncrease())
        )

        tooltip.addPara(
            "%s %s and %s production to military bases, increasing with upgrade level (based on population size)",
            10f,
            Misc.getHighlightColor(),
            "Bonus", "food", "luxury goods"
        )
    }
}
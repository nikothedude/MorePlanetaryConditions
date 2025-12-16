package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_marketUtils.applyDeficitToProductionStatic
import data.utilities.niko_MPC_settings
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
        val AOTD_DEFENSE_FORCE_FOOD_PROD = 4
        val AOTD_DEFENSE_FORCE_LUXURY_PROD = 3
        val AOTD_DEFENSE_FORCE_HAZARD_MULT = 0.9f
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

        /*if (decrement <= 0 && industry.id == "militarygarrison") { // aotd
            decrement += AOTD_DEFENSE_FORCE_HAZARD_MULT
        }*/

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
        /*if (amount <= 0 && industry.id == "militarygarrison") { // aotd
            amount += AOTD_DEFENSE_FORCE_FOOD_PROD
        }*/

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
        /*if (amount <= 0 && industry.id == "militarygarrison") { // aotd
            amount += AOTD_DEFENSE_FORCE_LUXURY_PROD
        }*/

        val anchorPoint = 5
        val sizeBonus = (market.size - anchorPoint).coerceAtMost(0)

        return (amount + sizeBonus).coerceAtLeast(0)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        if (Global.CODEX_TOOLTIP_MODE) {
            tooltip.addPara(
                "Toteliacs have a complex history. As far as the record goes, it's said Toteliacs were originally invented as an \"organic\" solution to clearing unwanted fauna/flora out of terraforming candidates. Unfortunately, while ecologically \"friendly\", they were outclassed by more traditional methods, and so the Toteliacs were sold off as to not waste credits. They served a variety of roles, from show animals, to guard beasts, to even exotic pets (much to the chargin of hospitals everywhere). Their most common role, however, was to be as livestock in highly controlled environments, as it was said that Toteliacs had the taste of perfectly seasoned steak, and their pelts made the best coats.\n" +
                        "\n" +
                "Unfortunately, the collapse brought an end to all this, and in the chaos, Toteliacs seem to have escaped the hundreds of safety nets the Domain had set in place to prevent ecological runaway from these creatures. They are now commonly considered to be highly dangerous pests - though their flesh and pelts are still highly desired.",

                10f
            )
        }

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
        if (niko_MPC_settings.AOTD_vaultsEnabled) {
            /*tooltip.addPara(
                "A %s is particularly effective (%s)",
                10f,
                Misc.getHighlightColor(),
                "standing army", "planetary defense force"
            )*/
        }
    }
}
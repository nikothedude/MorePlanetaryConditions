package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries

class niko_MPC_carnivorousFauna: niko_MPC_baseNikoCondition() {

    companion object {
        const val HAZARD_INCREASE_UNSUPPRESSED = 0.50f
        const val BASE_FOOD_PRODUCTION = 2 // assuming a size 3 colony

        val INDUSTRY_TO_HAZARD_REDUCTION = hashMapOf(
            Pair(Industries.TAG_MILITARY, 0.2f),
            Pair(Industries.TAG_COMMAND, 0.4f)
        )
    }

    override fun apply(id: String) {
        super.apply(id)

        val market = getMarket() ?: return
        market.hazard.modifyFlat(id, getHazardIncrease(), name)
    }

    private fun getHazardIncrease(): Float {
        val market = getMarket() ?: return 0f
        return 0f
        /*for (industry in market.industries) {
            if (industry.isDisrupted) continue
            val marineDeficit = industry.getMaxDeficit()
            val deficit = industry.allDeficit
            val totalDeficit = (deficit.)
            if (industry.defi)
        }*/
    }
}
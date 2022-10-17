package data.scripts.campaign.econ

import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin

abstract class niko_MPC_industryAddingCondition : BaseMarketConditionPlugin() {

    val industryIds = ArrayList<String>()

    override fun apply(id: String) {
        super.apply(id)
        if (wantToApplyAnyIndustry()) {
            for (industryId: String in industryIds) {
                val trueIndustryId: String? = getModifiedIndustryId(industryId);
                if (trueIndustryId != null) {
                    if (wantToApplyIndustry(trueIndustryId)) {
                        applyIndustry(trueIndustryId)
                    }
                }
            }
        }
    }

    override fun unapply(id: String) {
        super.unapply(id)
        for (industryId: String in industryIds) {
            val trueIndustryId: String? = getModifiedIndustryId(industryId);
            if (trueIndustryId != null) {
                if (wantToUnapplyIndustry(trueIndustryId)) {
                    unapplyIndustry(trueIndustryId)
                }
            }
        }
    }

    protected fun wantToApplyAnyIndustry() : Boolean {
        /*for (industryId: String in industryIds) {
            if (market.hasIndustry(industryId)) {
                return false
            }
        }*/
        return true
    }


    protected fun getModifiedIndustryId(industryId: String): String? {
        return industryId
    }

    protected fun wantToApplyIndustry(industryId : String) = !market.hasIndustry(industryId)

    protected fun wantToUnapplyIndustry(industryId: String): Boolean {
        var result = true
        for (condition: MarketConditionAPI in market.conditions) {
            if (condition is niko_MPC_industryAddingCondition) {
                if ((condition != this) && condition.industryIds.any { it == industryId }) {
                    result = false
                    break
                }
            }
        }
        return result
    }
    fun applyIndustry(industryId: String) = market.addIndustry(industryId)
    fun unapplyIndustry(industryId: String) = market.removeIndustry(industryId, null, false)
}

package data.scripts.campaign.econ

import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin

abstract class niko_MPC_industryAddingCondition : BaseMarketConditionPlugin() {

    val industryIds = ArrayList<String>()

    override fun apply(id: String) {
        super.apply(id)
        if (wantToApplyAnyIndustry()) {
            for (industryId: String in industryIds) {
                val trueIndustryId: String? = getModifiedIndustryIdToAdd(industryId);
                if (trueIndustryId != null) {
                    if (wantToApplyIndustry(trueIndustryId)) {
                        applyIndustry(trueIndustryId)
                    }
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

    protected fun getModifiedIndustryIdToAdd(industryId: String): String? {
        return industryId
    }

    protected fun wantToApplyIndustry(industryId : String) = !market.hasIndustry(industryId)
    protected fun applyIndustry(industryId: String) = market.addIndustry(industryId)
}
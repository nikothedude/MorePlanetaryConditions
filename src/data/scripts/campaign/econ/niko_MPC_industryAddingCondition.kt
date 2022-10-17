package data.scripts.campaign.econ

import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin

abstract class niko_MPC_industryAddingCondition : BaseMarketConditionPlugin() {

    val industryIds = ArrayList<String>()

    override fun apply(id: String) {
        super.apply(id)
        if (wantToApplyAnyIndustry(market)) {
            tryToApplyIndustries(market)
        }
    }

    override fun unapply(id: String) {
        super.unapply(id)
        tryToUnapplyIndustries(market)
    }
    
    protected fun tryToApplyIndustries(ourMarket: MarketAPI = market) {
        for (industryId: String in industryIds) {
            tryToApplyIndustry(ourMarket, industryId)
        }
    }
    
    protected fun tryToApplyIndustry(ourMarket: MarketAPI = market, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (trueIndustryId != null) {
            if (wantToApplyIndustry(ourMarket, trueIndustryId)) {
                applyIndustry(ourMarket, trueIndustryId)
            }
        }
    }
    
    protected fun tryToUnapplyIndustries(ourMarket: MarketAPI = market) {
        for (industryId: String in industryIds) {
            tryToUnapplyIndustry(ourMarket, industryId)
        }
    }
    
    protected fun tryToUnapplyIndustry(ourMarket: MarketAPI = market, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (trueIndustryId != null) {
            if (wantToUnapplyIndustry(ourMarket, trueIndustryId)) {
                unapplyIndustry(ourMarket, trueIndustryId)
            }
        }
    }

    protected fun wantToApplyAnyIndustry(ourMarket: MarketAPI = market) : Boolean {
        /*for (industryId: String in industryIds) {
            if (ourMarket.hasIndustry(industryId)) {
                return false
            }
        }*/
        return true
    }


    protected fun getModifiedIndustryId(ourMarket: MarketAPI = market, industryId: String): String? {
        return industryId
    }

    protected fun wantToApplyIndustry(ourMarket: MarketAPI = market, industryId : String) = !ourMarket.hasIndustry(industryId)

    protected fun wantToUnapplyIndustry(ourMarket: MarketAPI = market, industryId: String): Boolean {
        var result = true
        for (condition: MarketConditionAPI in ourMarket.conditions) { //todo: convert this to memory at some point, i dont trust conditions
            if (condition is niko_MPC_industryAddingCondition) {
                if ((condition !== this) && condition.industryIds.any { it == industryId }) {
                    result = false
                    break
                }
            }
        }
        return result
    }
    fun applyIndustry(ourMarket: MarketAPI = market, industryId: String) = ourMarket.addIndustry(industryId)
    fun unapplyIndustry(ourMarket: MarketAPI = market, industryId: String) = ourMarket.removeIndustry(industryId, null, false)
    
    override fun isTransient(): Boolean = return false //todo: is this a good idea
}

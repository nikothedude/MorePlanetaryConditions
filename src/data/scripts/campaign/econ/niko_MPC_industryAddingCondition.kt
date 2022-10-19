package data.scripts.campaign.econ

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

abstract class niko_MPC_industryAddingCondition: niko_MPC_baseNikoCondition() {

    val industryIds = ArrayList<String>()

    var cachedMarket: MarketAPI? = getMarket()

    override fun apply(id: String) {
        super.apply(id)
        val ourMarket = getMarket() ?: return
        if (wantToApplyAnyIndustry(ourMarket)) {
            tryToApplyIndustries(ourMarket)
        }
        checkForMarketDesync()
    }

    protected fun checkForMarketDesync() {
        val ourMarket = getMarket() ?: return
        if (ourMarket !== cachedMarket) handleMarketDesync(ourMarket)
    }

    protected fun handleMarketDesync(ourMarket: MarketAPI) {
        if (ourMarket === cachedMarket) {
            niko_MPC_debugUtils.displayError("desync check error: $market, ${market.name} is the same as the provided cached market")
            return
        }
        if (cachedMarket != null) {
            if (cachedMarket!!.hasSpecificCondition(condition.idForPluginModifications)) {
                niko_MPC_debugUtils.displayError("SOMEHOW $cachedMarket HAS $this STILL APPLIED DESPITE NOT BEING $market THIS IS FUCKED", highPriority = true)
                return
            }
            handleMarketDesyncEffect()
        }
        cachedMarket = market
    }

    protected open fun handleMarketDesyncEffect() {
        cachedMarket?.let { tryToUnapplyIndustries(it) }
    }

    override fun unapply(id: String) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        tryToUnapplyIndustries(ourMarket)
    }
    
    protected fun tryToApplyIndustries(ourMarket: MarketAPI) {
        for (industryId: String in industryIds) {
            tryToApplyIndustry(ourMarket, industryId)
        }
    }
    
    protected fun tryToApplyIndustry(ourMarket: MarketAPI, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (trueIndustryId != null) {
            if (wantToApplyIndustry(ourMarket, trueIndustryId)) {
                applyIndustry(ourMarket, trueIndustryId)
            }
        }
    }
    
    protected fun tryToUnapplyIndustries(ourMarket: MarketAPI) {
        for (industryId: String in industryIds) {
            tryToUnapplyIndustry(ourMarket, industryId)
        }
    }
    
    protected fun tryToUnapplyIndustry(ourMarket: MarketAPI, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (trueIndustryId != null) {
            if (wantToUnapplyIndustry(ourMarket, trueIndustryId)) {
                unapplyIndustry(ourMarket, trueIndustryId)
            }
        }
    }

    protected fun wantToApplyAnyIndustry(ourMarket: MarketAPI) : Boolean {
        /*for (industryId: String in industryIds) {
            if (ourMarket.hasIndustry(industryId)) {
                return false
            }
        }*/
        return true
    }


    protected fun getModifiedIndustryId(ourMarket: MarketAPI, industryId: String): String? {
        return industryId
    }

    protected fun wantToApplyIndustry(ourMarket: MarketAPI, industryId : String) = !ourMarket.hasIndustry(industryId)

    protected fun wantToUnapplyIndustry(ourMarket: MarketAPI, industryId: String): Boolean {
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
    fun applyIndustry(ourMarket: MarketAPI, industryId: String) = ourMarket.addIndustry(industryId)
    fun unapplyIndustry(ourMarket: MarketAPI, industryId: String) = ourMarket.removeIndustry(industryId, null, false)
    
    override fun isTransient(): Boolean = false //todo: is this a good idea
}

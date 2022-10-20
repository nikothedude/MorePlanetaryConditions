package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import data.utilities.niko_MPC_debugUtils
/** Base class for any industry that has industries/structures bound to it's existence. While the removal of the
 * condition should remove the structures (usually), removal of the industries does not need to remove the condition. */
abstract class niko_MPC_industryAddingCondition: niko_MPC_baseNikoCondition() {

    /** The ids, in string, of industries that we will potentially add to [market].
     * When creating a [tryToApplyIndustry] or [tryToUnapplyIndustry] override, never
     * use the string raw, instead, use the value of [getModifiedIndustryId].(id).*/
    val industryIds = ArrayList<String>()

    /** A stored value of our [market], that is only updated when [handleMarketDesync] is ran. Used for checking if our
     * [market] has changed.*/
    var cachedMarket: MarketAPI? = getMarket()

    override fun apply(id: String) {
        super.apply(id)
        val ourMarket = getMarket() ?: return
        if (wantToApplyAnyIndustry(ourMarket, true)) {
            tryToApplyIndustries(ourMarket)
        }
        checkForMarketDesync()
    }

    /** Should return true and call [handleMarketDesync] if [ourMarket] is not the same as [cachedMarket],
     * which is true in a scenario where market is set to something different. False and do nothing otherwise.*/
    protected open fun checkForMarketDesync(): Boolean {
        val ourMarket = getMarket()
        if (ourMarket !== cachedMarket) {
            handleMarketDesync(ourMarket)
            return true
            // fun fact, even if our market is null, we still want to handle a desync, because this is 100% an operation
            // that only affects the cached market
        }
        return false
    }

    /** Assumes [ourMarket] is not the same as [cachedMarket], but if it is, displays an error and returns.
     * Should call [handleMarketDesyncEffect] if [cachedMarket] is not null and the aforementioned error state isnt met.
     * Should then set [cachedMarket] to [market], to sync the market.*/
    protected open fun handleMarketDesync(ourMarket: MarketAPI?) {
        if (ourMarket === cachedMarket) {
            niko_MPC_debugUtils.displayError("desync check error: $market, ${market.name} is the same as the provided cached market")
            return
        }
        if (cachedMarket != null) {
            if (cachedMarket!!.hasSpecificCondition(condition.idForPluginModifications)) {
                niko_MPC_debugUtils.displayError("SOMEHOW $cachedMarket, ${cachedMarket?.name} HAS $this STILL APPLIED DESPITE NOT BEING $market THIS IS FUCKED", highPriority = true)
                return
            }
            handleMarketDesyncEffect()
        }
        cachedMarket = market // we've done our work here, we dont need to do it again until it changes
    }

    /** DO NOT CALL DIRECTLY. Let [handleMarketDesync] do it outside of exceptional circumstances.
     * This is what will be called when a cached market is desynced from the current market (e.g. this condition was
     * somehow moved). Calling super is pretty important. */
    protected open fun handleMarketDesyncEffect() {
        cachedMarket?.let { tryToUnapplyIndustries(it) }
    }

    override fun unapply(id: String) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        if (wantToUnapplyAnyIndustry(ourMarket, true)) {
            tryToUnapplyIndustries(ourMarket)
        }
    }

    /** A simple method; runs [tryToApplyIndustry] for each item in [industryIds].*/
    private fun tryToApplyIndustries(ourMarket: MarketAPI) {
        for (industryId: String in industryIds) {
            tryToApplyIndustry(ourMarket, industryId)
        }
    }

    /** Never use [industryId] raw, put it into [getModifiedIndustryId] first. Should also use
     * [wantToApplyIndustry].(trueId) in an if check, running [applyIndustry].(trueId) if it returns true.*/
    protected open fun tryToApplyIndustry(ourMarket: MarketAPI, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (trueIndustryId != null) {
            if (wantToApplyIndustry(ourMarket, trueIndustryId)) {
                applyIndustry(ourMarket, trueIndustryId)
            }
        }
    }

    /** A simple method; runs [tryToUnapplyIndustry] for each item in [industryIds].*/
    protected open fun tryToUnapplyIndustries(ourMarket: MarketAPI) {
        for (industryId: String in industryIds) {
            tryToUnapplyIndustry(ourMarket, industryId)
        }
    }

    /** Never use [industryId] raw, put it into [getModifiedIndustryId] first. Should also use
     * [wantToUnapplyIndustry].(trueId) in an if check, running [unapplyIndustry].(trueId) if it returns true.*/
    protected open fun tryToUnapplyIndustry(ourMarket: MarketAPI, industryId: String) {
        val trueIndustryId: String? = getModifiedIndustryId(ourMarket, industryId);
        if (wantToUnapplyIndustry(ourMarket, trueIndustryId)) {
            if (trueIndustryId != null) { //redundant nullcheck so intellij doesnt moan at me
                unapplyIndustry(ourMarket, trueIndustryId)
            }
        }
    }

    protected open fun wantToApplyAnyIndustry(ourMarket: MarketAPI, calledFromApply: Boolean = false) : Boolean {
        return true
    }

    protected open fun wantToUnapplyAnyIndustry(ourMarket: MarketAPI, calledFromUnapply: Boolean = false): Boolean {
        return true
    }

    protected open fun getModifiedIndustryId(ourMarket: MarketAPI, originalId: String): String? {
        return originalId
    }

    protected open fun wantToApplyIndustry(ourMarket: MarketAPI, industryId : String?): Boolean {
        return (industryId != null && !ourMarket.hasIndustry(industryId))
    }

    protected open fun wantToUnapplyIndustry(ourMarket: MarketAPI, industryId: String?): Boolean {
        if (industryId == null) return false
        var result = true
        for (condition: MarketConditionAPI in ourMarket.conditions) {
            if (condition is niko_MPC_industryAddingCondition) {
                if ((condition !== this) && condition.industryIds.any { it == industryId }) {
                    result = false
                    break
                }
            }
        }
        return result
    }
    open fun applyIndustry(ourMarket: MarketAPI, industryId: String) = ourMarket.addIndustry(industryId)
    open fun unapplyIndustry(ourMarket: MarketAPI, industryId: String) = ourMarket.removeIndustry(industryId, null, false)

    override fun isTransient(): Boolean = false
}


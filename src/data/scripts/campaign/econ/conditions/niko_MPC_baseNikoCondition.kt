package data.scripts.campaign.econ.conditions

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_settings

/** A base class that holds extremely basic and generic methods I will apply to all conditions I make.*/
abstract class niko_MPC_baseNikoCondition: BaseMarketConditionPlugin() {

    var deletionScript: niko_MPC_conditionRemovalScript? = null

    /** Use [niko_MPC_settings] variables to determine this.*/
    protected open val isEnabled: Boolean = true
    val suppressedConditions = ArrayList<String>()

    override fun apply(id: String) {
        super.apply(id)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!doEnabledCheck()) return
    }

    protected inline fun doEnabledCheck(): Boolean {
        if (!isEnabled) {
            market.removeSpecificCondition(condition.idForPluginModifications)
            return false
        }
        return true
    }

    /** Wrapper for [market]. If [doNullCheck] is true, which it is by default, logs an error if [market] is null.
     * Returns a nullable instance of MarketAPI corresponding to our [market] variable. */
     fun getMarket(doNullCheck: Boolean = true): MarketAPI? {
        if (doNullCheck && market == null) {
            handleNullMarket()
        }
        return market
    }

    /** Just in case of insanity, we should have some error handling for a null market.*/
    protected open fun handleNullMarket() {
        niko_MPC_debugUtils.displayError("Something has gone terribly wrong and market was $market in $this.")
    }

    fun getCondition(): MarketConditionAPI {
        return condition
    }

    open fun delete() {
        val ourMarket = getMarket() ?: return

        ourMarket.removeSpecificCondition(condition.idForPluginModifications)
    }
}

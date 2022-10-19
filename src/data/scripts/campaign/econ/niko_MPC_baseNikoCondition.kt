package data.scripts.campaign.econ

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.BaseMarketConditionPlugin
import data.utilities.niko_MPC_debugUtils

abstract class niko_MPC_baseNikoCondition: BaseMarketConditionPlugin() {

    protected fun getMarket(doNullCheck: Boolean = true): MarketAPI? {
        if (doNullCheck && market == null) {
            handleNullMarket()
        }
        return market
    }

    /** Just in case of insanity, we should have some error handling for a null market.*/
    protected fun handleNullMarket() {
        niko_MPC_debugUtils.displayError("Something has gone terribly wrong and market was $market in $this.")
    }
}

package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getOvergrownJunkHandler
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler

class overgrownNanoforgeJunk: baseOvergrownNanoforgeStructure() {

    override fun init(id: String?, market: MarketAPI?) {
        super.init(id, market)
        if (id == null || market == null) return

        val nanoforge = market.getOvergrownNanoforge()
        val masterHandler = market.getOvergrownNanoforgeIndustryHandler()
        if (nanoforge == null || masterHandler == null) {
            //displayError("$this instantiated on market with no nanoforge")
            //logDataOf(market)
            market.removeIndustry(id, null, false)
            return
        }
        val handler = instantiateNewHandler()
    }

    override fun createNewHandlerInstance(): overgrownNanoforgeJunkHandler {
        return overgrownNanoforgeJunkHandler(market, market.getOvergrownNanoforgeIndustryHandler()!!, getDesignation())
    }

    override fun getHandler(): overgrownNanoforgeJunkHandler? {
        return market.getOvergrownJunkHandler(getDesignation())
    }

    fun getDesignation(): Int {
        return (id.filter { it.isDigit() }.toInt())
    }

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean {
        return false
    }
}

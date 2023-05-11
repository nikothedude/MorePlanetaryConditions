package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge

class overgrownNanoforgeJunk: baseOvergrownNanoforgeStructure() {

    lateinit var ourHandler: overgrownNanoforgeJunkHandler

    override fun init(id: String?, market: MarketAPI?) {
        super.init(id, market)
        if (id == null || market == null) return

        val nanoforge = market.getOvergrownNanoforge()
        if (nanoforge == null) {
            displayError("$this instantiated on market with no nanoforge")
            logDataOf(market)
            market.removeIndustry(id, null, false)
            return
        }
    }

    override fun createNewHandlerInstance(): overgrownNanoforgeJunkHandler {
        TODO("Not yet implemented")
    }

    override fun getHandler(): overgrownNanoforgeJunkHandler? {
        TODO("Not yet implemented")
    }

    override fun isAvailableToBuild(): Boolean {
        return false
    }

    override fun showWhenUnavailable(): Boolean {
        return false
    }
}

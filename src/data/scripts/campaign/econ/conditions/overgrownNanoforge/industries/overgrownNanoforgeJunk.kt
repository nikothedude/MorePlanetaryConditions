package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getOvergrownJunkHandler
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_JUNK_NAME

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
    }

    override fun createNewHandlerInstance(): overgrownNanoforgeJunkHandler {
        return overgrownNanoforgeJunkHandler(market, market.getOvergrownNanoforgeIndustryHandler()!!, getDesignation())
    }

    override fun getHandler(): overgrownNanoforgeJunkHandler? {
        return market.getOvergrownJunkHandler(id)
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

    override fun getCurrentName(): String {
        return OVERGROWN_NANOFORGE_JUNK_NAME
    }

    override fun canInstallAICores(): Boolean {
        return false
    }

    override fun isIndustry(): Boolean {
        val handler = getHandler() ?: return false
        return handler.isIndustry()
    }
}

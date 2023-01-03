package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeRandomizedSource
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import niko.MCTE.utils.MCTE_debugUtils.displayError

class overgrownNanoforgeJunk: baseOvergrownNanoforgeStructure() {

    var source: overgrownNanoforgeRandomizedSource? = null

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
        nanoforge.junk += this

        generateData(id)
    }

    override fun canBeDestroyed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun apply() {
        TODO("Not yet implemented")
    }

}

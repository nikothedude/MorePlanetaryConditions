package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.sun.org.apache.xpath.internal.operations.Bool
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeRandomizedSource
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import niko.MCTE.utils.MCTE_debugUtils.displayError

class overgrownNanoforgeJunk: baseOvergrownNanoforgeStructure() {

    lateinit var source: overgrownNanoforgeRandomizedSource
    var properlyAdded: Boolean = false

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
    }

    override fun canBeDestroyed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun apply() {
        if (!properlyAdded) {
            displayError("$this was improperly created")
            delete()
            return
        }
        source.apply()
        TODO("Not yet implemented")
    }
}

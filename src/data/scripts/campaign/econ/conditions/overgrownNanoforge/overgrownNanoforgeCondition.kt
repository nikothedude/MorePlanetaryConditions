package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.everyFrames.niko_MPC_overgrownNanoforgeRemovalScript
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.purgeOvergrownNanoforgeBuildings

class overgrownNanoforgeCondition : niko_MPC_baseNikoCondition() {

    override fun isTransient(): Boolean {
        return false
    }

    override fun init(market: MarketAPI?, condition: MarketConditionAPI?) {
        super.init(market, condition)
        val ourMarket = getMarket() ?: return
        ourMarket.addIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId)
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        createDeletionScript()
    }

    fun createDeletionScript() {
        if (deletionScript == null) {
            val script = createDeletionScriptInstance() ?: return
            deletionScript = script
            script.start()
        }
    }

    fun createDeletionScriptInstance(): niko_MPC_overgrownNanoforgeRemovalScript? {
        val ourMarket = getMarket() ?: return null
        return niko_MPC_overgrownNanoforgeRemovalScript(ourMarket.primaryEntity, getCondition().id, this)
    }

    override fun delete() {
        super.delete()
        val ourMarket = getMarket() ?: return
        ourMarket.purgeOvergrownNanoforgeBuildings()
        TODO()
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        TODO()
    }

}
package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.purgeOvergrownNanoforgeBuildings

class overgrownNanoforgeCondition : niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_overgrownNanoforgeRemovalScript?> {

    override var deletionScript: niko_MPC_overgrownNanoforgeRemovalScript? = null
    override fun isTransient(): Boolean {
        return false
    }

    override fun apply(id: String) {
        super.apply(id)

        val ourMarket = getMarket() ?: return
        val ourEntity: SectorEntityToken? = ourMarket.primaryEntity

        val ourIndustry = ourMarket.getOvergrownNanoforge()

        if (shouldHaveIndustry(ourMarket, ourIndustry)) {
            ourMarket.addIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId)
        }
    }

    open fun shouldHaveIndustry(ourMarket: MarketAPI, ourIndustry: overgrownNanoforgeIndustry? = ourMarket.getOvergrownNanoforge()): Boolean {
        return (!ourMarket.isPlanetConditionMarketOnly && ourIndustry == null && ourMarket.id != "fake_Colonize" &&
                ourMarket.isInEconomy)
    }

    override fun unapply(id: String?) {
        super.unapply(id)
        val ourMarket = getMarket() ?: return
        startDeletionScript(ourMarket)
    }

    override fun createDeletionScriptInstance(vararg args: Any): niko_MPC_overgrownNanoforgeRemovalScript {
        val ourMarket = args[0] as MarketAPI
        return niko_MPC_overgrownNanoforgeRemovalScript(ourMarket.primaryEntity, getCondition().id, this, this)
    }

    override fun delete() {
        super.delete()
        val ourMarket = getMarket() ?: return
        ourMarket.purgeOvergrownNanoforgeBuildings()
        //TODO()
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        //TODO()
    }
}
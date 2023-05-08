package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.everyFrames.deletionScript
import data.scripts.everyFrames.niko_MPC_conditionRemovalScript
import data.utilities.niko_MPC_industryIds

class niko_MPC_overgrownNanoforgeRemovalScript(
    entity: SectorEntityToken, conditionId: String, override val condition: overgrownNanoforgeCondition? = null,
    hasDeletionScript: hasDeletionScript<out deletionScript?>,
):
    niko_MPC_conditionRemovalScript(entity, conditionId, condition, hasDeletionScript) {

    override fun shouldDeleteWithMarket(market: MarketAPI?): Boolean {
        val superResult = super.shouldDeleteWithMarket(market)
        val isSupposedToHaveIndustry = ((market == null) || condition?.shouldHaveIndustry(market) == true)
        return (superResult || (isSupposedToHaveIndustry && market?.hasIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId) == false))
    }
}
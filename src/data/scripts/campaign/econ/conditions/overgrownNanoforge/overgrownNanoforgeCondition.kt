package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.thoughtworks.xstream.mapper.Mapper.Null
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_marketUtils.isValidTargetForOvergrownHandler
import data.utilities.niko_MPC_marketUtils.purgeOvergrownNanoforgeBuildings
import data.utilities.niko_MPC_marketUtils.shouldHaveOvergrownNanoforgeIndustry
import org.lwjgl.util.vector.Vector2f
import java.lang.NullPointerException

class overgrownNanoforgeCondition : niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_overgrownNanoforgeRemovalScript?> {

    override var deletionScript: niko_MPC_overgrownNanoforgeRemovalScript? = null
    override fun isTransient(): Boolean {
        return false
    }

    override fun apply(id: String) {
        super.apply(id)

        val ourMarket = getMarket() ?: return
        if (ourMarket.isDeserializing()) return
        applyConditions()

        if (ourMarket.shouldHaveOvergrownNanoforgeIndustry()) {
            ourMarket.addIndustry(niko_MPC_industryIds.overgrownNanoforgeIndustryId)
        }
        updateHandlerValues()
    }

    private fun applyConditions() { // learned it the hard way, you can add multiple versions of the same condition in a infinite loop :)
        if (market.hasCondition(Conditions.HABITABLE) && !market.hasCondition(Conditions.POLLUTION)) {
            market.addCondition(Conditions.POLLUTION)
        }
    }

    private fun updateHandlerValues() {

        val handler = getHandlerWithUpdate()

        if (handler?.market?.isDeserializing() != false) return

        handler.market = this.market
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

        getHandler()?.delete()
        //TODO()
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        //TODO()
    }

    fun getHandler(): overgrownNanoforgeIndustryHandler? {
        return market.getOvergrownNanoforgeIndustryHandler()
    }

    fun getHandlerWithUpdate(): overgrownNanoforgeIndustryHandler? {
        updateHandler()
        return getHandler()
    }

    fun updateHandler() {
        if (getHandler() == null && market.isValidTargetForOvergrownHandler()) instantiateNewHandler()
    }

    fun instantiateNewHandler(): overgrownNanoforgeIndustryHandler {
        val newHandler = createNewHandlerInstance()
        newHandler.init()
        return newHandler
    }

    private fun createNewHandlerInstance(): overgrownNanoforgeIndustryHandler {
        return overgrownNanoforgeIndustryHandler(market)
    }
}

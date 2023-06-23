package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.mapper.Mapper.Null
import data.scripts.campaign.econ.conditions.hasDeletionScript
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.getOvergrownJunkHandlers
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_marketUtils.isValidTargetForOvergrownHandler
import data.utilities.niko_MPC_marketUtils.purgeOvergrownNanoforgeBuildings
import data.utilities.niko_MPC_marketUtils.setOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.shouldHaveOvergrownNanoforgeIndustry
import org.apache.log4j.Level
import org.lwjgl.util.vector.Vector2f
import java.lang.NullPointerException

class overgrownNanoforgeCondition : niko_MPC_baseNikoCondition(), hasDeletionScript<niko_MPC_overgrownNanoforgeRemovalScript?> {

    override var deletionScript: niko_MPC_overgrownNanoforgeRemovalScript? = null
    override fun isTransient(): Boolean = false

    override fun apply(id: String) {
        super.apply(id)

        if (market.primaryEntity?.market != market) {
            return //TODO: remove
        }

        val ourMarket = getMarket() ?: return
        if (ourMarket.isDeserializing()) return

        applyConditions()

        updateHandlerValues()
        getHandler()?.updateStructure()
    }

    private fun applyConditions() { // learned it the hard way, you can add multiple versions of the same condition in a infinite loop :)
        if (market.hasCondition(Conditions.HABITABLE) && !market.hasCondition(Conditions.POLLUTION)) {
            market.addCondition(Conditions.POLLUTION)
        }
    }

    private fun updateHandlerValues() {

        val handler = getHandlerWithUpdate()
        if (handler?.isCorrupted() == true) {
            replaceHandler(handler)
            return
        }
        if (handler == null) {
            displayError("handler null on condition updatehandlervalues HOW.")
            return
        }
        if (handler.market.isDeserializing()) return

        if (market.getOvergrownNanoforgeIndustryHandler() == null) {
            Global.getSector().campaignUI.addMessage("it happened")
        }

        val ourMarket = market
        if (ourMarket != null) {
            handler.market = this.market
        }
    }

    private fun replaceHandler(originalHandler: overgrownNanoforgeIndustryHandler) { //assumes handler is corrupted
        displayError("overgrown nanoforge corruption detected, replacing handler so the game doesnt crash, stability cannot be guaranteed")
        for (junkHandler in market.getOvergrownJunkHandlers()) {
            if (junkHandler.masterHandler == null || junkHandler.masterHandler == originalHandler) {
                try {
                    junkHandler.baseSource.delete()
                    junkHandler.delete()
                }
                catch (ex: NullPointerException) {
                    niko_MPC_debugUtils.log.log(Level.ERROR, ex)
                }
            }
        }
        try {
            market.setOvergrownNanoforgeIndustryHandler(null)
            originalHandler.manipulationIntel?.brain?.delete()
            originalHandler.manipulationIntel?.delete()
            originalHandler.intelBrain?.delete()
            originalHandler.baseSource.delete()
            originalHandler.delete()
        }
        catch (ex: NullPointerException) {
            niko_MPC_debugUtils.log.log(Level.ERROR, ex)
        }
        updateHandler()
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
 //       ourMarket.purgeOvergrownNanoforgeBuildings()
        // disabling experimentally to see if this will fix a commoddification error

        getHandler()?.delete()
        //TODO()
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)
        if (tooltip == null) return

        val handler = getHandlerWithUpdate() ?: return
        val growthNum = handler.junkHandlers.size
        val s = if (growthNum > 1) "s" else ""
        var spreadingOrNot = if (handler.isSpreading()) ", one of which is currently spreading" else ""
        tooltip.addPara("${market.name} currently has %s growth$s$spreadingOrNot.", 5f, Misc.getHighlightColor(), "$growthNum")
        //"...currently has x growth(s)[, one of which is currently spreading].

        tooltip.addPara("Further details, such as purpose of the growths, or the traits of the nanoforge, are not determinable " +
                "without establishing a long-term prescense such as a colony.", 5f)
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
        if (getHandler() == null && market.isValidTargetForOvergrownHandler()) {
            instantiateNewHandler()
        }
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

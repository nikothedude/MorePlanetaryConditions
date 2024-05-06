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
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeJunkHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids.overgrownNanoforgeJunkHandlerMemoryId
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_marketUtils.convertToMemKey
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
import java.lang.Exception
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
        /*if (handler != null && Global.getSector().memoryWithoutUpdate.get("\$awergh") == null) {
            replaceHandler(handler)
            Global.getSector().memoryWithoutUpdate.set("\$awergh", false)
            return
        }*/
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
        when (market.surveyLevel) {
            MarketAPI.SurveyLevel.FULL -> {handler.discovered = true}
            else -> {handler.discovered = false}
        }

        for (junkHandler: overgrownNanoforgeJunkHandler in market.getOvergrownJunkHandlers())
            if (junkHandler.deleted) {
                market.memoryWithoutUpdate.unset(convertToMemKey(junkHandler.cachedBuildingId!!))
                continue
            }
    }

    // triggered in case of what im going to call, the "nanoforge serialization failure"
    // its when, on save load, the nanoforge industry handler mysteriously has half its variables PERMANENTLY nulled
    // i wish i knew why this happened, but in truth, i dont. theyre nonnullable variables, meaning its impossible for ME
    // to null them
    // its completely unreplicatable, so, like. fuck
    private fun replaceHandler(originalHandler: overgrownNanoforgeIndustryHandler) { //assumes handler is corrupted
        displayError("overgrown nanoforge corruption detected, replacing handler so the game doesnt crash, stability cannot be guaranteed")
        try {
            originalHandler.removeStructure()
            market.setOvergrownNanoforgeIndustryHandler(null)

            val growingState = originalHandler.growing ?: false
            instantiateNewHandler(originalHandler.baseSource, originalHandler.cullingResistance, originalHandler.cullingResistanceRegeneration, growingState, false)
            val newHandler = getHandler()!!
            for (effectSource in originalHandler.baseSource.effects) {
                effectSource.handler = newHandler
            }
            originalHandler.baseSource.handler = newHandler

            val cachedJunkHandlers = HashSet(market.getOvergrownJunkHandlers())
            for (junkHandler in market.getOvergrownJunkHandlers()) {
                junkHandler.removeStructure()
                val junkGrowingState = junkHandler.growing ?: false
                val newJunkHandler = overgrownNanoforgeJunkHandler(market, newHandler, junkHandler.getOurDesignation(), junkGrowingState)
                for (effectSource in originalHandler.baseSource.effects) {
                    effectSource.handler = newHandler
                }
                junkHandler.baseSource.handler = newJunkHandler
                newJunkHandler.init(junkHandler.baseSource, junkHandler.cullingResistance, junkHandler.cullingResistanceRegeneration)
                market.memoryWithoutUpdate.set(convertToMemKey(newJunkHandler.cachedBuildingId!!), newJunkHandler)
                newHandler.junkHandlers += newJunkHandler
            }
            for (junkHandler in cachedJunkHandlers) {
                if (junkHandler.masterHandler?.junkHandlers != null) {
                    junkHandler.masterHandler.junkHandlers -= junkHandler
                }
                junkHandler.manipulationIntel?.delete()
            }

            originalHandler.manipulationIntel?.brain?.delete()
            originalHandler.intelBrain?.spreadingIntel?.delete()
            originalHandler.manipulationIntel?.delete()
            originalHandler.intelBrain?.delete()

            val industryHandlerList: HashSet<overgrownNanoforgeIndustryHandler> = Global.getSector().memoryWithoutUpdate.get("\$overgrownNanoforgeHandlerList") as HashSet<overgrownNanoforgeIndustryHandler>
            industryHandlerList -= originalHandler
        } catch (ex: Exception) {
            niko_MPC_debugUtils.log.log(Level.ERROR, ex)
            nukeHandler(originalHandler)
            val newHandler = getHandler()
            if (newHandler != null) nukeHandler(newHandler)
        }
    }

    // the ultimate failsafe in case something goes really wrong
    private fun nukeHandler(originalHandler: overgrownNanoforgeIndustryHandler) {
        niko_MPC_debugUtils.log.error("nukeHandler called. something has REALLY gone wrong")
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
        if (market != null) {
            updateHandler()
        }
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

    fun instantiateNewHandler(initBaseSource: overgrownNanoforgeEffectSource? = null, resistance: Int? = null, resistanceRegen: Int? = null, growing: Boolean = false, generateJunk: Boolean = true): overgrownNanoforgeIndustryHandler {
        val newHandler = createNewHandlerInstance(growing, generateJunk)
        newHandler.init(initBaseSource, resistance, resistanceRegen)
        return newHandler
    }

    private fun createNewHandlerInstance(growing: Boolean = false, generateJunk: Boolean = true): overgrownNanoforgeIndustryHandler {
        return overgrownNanoforgeIndustryHandler(market, growing, generateJunk)
    }
}

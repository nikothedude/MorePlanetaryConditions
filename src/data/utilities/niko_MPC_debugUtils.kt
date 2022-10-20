package data.utilities

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.exceptions.niko_MPC_stackTraceGenerator
import data.utilities.niko_MPC_debugUtils.doLogOf
import data.utilities.niko_MPC_debugUtils.logEntityData
import data.utilities.niko_MPC_debugUtils.memKeyHasIncorrectType
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_satelliteUtils.defenseSatellitesApplied
import data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteMarket
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.lwjgl.Sys
import java.awt.Color

object niko_MPC_debugUtils {
    val log: Logger = Global.getLogger(niko_MPC_debugUtils::class.java)

    init {
        log.level = Level.ALL
    }

    /**
     * Displays the given errorCode in whatever game layer the game is currently in. Also logs it, as well as prints
     * the stack trace in log.
     * @param errorCode The errorCode to display and log.
     * @param highPriority If true, a very loud and visible message will be displayed to the user.
     * @param crash If true, crashes the game.
     * @throws RuntimeException
     */
    @Throws(RuntimeException::class)
    fun displayError(errorCode: String = "Unimplemented errorcode", highPriority: Boolean = false, crash: Boolean = false,
                     logType: (Any, Throwable) -> Unit = log::error) {

        if (niko_MPC_settings.SHOW_ERRORS_IN_GAME) {
            when (val gameState = Global.getCurrentState()) {
                GameState.CAMPAIGN -> displayErrorToCampaign(errorCode, highPriority)
                GameState.COMBAT -> displayErrorToCombat(errorCode, highPriority)
                GameState.TITLE -> displayErrorToTitle(errorCode, highPriority)
                else -> log.warn("Non-standard gamestate value during displayError, gamestate: $gameState")
            }
        }
        logType("Error code:", niko_MPC_stackTraceGenerator(errorCode))
        if (crash) {
            throw RuntimeException(
                "A critical error has occurred in More Planetary Conditions, and for one reason" +
                " or another, the mod author has decided that this error is severe enough to warrant a crash." +
                " Error code: " + errorCode
            )
        }
    }

    private fun displayErrorToCampaign(errorCode: String, highPriority: Boolean) {
        val campaignUI = Global.getSector().campaignUI
        campaignUI.addMessage("###### MORE PLANETARY CONDITIONS ERROR ######")
        campaignUI.addMessage(errorCode)
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f)
            campaignUI.addMessage(
                "The above error has been deemed high priority by the mod author. It is likely" +
                        " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                        + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                Color.RED
            )
        }
        campaignUI.addMessage("Please provide the mod author a copy of your logs. These messages can be disabled in settings.")
    }

    private fun displayErrorToCombat(errorCode: String, highPriority: Boolean) {
        val engine = Global.getCombatEngine()
        val combatUI = engine.combatUI
        combatUI.addMessage(
            1,
            "Please provide the mod author with a copy of your logs.  These messages can be disabled in settings."
        )
        if (highPriority) {
            Global.getSoundPlayer().playUISound("cr_playership_critical", 1f, 1f)
            combatUI.addMessage(
                1, "The above error has been deemed high priority by the mod author. It is likely" +
                        " that it's occurrence will interfere with your gameplay, and/or it is considered to be a \"critical\" error,"
                        + " one that interferes with crucial functionality. You may want to save your game and come to the mod author.",
                Color.RED
            )
        }
        combatUI.addMessage(1, errorCode)
        combatUI.addMessage(1, "###### MORE PLANETARY CONDITIONS ERROR ######")
    }

    private fun displayErrorToTitle(errorCode: String, highPriority: Boolean) {
        return
    }

    /**
     * Returns false if the entity has satellite params, a tracker, or if the entity has a satellite market.
     */
    fun doEntityHasNoSatellitesTest(entity: SectorEntityToken?): Boolean {
        var result = true
        if (entity == null) {
            displayError("doEntityNoSatellitesTest failed because entity was null")
            return false
        }
        val entitySatelliteMarket : MarketAPI? = getEntitySatelliteMarket(entity)
        if (entitySatelliteMarket != null) {
            displayError(entity.name + " failed doEntityNoSatellitesTest because " + entitySatelliteMarket.name + " was still applied")
            result = false
        }
        if (defenseSatellitesApplied(entity) || entity.memoryWithoutUpdate[niko_MPC_ids.satelliteHandlersId] != null) {
            if (Global.getSettings().isDevMode) {
                displayError(entity.name + " failed doEntityNoSatellitesTest because defenseSatellitesApplied returned true")
                result = false
            }
        }
        if (!result) {
            logDataOf(entity)
        }
        return result
    }

    /**
     * Checks getEntitySatelliteParams(entity) and looks to see if it isnt null.
     * @param entity The entity to check.
     * @return False if entity is null or the params are null. If params are null, an error will be displayed.
     */
    @JvmStatic
    fun assertEntityHasSatellites(entity: SectorEntityToken?): Boolean {
        var result = true
        if (entity == null) return false
        val handler = getSatelliteHandlerOfEntity(entity)
        if (handler == null) {
            displayError("assertEntityHasSatellitesFailure on " + entity + ", entity name: " + entity.name)
            logDataOf(entity)
            result = false
        }
        return result
    }

    fun isDebugMode(): Boolean {
        return (Global.getSettings().isDevMode)
    }

    fun CustomCampaignEntityAPI.isCosmeticSatelliteInValidState(): Boolean {
        //todo: the below might not work. if it does you want to refactor it
        if (!isCosmeticSatellite()) return true
        var result = true
        val entityHandler: niko_MPC_satelliteHandlerCore? = getSatelliteEntityHandler()
        if (isAlive && !(hasTag(Tags.FADING_OUT_AND_EXPIRING))) { //todo: consider coming back to this?
            if (entityHandler == null) {
                // error state, the only time in which a living satellite (non-deleted) shouldnt have a handler
                // is absolutely nothing
                displayError("$this was not alive but had no handler")
                result = false
            }
            else if (orbit == null) {
                displayError("Null orbit on $this, invalid state")
                result = false
            }
            else if (orbit.focus == null) {
                displayError("Null orbit focus on $this, invalid state")
                result = false
            }
            else if (orbit.focus.market != entityHandler.market) {
                displayError("Unsynced orbit focus market: ${orbit.focus.market}, ${orbit.focus.market.name} " +
                        ". Should be ${entityHandler.market}, ${entityHandler.market?.name}")
                result = false
            }
        }
        else if (entityHandler != null) {
            displayError("$this was not alive but still had a satellite handler: $entityHandler")
            logDataOf(entityHandler.entity)
            result = false
        }
        if (!result) {
            logDataOf(this)
            niko_MPC_satelliteUtils.removeSatellite(this)
        }
        return result
    }

    fun CampaignFleetAPI.isSatelliteFleetInValidState(): Boolean {
        if (!isSatelliteFleet()) return true
        var result = true
        val entityHandler: niko_MPC_satelliteHandlerCore? = getSatelliteEntityHandler()
        if (isAlive && !isDespawning) {
            if (entityHandler == null) {
                displayError("$this had no entityHandler despite isAlive being true and isdespawning being false")
                result = false
            }
        }
        else if (entityHandler != null) {
            displayError("$this was not alive but still had a satellite handler: $entityHandler")
            logDataOf(entityHandler.entity)
            result = false
        }
        if (!result) {
            logDataOf(this)
            satelliteFleetDespawn()
        }
        return result
    }

    inline fun <reified T> memKeyHasIncorrectType(hasMemory: HasMemory, key: String): Boolean {
        return memKeyHasIncorrectType<T>(hasMemory.memoryWithoutUpdate, key)
    }
    inline fun <reified T> memKeyHasIncorrectType(memory: MemoryAPI?, key: String): Boolean {
        if (memory == null) return false
        val cachedValue = memory[key]
        if (cachedValue !is T) {
            if (cachedValue != null) displayError(
                "Non-null invalid value in $this memory, key: $key." +
                        "Expected value: ${T::class.simpleName} Value: $cachedValue", true)
            return true
        }
        return false
    }

    fun logDataOf(obj: Any?) {
        if (obj != null) {
            when(obj) {
                is niko_MPC_dataLoggable -> doLogOf(obj, obj.provideLoggableData())
                // CampaignFleetAPI -> obj.logFleetData()
                is SectorEntityToken -> obj.logEntityData()
                is MarketAPI -> obj.logMarketData()
                else -> log.warn("$this is an unloggable type.")
            }
        } else log.debug("Cannot log debug info of obj-it is null.")
    }
    private fun doLogOf(obj: Any, loggableData: List<String>) {
        log.info("Now logging data of " + obj::class.simpleName + " :" + loggableData.joinToString(" "))
    }
    private fun SectorEntityToken.logEntityData() {
        doLogOf(this, arrayListOf("$this, ${this.name}", "Market: ${this.market}, Market Faction: ${this.market?.factionId}",
            "Entity location: ${this.containingLocation}, ${this.containingLocation?.name}, is star system: " +
            "${this.containingLocation is StarSystemAPI}", "Handlers: ${this.getSatelliteHandlers()}")
        )
    }
    private fun MarketAPI.logMarketData() {
        doLogOf(this, arrayListOf("$this, ${this.name}", "Entity: ${this.primaryEntity}, Faction: ${factionId}",
            "Our location: ${this.containingLocation}, ${this.containingLocation?.name}, is star system: " +
            "${this.containingLocation is StarSystemAPI}", "Handlers: ${this.getSatelliteHandlers()}")
        )
    }
}

fun SectorEntityToken.getSatelliteEntityHandler(): niko_MPC_satelliteHandlerCore? {
    if (memKeyHasIncorrectType<niko_MPC_satelliteHandlerCore>(this, niko_MPC_ids.satelliteEntityHandler)) {
        return null
    }
    return memoryWithoutUpdate[niko_MPC_ids.satelliteEntityHandler] as niko_MPC_satelliteHandlerCore
}

fun CampaignFleetAPI.isSatelliteFleet(): Boolean {
    return hasTag(niko_MPC_ids.isSatelliteFleetId)
}

fun CustomCampaignEntityAPI.isCosmeticSatellite(): Boolean {
    return (hasTag(niko_MPC_ids.cosmeticSatelliteTagId))
}

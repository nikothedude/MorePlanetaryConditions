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
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.exceptions.niko_MPC_stackTraceGenerator
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_miscUtils.isDespawning
import data.utilities.niko_MPC_satelliteUtils.deleteIfCosmeticSatellite
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.isCosmeticSatellite
import org.apache.log4j.Level
import org.apache.log4j.Logger
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
    @JvmStatic
    @JvmOverloads
    fun displayError(errorCode: String = "Unimplemented errorcode", highPriority: Boolean = false, crash: Boolean = false,
                     logType: Level = Level.ERROR) {

        if (niko_MPC_settings.SHOW_ERRORS_IN_GAME) {
            when (val gameState = Global.getCurrentState()) {
                GameState.CAMPAIGN -> displayErrorToCampaign(errorCode, highPriority)
                GameState.COMBAT -> displayErrorToCombat(errorCode, highPriority)
                GameState.TITLE -> displayErrorToTitle(errorCode, highPriority)
                else -> log.warn("Non-standard gamestate value during displayError, gamestate: $gameState")
            }
        }
        log.log(logType, "Error code:", niko_MPC_stackTraceGenerator(errorCode))
        if (crash) {
            throw RuntimeException(
                "A critical error has occurred in More Planetary Conditions, and for one reason" +
                " or another, the mod author has decided that this error is severe enough to warrant a crash." +
                " Error code: " + errorCode
            )
        }
    }

    @JvmStatic
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
        campaignUI.addMessage("Please provide the mod author of more planetary conditions a copy of your logs. These messages can be disabled in the niko_MPC_settings.json file in the MPC mod folder.")
    }

    @JvmStatic
    private fun displayErrorToCombat(errorCode: String, highPriority: Boolean) {
        val engine = Global.getCombatEngine()
        val combatUI = engine.combatUI
        combatUI.addMessage(
            1,
            "Please provide the mod author of more planetary conditions with a copy of your logs. These messages can be disabled in the niko_MPC_settings.json file in the MPC mod folder."
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

    @JvmStatic
    private fun displayErrorToTitle(errorCode: String, highPriority: Boolean) {
        return
    }

    @JvmStatic
    fun isDebugMode(): Boolean {
        return (Global.getSettings().isDevMode)
    }

    @JvmStatic
    fun CustomCampaignEntityAPI.isCosmeticSatelliteInValidState(): Boolean {
        //todo: the below might not work. if it does you want to refactor it
        if (!isCosmeticSatellite()) return true
        var result = true
        val entityHandler: niko_MPC_satelliteHandlerCore? = getSatelliteEntityHandler()
        if (!isDespawning()) { //todo: consider coming back to this?
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
            deleteIfCosmeticSatellite()
        }
        return result
    }

    @JvmStatic
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

    @JvmStatic
    inline fun <reified T> memKeyHasIncorrectType(hasMemory: HasMemory, key: String): Boolean {
        return memKeyHasIncorrectType<T>(hasMemory.memoryWithoutUpdate, key)
    }

    @JvmStatic
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

    @JvmStatic
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
    @JvmStatic
    private fun doLogOf(obj: Any, loggableData: List<String>) {
        log.info("Now logging data of " + obj::class.simpleName + " :" + loggableData.joinToString(" "))
    }
    @JvmStatic
    private fun SectorEntityToken.logEntityData() {
        doLogOf(this, arrayListOf("$this, ${this.name}", "Market: ${this.market}, Market Faction: ${this.market?.factionId}",
            "Entity location: ${this.containingLocation}, ${this.containingLocation?.name}, is star system: " +
            "${this.containingLocation is StarSystemAPI}", "Handlers: ${this.getSatelliteHandlers()}")
        )
    }
    @JvmStatic
    private fun MarketAPI.logMarketData() {
        doLogOf(this, arrayListOf(
            this.name, "Entity: ${this.primaryEntity}, Faction: ${factionId}",
            "Our location: ${this.containingLocation}, ${this.containingLocation?.name}, is star system: " +
            "${this.containingLocation is StarSystemAPI}", "Handlers: ${this.getSatelliteHandlers()}")
        )
    }
}

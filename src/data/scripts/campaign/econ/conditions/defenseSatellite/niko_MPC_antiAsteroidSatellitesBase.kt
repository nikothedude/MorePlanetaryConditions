package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.scripts.campaign.econ.conditions.niko_MPC_industryAddingCondition
import data.scripts.campaign.econ.industries.niko_MPC_defenseSatelliteLuddicSuppressor
import data.scripts.everyFrames.niko_MPC_satelliteCustomEntityRemovalScript
import data.utilities.*
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_satelliteUtils.getConditionLinkedHandler
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler
import data.utilities.niko_MPC_satelliteUtils.setConditionLinkedHandler
import org.apache.log4j.Level
import kotlin.math.abs


/** Base class for all forms of satellite-bound conditions. This condition should, apon application, create a new handler
 * for the [market], and bind itself to it. The reason that the handler is separate is because the handler can exist
 * on it's own, without a condition. The only thing a condition adds is 1. binding the handler to the condition, so
 * deleting it if the condition is removed, 2. binding the handler to the market, so it follows wherever the market is, rather
 * than the entity of the market, and 3. adding market-specific effects that the generic handler cannot.*/
abstract class niko_MPC_antiAsteroidSatellitesBase: niko_MPC_industryAddingCondition(), niko_MPC_dataLoggable {
    abstract val suppressorId: String
    override val isEnabled: Boolean
        get() = niko_MPC_settings.DEFENSE_SATELLITES_ENABLED
    var deletionScript: niko_MPC_satelliteCustomEntityRemovalScript? = null

    fun getHandlerWithErrorCheck(doNullCheck: Boolean = true): niko_MPC_satelliteHandlerCore? {
        val ourHandler = getHandler()
        if (doNullCheck && ourHandler == null) displayError("handler null during getHandler on $this")
        return ourHandler
    }

    override fun apply(id: String) {
        super.apply(id) // in the rare case that market is null (should never happen, none of my code is written to account for it)
        // we should avoid any jank behavior by having a specific error condition for it
        val ourMarket = getMarket() ?: return
        val ourEntity: SectorEntityToken? = ourMarket.primaryEntity

        val isValidTargetForHandler = (ourMarket.id != "fake_Colonize") // markets made during loading have null tags
        if (isValidTargetForHandler) {
            val satelliteHandler: niko_MPC_satelliteHandlerCore? = getHandlerWithUpdate()
            // DO NOT REMOVE THIS NULLCHECK THIS PREVENTS THE HANDLER BEING ACCESSED BEFORE ITS DESERIALIZED!!!!
            if (satelliteHandler != null && satelliteHandler.entity != null) {
                if (satelliteHandler.market === ourMarket) {
                    updateHandlerValues()
                    satelliteHandler.addNewConditionToDelete(this)
                }
                val lastCondition = satelliteHandler.conditions.lastOrNull()
               /* if (lastCondition !== this) {
                    displayError("niko's theory has been proven false.", true, logType = Level.INFO)
                }*/ // on post game-creation, this is triggered
            }
        }
        handleConditionAttributes(id, ourMarket)
    }

    private fun getHandlerWithUpdate(): niko_MPC_satelliteHandlerCore? {
        val ourMarket = getMarket() ?: return null
        if (getHandler() == null) {
            val newHandler = createNewHandler()
        }
        return getHandler()
    }

    fun getHandler(): niko_MPC_satelliteHandlerCore? {
        val ourMarket = getMarket() ?: return null
        return ourMarket.getConditionLinkedHandler(this.condition.id)
    }

    override fun unapply(id: String) {
        super.unapply(id)

        val ourMarket = getMarket() ?: return
        unapplyConditionAttributes(id, ourMarket)

        prepareToRemoveSatellites(ourMarket)
    }

    protected open fun prepareToRemoveSatellites(ourMarket: MarketAPI) {
        addDeletionScriptToMarket(ourMarket)
    }

    protected open fun addDeletionScriptToMarket(ourMarket: MarketAPI) {
        val ourEntity = ourMarket.primaryEntity ?: return
        val ourHandler = getHandler()
        var shouldAdd = false
        if (ourHandler != null) {
            if (deletionScript != null) {
                if (deletionScript!!.handler != ourHandler) {
                    // global beacuse during loading entity's scripts just dont exist at all
                    //TODO: make it so that this accesses a memkey to see if we already have a script
                    displayError("desynced handler, $ourHandler on $this deletionscript: $deletionScript, ${deletionScript!!.handler}")
                    shouldAdd = true
                }
            } else shouldAdd = true
            if (shouldAdd) {
                deletionScript = createDeletionScript(ourEntity, ourHandler)
                deletionScript?.start()
            }
        }
    }

    /** Should EXCLUSVELY create and return a removal script, no side effects. */
    protected open fun createDeletionScript(ourEntity: SectorEntityToken, ourHandler: niko_MPC_satelliteHandlerCore): niko_MPC_satelliteCustomEntityRemovalScript {
        return niko_MPC_satelliteCustomEntityRemovalScript(ourEntity, condition.id, ourHandler, this)
    }

    /** Generic value-based and non-jank operations should be here. Ex. an access buff.*/
    abstract fun handleConditionAttributes(id: String, ourMarket: MarketAPI)

    /** Generic value-based and non-jank operations should be here. Ex. an access buff removal.*/
    abstract fun unapplyConditionAttributes(id: String, ourMarket: MarketAPI)

    protected fun createNewHandler(): niko_MPC_satelliteHandlerCore? {
        val ourMarket = getMarket() ?: return null
        val ourEntity = ourMarket.primaryEntity ?: return null
        val handler = createNewHandlerInstance(ourEntity)
        ourMarket.setConditionLinkedHandler(this.condition.id, handler)
        handler.addNewConditionToDelete(this)
        assignHandlerToMarket()

        return getHandler()
    }

    /** Should EXCLUSIVELY exist to create a new instance. No side effects. */
    abstract fun createNewHandlerInstance(entity: SectorEntityToken): niko_MPC_satelliteHandlerCore

    protected fun assignHandlerToMarket() {
        val ourMarket = getMarket() ?: return
        val ourHandler = getHandler() ?: return
        niko_MPC_satelliteUtils.instantiateSatellitesOntoMarket(ourHandler, ourMarket)
    }

    protected open fun updateHandlerValues(handler: niko_MPC_satelliteHandlerCore? = getHandlerWithErrorCheck()) {
        if (handler == null) return
        val ourMarket = getMarket() ?: return
        if (handler.market === ourMarket) {
            val entity = ourMarket.primaryEntity ?: return
            handler.entity = entity
            val factionid = ourMarket.factionId ?: "neutral"
            handler.currentSatelliteFactionId = factionid
        }
    }

    override fun handleMarketDesyncEffect() {
        val ourHandler = getHandlerWithErrorCheck()
        if (ourHandler != null) {
            if (cachedMarket != null) {
                if (cachedMarket!!.hasSatelliteHandler(ourHandler)) {
                    return displayError("Desync check failure-$cachedMarket still has $ourHandler" + "applied to it")
                }
            }
        }
        super.handleMarketDesyncEffect()
    }

    fun getLuddicSupression(): Int {
        var patherInterestReductionAmount = 0

        if (market.hasIndustry(suppressorId)) {
            val industry = market.getIndustry(suppressorId) as niko_MPC_defenseSatelliteLuddicSuppressor
            patherInterestReductionAmount = abs(industry.patherInterest).toInt()
        } else {
            displayError("no luddic path supressor on $market during $this getLuddincSupression")
            logDataOf(this)
            if (market != null) {
                logDataOf(market)
            }
        }
        return patherInterestReductionAmount
    }

    override fun delete() {
        val ourHandler = getHandler()
        if (ourHandler != null) {
            ourHandler.conditions -= this
        }

        super.delete()
    }

    override fun isTransient(): Boolean = false

    override fun provideLoggableData(): List<String> {
        return arrayListOf(
            "$this, ${this.name}",
            "handler: ${getHandler()}",
            "Market: $market",
            "Market name: ${market?.name}, market faction: ${market?.factionId}, " +
                    "market entity: ${market?.primaryEntity}, market entity name: ${market?.primaryEntity?.name}, market" +
                    "location: ${market?.containingLocation}"
        )
    }
}

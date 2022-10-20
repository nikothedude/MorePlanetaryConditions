package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.niko_MPC_industryAddingCondition
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.scripts.everyFrames.niko_MPC_satelliteCustomEntityRemovalScript
import data.utilities.*
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.isCosmeticSatelliteInValidState
import data.utilities.niko_MPC_debugUtils.isSatelliteFleetInValidState
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

/** Base class for all forms of satellite-bound conditions. This condition should, apon application, create a new handler
 * for the [market], and bind itself to it. The reason that the handler is separate is because the handler can exist
 * on it's own, without a condition. The only thing a condition adds is 1. binding the handler to the condition, so
 * deleting it if the condition is removed, 2. binding the handler to the market, so it follows wherever the market is, rather
 * than the entity of the market, and 3. adding market-specific effects that the generic handler cannot.*/
abstract class niko_MPC_antiAsteroidSatellitesBase: niko_MPC_industryAddingCondition(), niko_MPC_dataLoggable {
    override val isEnabled: Boolean
        get() = niko_MPC_settings.DEFENSE_SATELLITES_ENABLED

    //todo: READ ME NIKO.
    // we can move to using market instead of entity for most things
    // except for scripts and cosmetics and shit
    // market.getContaningLocation and .getLocation exist and work

    /** The primary reason this is done on a handler is because this is detachable from conditions
     * and can be done seperately. Also I hate storing data on conditions out of paranoia.*/
    var handler: niko_MPC_satelliteHandlerCore? = null
    fun getHandlerWithErrorCheck(doNullCheck: Boolean = true): niko_MPC_satelliteHandlerCore? {
        if (doNullCheck && handler == null) displayError("handler null during getHandler on $this")
        return handler
    }

    override fun apply(id: String) {
        super.apply(id) // in the rare case that market is null (should never happen, none of my code is written to account for it)
        // we should avoid any jank behavior by having a specific error condition for it
        val ourMarket = getMarket() ?: return

        var satelliteHandler: niko_MPC_satelliteHandlerCore? = handler
        if (satelliteHandler == null) { // realistically this will only happen if we were just created but it may happen more
            doHadHandlerReferencesProperlyRemovedCheck() // make sure that if we had it removed before, we properly removed all of it's stuff
            // ^ as of now, only does most of its stuff in devmode to avoid performance loss
            satelliteHandler = createNewHandler() // reassign it to a new handler. 100% of the time if this condition is active
        } else { //update the values of our handler
            // dont need to check these values if we make a new handler because we alreayd know what they are
            updateHandlerValues(satelliteHandler)
        }
        handleConditionAttributes(id, ourMarket)
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
        val ourHandler = getHandlerWithErrorCheck()
        if (ourHandler != null) {
            // global beacuse during loading entity's scripts just dont exist at all
            //TODO: make it so that this accesses a memkey to see if we already have a script
            val deletionScript = createDeletionScript(ourMarket, ourHandler)
            Global.getSector().addScript(deletionScript)
        }
    }

    /** Should EXCLUSVELY create and return a removal script, no side effects. */
    protected open fun createDeletionScript(ourMarket: MarketAPI, ourHandler: niko_MPC_satelliteHandlerCore): niko_MPC_baseNikoScript {
        return niko_MPC_satelliteCustomEntityRemovalScript(ourMarket, ourHandler)
    }

    /** Generic value-based and non-jank operations should be here. Ex. an access buff.*/
    abstract fun handleConditionAttributes(id: String, ourMarket: MarketAPI)

    /** Generic value-based and non-jank operations should be here. Ex. an access buff removal.*/
    abstract fun unapplyConditionAttributes(id: String, ourMarket: MarketAPI)

    override fun advance(amount: Float) {
        super.advance(amount)

        TODO() // do i even want to use the advance here? maybe just put it on the handler?
    }

    protected fun createNewHandler(): niko_MPC_satelliteHandlerCore {
        handler = createNewHandlerInstance()
        assignHandlerToMarket()

        return handler!!
    }

    /** Should EXCLUSIVELY exist to create a new instance. No side effects. */
    abstract fun createNewHandlerInstance(): niko_MPC_satelliteHandlerCore

    protected fun assignHandlerToMarket() {
        val ourMarket = getMarket() ?: return
        val ourHandler = getHandlerWithErrorCheck() ?: return
        niko_MPC_satelliteUtils.instantiateSatellitesOntoMarket(ourHandler, ourMarket)
    }

    protected open fun updateHandlerValues(handler: niko_MPC_satelliteHandlerCore? = getHandlerWithErrorCheck()) {
        if (handler == null) return
        val ourMarket = getMarket() ?: return
        handler.market = ourMarket
        handler.entity = ourMarket.primaryEntity
        handler.currentSatelliteFactionId = ourMarket.factionId
    }

    override fun handleMarketDesyncEffect() {
        val ourHandler = getHandlerWithErrorCheck()
        if (ourHandler != null) {
            if (cachedMarket != null) {
                if (cachedMarket!!.hasSatelliteHandler(handler!!)) {
                    return displayError("Desync check failure-$cachedMarket still has $handler" + "applied to it")
                    TODO() //more debuggign code, ex. code that tells us if both the cachedmarket and our market have
                    // the handler in memory or smthn
                }
            }
        }
        super.handleMarketDesyncEffect()
    }

    /// TEST FUNCTIONS

    protected fun handleNonNullHandlerWithNoPrimaryEntity() {
        TODO("Not yet implemented")
    }

    /** Only call this when handler is expected to be null.*/
    protected fun doHadHandlerReferencesProperlyRemovedCheck(entity: SectorEntityToken? = getMarket()?.primaryEntity) {
        val ourMarket = getMarket() ?: return
        if (niko_MPC_debugUtils.isDebugMode()) { //this would cause some fucked up lag otherwise
            // THESE DO NOT NEED HANDLER PASSED OR A HANDLER CHECK! These are meant to be ran if handler is NULL!
            for (cosmeticSatellite: CustomCampaignEntityAPI in ourMarket.getPotentialCosmeticSatellites()) {
                cosmeticSatellite.isCosmeticSatelliteInValidState()
            }
            for (satelliteFleet : CampaignFleetAPI in ourMarket.getPotentialSatelliteFleets()) {
                satelliteFleet.isSatelliteFleetInValidState()
            }
        }
        if (handler != null) {
            displayError("$handler not null when it should be")
            handler!!.delete()
        }
    }

    /** Handler can be null here. */
    fun MarketAPI.getPotentialCosmeticSatellites(): List<CustomCampaignEntityAPI> {
        if (handler != null) return ArrayList<CustomCampaignEntityAPI>(handler!!.cosmeticSatellites)
        val cosmeticSatellitesList = ArrayList<CustomCampaignEntityAPI>()
        val primaryEntity = primaryEntity ?: return cosmeticSatellitesList
        if (containingLocation != null) {
            for (potentialCosmeticSatellite: CustomCampaignEntityAPI in containingLocation.getCustomEntitiesWithTag(niko_MPC_ids.cosmeticSatelliteTagId)) {
                if (potentialCosmeticSatellite.orbitFocus == primaryEntity) {
                    cosmeticSatellitesList.add(potentialCosmeticSatellite)
                }
            }
        }
        return cosmeticSatellitesList
    }

    fun MarketAPI.getPotentialSatelliteFleets(): List<CampaignFleetAPI> {
        if (handler != null) return ArrayList<CampaignFleetAPI>(handler!!.getAllSatelliteFleets())
        val satelliteFleetList = ArrayList<CampaignFleetAPI>()
        //val primaryEntity = primaryEntity ?: return satelliteFleetList
        if (containingLocation != null) {
            for (potentialSatelliteFleet: CampaignFleetAPI in containingLocation.fleets) {
                if (potentialSatelliteFleet.isSatelliteFleet()) {
                    if (potentialSatelliteFleet.market == this) { //todo
                        satelliteFleetList.add(potentialSatelliteFleet)
                    }
                }
            }
        }
        return satelliteFleetList
    }

    override fun isTransient(): Boolean = false

    override fun provideLoggableData(): List<String> {
        return arrayListOf("$this, ${this.name}", "handler: $handler" ,"Market: $market", "Market name: ${market?.name}, market faction: ${market?.factionId}, " +
                "market entity: ${market?.primaryEntity}, market entity name: ${market?.primaryEntity?.name}, market" +
                "location: ${market?.containingLocation}")
    }
}

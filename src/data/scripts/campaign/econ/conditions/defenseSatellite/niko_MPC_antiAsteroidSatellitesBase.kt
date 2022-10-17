package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.niko_MPC_industryAddingCondition
import data.scripts.everyFrames.niko_MPC_satelliteCustomEntityRemovalScript
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.isCosmeticSatelliteInValidState
import data.utilities.niko_MPC_debugUtils.isSatelliteFleetInValidState
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandler
import org.apache.log4j.Level

abstract class niko_MPC_antiAsteroidSatellitesBase : niko_MPC_industryAddingCondition() {

    private val log = Global.getLogger(niko_MPC_antiAsteroidSatellitesBase::class.java)

    init {
        log.level = Level.ALL
        industryIds.add(niko_MPC_industryIds.luddicPathSuppressorStructureId)
    }

    //todo: READ ME NIKO.
    // we can move to using market instead of entity for most things
    // except for scripts and cosmetics and shit
    // market.getContaningLocation and .getLocation exist and work

    override fun apply(id: String) {
        super.apply(id) // in the rare case that market is null (should never happen, none of my code is written to account for it)
        // we should avoid any jank behavior by having a specific error condition for it
        val ourMarket = getMarket() ?: return

        var satelliteHandler: niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler()
        if (satelliteHandler == null) { // doesnt have our type of satellite handler
            doHadHandlerReferencesProperlyRemovedCheck() // make sure that if we had it removed before, we properly removed all of it's stuff
            // ^ as of now, only does most of its stuff in devmode to avoid performance loss
            satelliteHandler = createNewHandler() // reassign it to a new handler. 100% of the time if this condition is active
            // the market should have a handler
        } else { //update the values of our handler
            updateHandlerValues(satelliteHandler)
            checkForMarketDesync(satelliteHandler) //todo: move this into the industry adder abstract thingy
        }
        handleConditionAttributes(id, ourMarket)
    }

    override fun unapply(id: String) {
        super.unapply(id)

        val ourMarket = getMarket() ?: return
        unapplyConditionAttributes(id, ourMarket)

        prepareToRemoveSatellites(ourMarket)
    }

    protected fun prepareToRemoveSatellites(ourMarket: MarketAPI) {
        // global beacuse during loading entity's scripts just dont exist at all
        val ourHandler = getMarketSatelliteHandler()
        if (ourHandler != null) {
            Global.getSector().addScript(niko_MPC_satelliteCustomEntityRemovalScript(ourMarket, condition.idForPluginModifications))
        }
    }

    /** Generic value-based and non-jank operations should be here. Ex. an access buff.*/
    abstract fun handleConditionAttributes(id: String, ourMarket: MarketAPI)

    /** Generic value-based and non-jank operations should be here. Ex. an access buff removal.*/
    abstract fun unapplyConditionAttributes(id: String, ourMarket: MarketAPI)

    override fun advance(amount: Float) {
        super.advance(amount)
        val ourMarket = getMarket() ?: return
        TODO()
    }

    /** Should force the satellite handler to update the factions of all of its entities. */
    protected fun updateSatelliteFactions(handler: niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler()) {
        handler?.updateSatelliteFactions()
    }

    protected fun getMarketSatelliteHandler(): niko_MPC_satelliteHandlerCore? {
        val ourMarket = getMarket() ?: return null
        return ourMarket.getSatelliteHandler(getHandlerType()) //store it on the market, not entity
    }

    abstract fun getHandlerType(): String //todo: maybe make it so we associate handlers with $this?

    fun createNewHandler(): niko_MPC_satelliteHandlerCore {
        val newHandler: niko_MPC_satelliteHandlerCore = createNewHandlerInstance()

        TODO()
    }

    abstract fun createNewHandlerInstance(): niko_MPC_satelliteHandlerCore
    
    protected fun updateHandlerValues(handler: niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler()) {
        if (handler == null) return
        val ourMarket = getMarket() ?: return
        handler.market = ourMarket
        handler.entity = ourMarket.primaryEntity
        handler.currentSatelliteFactionId = ourMarket.factionId
    }

    protected fun getMarket(doNullCheck: Boolean = true): MarketAPI? {
        if (doNullCheck && market == null) {
            handleNullMarket()
        }
        return market
    }
    /// EDGECASE FAILSAFES
    protected fun checkForMarketDesync(handler: niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler()) {
        if (handler == null) return
        val ourMarket = getMarket() ?: return
        val cachedMarket = handler.cachedMarket
        if (ourMarket !== cachedMarket) handleMarketDesync(handler, cachedMarket)
    }
    
    protected fun handleMarketDesync(handler: niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler(), cachedMarket: MarketAPI?) {
        if (handler == null) return
        val ourMarket = getMarket() ?: return
        if (ourMarket === cachedMarket) {
            displayError("desync check error: $market, ${market.name} is the same as the provided cached market")
            return
        }
        if (satelliteMarket != null) {
            if (satelliteMarket.hasSatellites(getHandlerType())) {
                return displayError("Desync check failure-$cachedMarket still has ${cachedMarket.getSatelliteHandler(getHandlerType())}" + "applied to it")
            }
            else if (cachedMarket != null) tryToUnapplyIndustries(cachedMarket)
        }
    }

    /** Just in case of insanity, we should have some error handling for a null market.*/
    protected fun handleNullMarket() {
        displayError("Something has gone terribly wrong and market was $market in $this.")
    }

    /// TEST FUNCTIONS

    protected fun handleNonNullHandlerWithNoPrimaryEntity() {
        TODO("Not yet implemented")
    }

    /** Only call this when handler is expected to be null.*/
    protected fun doHadHandlerReferencesProperlyRemovedCheck(entity: SectorEntityToken? = getMarket()?.primaryEntity) {
        val ourMarket = getMarket() ?: return
        if (niko_MPC_debugUtils.isDebugMode()) { //this would cause some fucked up lag otherwise
            for (cosmeticSatellite: CustomCampaignEntityAPI in niko_MPC_satelliteUtils.getPotentialCosmeticSatellites(ourMarket, entity, this)) {
                cosmeticSatellite.isCosmeticSatelliteInValidState()
            }
            for (satelliteFleet : CampaignFleetAPI in niko_MPC_satelliteUtils.getPotentialSatelliteFleets(ourMarket, entity, this)) {
                satelliteFleet.isSatelliteFleetInValidState()
            }
        }
        val handler : niko_MPC_satelliteHandlerCore? = getMarketSatelliteHandler()
        if (handler != null) {
            displayError("$handler not null when it should be")
            handler.delete()
        }
    }

    /** Should be called when the market has migrated through schenanigans and we need to remove shit. */
    protected fun doMarketMigrationLeftoversCleanup(currentEntity: SectorEntityToken?, oldEntity: SectorEntityToken?) {
        doHadHandlerReferencesProperlyRemovedCheck()
    }
}

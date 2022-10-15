package data.scripts.campaign.econ

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.misc.niko_MPC_satelliteHandler
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_debugUtils.isCosmeticSatelliteInValidState
import data.utilities.niko_MPC_debugUtils.isSatelliteFleetInValidState
import data.utilities.niko_MPC_industryIds
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandler
import org.apache.log4j.Level
import kotlin.math.cos

abstract class niko_MPC_antiAsteroidSatellitesBase : niko_MPC_industryAddingCondition() {

    private val log = Global.getLogger(niko_MPC_antiAsteroidSatellitesBase::class.java)

    init {
        industryIds.add(niko_MPC_industryIds.luddicPathSuppressorStructureId)
        log.level = Level.ALL
    }

    override fun apply(id: String) {
        super.apply(id) // in the rare case that market is null (should never happen, none of my code is written to account for it)
        // we should avoid any jank behavior by having a specific error condition for it
        val ourMarket = getMarket() ?: return handleNullMarket()

        // primaryentity can be null in certain cases, such as a ghost market, or fake market, and in that case we cant apply satellites
        // not an error state!
        val primaryEntity: SectorEntityToken? = ourMarket.primaryEntity
        if (primaryEntity != null) {
            val satelliteHandler: niko_MPC_satelliteHandler? = ourMarket.getSatelliteHandler()
            if (satelliteHandler == null) {
                doEntityProperlyHadHandlerRemovedCheck()
                initializeSatellitesOntoEntity()
            }
            TODO() //todo: put the market syncing code here. i kinda forgot what it was used for

        }
    }

    /** Just in case of insanity, we should have some error handling for a null market.*/
    protected fun handleNullMarket() {
        niko_MPC_debugUtils.displayError("Something has gone terribly wrong and market was $market in $this.")
    }

    /** Only call this when handler is expected to be null.*/
    protected fun doEntityProperlyHadHandlerRemovedCheck() {
        val ourMarket = getMarket() ?: return handleNullMarket()
        if (niko_MPC_debugUtils.isDebugMode()) {
            for (cosmeticSatellite: CustomCampaignEntityAPI in niko_MPC_satelliteUtils.getPotentialCosmeticSatellites(ourMarket, this)) {
                cosmeticSatellite.isCosmeticSatelliteInValidState()
            }

            for (satelliteFleet : CampaignFleetAPI in niko_MPC_satelliteUtils.getPotentialSatelliteFleets(ourMarket, this)) {
               satelliteFleet.isSatelliteFleetInValidState()
            }
        }
        val handler : niko_MPC_satelliteHandler? = market.getSatelliteHandler()
        if (handler != null) {
            niko_MPC_debugUtils.displayError("$handler not null when it should be")
            handler.delete()
        }
    }

    protected fun initializeSatellitesOntoEntity() {
        TODO("Not yet implemented")
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        val ourMarket = getMarket() ?: return handleNullMarket()
    }

    protected fun getMarket(): MarketAPI? {
        return market
    }
}
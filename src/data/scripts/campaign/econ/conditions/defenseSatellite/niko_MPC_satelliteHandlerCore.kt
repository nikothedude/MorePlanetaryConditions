package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset
import kotlin.math.cos
import kotlin.math.roundToInt


/** The logic and data core of satellites.
 * Should be applicable to markets and entities. */
abstract class niko_MPC_satelliteHandlerCore(
    entity: SectorEntityToken?,
    market: MarketAPI? = entity?.market,
    val industryIds: List<String>,
    val conditionId: String? = null
) {
    var entity: SectorEntityToken? = entity //entity should be detachable from market
        set(value) {
            field = value
            if (field !== cachedEntity) handleEntityDesync()
        }

    var cachedEntity: SectorEntityToken? = this.entity

    var market: MarketAPI? = market
        set(value) {
            field = value
            if (field !== cachedMarket) handleMarketDesync()
            //the below is fine; SS is not multithreaded so this wont break. yet? i wish i could do this in a threadsafe way easily
            currentSatelliteFactionId = (if (market != null) market!!.factionId else defaultSatelliteFactionId)
        }

    var cachedMarket: MarketAPI? = this.market
    val defaultSatelliteFactionId: String = "derelict" //todo: make abstract as a test to see if it npes?
    var currentSatelliteFactionId: String = defaultSatelliteFactionId
        set(value) {
            if (field != value) updateSatelliteFactions(value)
            // no !== because if we use that, "blah" !== "blah" return false, while
            // "blah" != "blah" returns true due to contents being the same
            field = value
        }

    val cosmeticSatellites: MutableList<CustomCampaignEntityAPI> = ArrayList()
    val satelliteFleets: MutableList<CampaignFleetAPI> = ArrayList()
    abstract val satelliteFleetName: String
    abstract val maximumSatelliteFleetFp: Int
    abstract val cosmeticSatelliteName: String
    var maxCosmeticSatellitesForEntity: Int = calculateMaxCosmeticSatellitesForEntity()

    /** The ID of the dummy faction used to construct our fleets. */
    abstract val satelliteConstructionFactionId: String

    var dummyFleetForConditionalLogic: CampaignFleetAPI = createNewDummyFleet()
    protected fun createNewDummyFleet(): CampaignFleetAPI = TODO()

    abstract val satelliteOrbitDistance: Float

    /** Handles situations where we migrated entities, ex. our satellites, or our condition, moved to a new entity, or
     * market with a different entity.*/
    protected fun handleEntityDesync() {
        val oldEntity = cachedEntity
        var currentEntity = entity
        if (currentEntity === oldEntity) {
            displayError("desync attempt: $oldEntity, ${oldEntity?.name} is the same as the provided entity")
            return //todo: change to one line
        }
        if (oldEntity != null) {
            migrateEntityFeatures(oldEntity, currentEntity)
        }
        cachedEntity = entity
    }

    /**Assumes the current entity is the entity to migrate things to.*/
    private fun migrateEntityFeatures(oldEntity: SectorEntityToken, migrationTarget: SectorEntityToken?) {
        deleteCosmeticSatellites()
        maxCosmeticSatellitesForEntity = calculateMaxCosmeticSatellitesForEntity()
        createNewCosmeticSatellites(maxCosmeticSatellitesForEntity)
        deleteAllFleets()
        
        TODO() // not done! might want to attach a few more entity-specific variables, like stations
        // (put those on market?)
    }

    /** Handles situations where we migrated markets, ex. our condition moved to a new market.*/
    protected fun handleMarketDesync() {
        val oldMarket = cachedMarket
        val currentMarket = market
        if (currentMarket === oldMarket) {
            displayError("desync attempt: $oldMarket, ${oldMarket?.name} is the same as the provided market")
            return
        }
        if (oldMarket != null) {
            if (oldMarket.hasSatellites(this.getType())) {
                displayError("Desync check failure-$oldMarket still has $this" + "applied to it")
            }
            else  {
                migrateMarketFeatures(oldMarket, currentMarket)
            }
        }
        cachedMarket = market
    }

    protected fun migrateMarketFeatures(oldMarket: MarketAPI?, migrationTarget: MarketAPI) {
        TODO("Not yet implemented")
    }

    protected fun createNewCosmeticSatellites(amountToAdd: Int) {
        var index = amountToAdd
        while (index > 0 && cosmeticSatellites.size < maxCosmeticSatellitesForEntity) {
            index--
            createNewCosmeticSatellite()
        }
        regenerateOrbitSpacing()
    }

    /** Dont call this directly.*/
    private fun createNewCosmeticSatellite() {
        val cosmeticSatellite: CustomCampaignEntityAPI =
        TODO("Not yet implemented")
    }

    protected fun deleteCosmeticSatellites(satellitesToDelete: ArrayList<CustomCampaignEntityAPI> = ArrayList(cosmeticSatellites)) {
        for (cosmeticSatellite: CustomCampaignEntityAPI in satellitesToDelete) cosmeticSatellite.deleteIfCosmeticSatellite()
    }

    fun updateSatelliteFactions(factionId: String = currentSatelliteFactionId) {
        for (cosmeticSatellite: CustomCampaignEntityAPI in cosmeticSatellites) {
            cosmeticSatellite.setFaction(factionId)
        }
        for (satelliteFleet: CampaignFleetAPI in getAllSatelliteFleets()) {
            satelliteFleet.setFaction(factionId)
        }
        TODO()
    }

    fun getAllSatelliteFleets(): List<CampaignFleetAPI> {
        if (dummyFleetForConditionalLogic != null) {
            return (satelliteFleets + dummyFleetForConditionalLogic)
        }
        return satelliteFleets
    }

    protected fun getOptimalOrbitalOffsetForSatellites(): Float {
        val numOfSatellites = cosmeticSatellites.size
        var optimalAngle = (360 / numOfSatellites.toFloat())
        // 1 satellite = offset of 360, so none. 2 satellites = offset or 180, so they are on opposite ends of the planet.
        // 3 satellites = offset of 120, meaning the satellites form a triangle around the entity. Etc.
        if (optimalAngle == 360f) {
            optimalAngle = 0f //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle
    }

    /**
     * Places all satellites in orbit around our entity, ensuring they are all equally spaced apart from eachother.
     */
    protected fun regenerateOrbitSpacing() {
        val optimalOrbitAngleOffset = getOptimalOrbitalOffsetForSatellites()
        var orbitAngle = 0f
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc,
        // so its safe to not add a buffer to the calculation in the optimalangle method
        for (cosmeticSatellite: CustomCampaignEntityAPI in ArrayList(cosmeticSatellites)) {
            var errored = false
            if (orbitAngle >= 360) {
                if (!errored) displayError("regenerateOrbitSpacing orbitAngle = $orbitAngle")
                errored = true
                cosmeticSatellite.deleteIfCosmeticSatellite()
            }
            addOrbitPointingDownWithRelativeOffset(
                cosmeticSatellite,
                entity,
                orbitAngle,
                satelliteOrbitDistance
            )
            orbitAngle += optimalOrbitAngleOffset //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    protected fun calculateMaxCosmeticSatellitesForEntity(radiusDivisor: Float = 5f): Int {
        if (entity == null || entity!!.radius == 0f) return 0;
        return ((entity!!.radius) / radiusDivisor).roundToInt();
        // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    fun delete() {
        TODO("Not yet implemented")
    }
}

/// COSMETIC SATELLITES EXTENSIONS

fun CustomCampaignEntityAPI.deleteIfCosmeticSatellite() {
    if (!isCosmeticSatellite()) return
    val handler: niko_MPC_satelliteHandlerCore = getCosmeticSatelliteHandler() ?: return
    handler.cosmeticSatellites.remove(this)
    Misc.fadeAndExpire(this) //this causes a removeentity after a bit
    //containingLocation.removeEntity(this)
}

package data.scripts.campaign.econ.conditions.defenseSatellite

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import data.scripts.campaign.listeners.niko_MPC_satelliteFleetDespawnListener
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer
import data.scripts.everyFrames.niko_MPC_satelliteFleetProximityChecker
import data.scripts.everyFrames.niko_MPC_satelliteStationBattleChecker
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.*
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.log
import data.utilities.niko_MPC_debugUtils.logDataOf
import data.utilities.niko_MPC_fleetUtils.isDummyFleet
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_fleetUtils.setDummyFleet
import data.utilities.niko_MPC_fleetUtils.setSatelliteEntityHandler
import data.utilities.niko_MPC_fleetUtils.trimDownToFP
import data.utilities.niko_MPC_satelliteUtils.deleteIfCosmeticSatellite
import data.utilities.niko_MPC_satelliteUtils.getAllSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.apache.log4j.Level
import kotlin.math.roundToInt

/** The logic and data core of satellites.
 * Should be applicable to markets and entities.
 * DO NOT USE THE CONSTRUCTOR AS-IS. USE THE COMPANION FACTORY TO ASSIGN IT PROPERLY.*/
abstract class niko_MPC_satelliteHandlerCore(
    entity: SectorEntityToken?,
    market: MarketAPI? = entity?.market,
    var condition: niko_MPC_antiAsteroidSatellitesBase? = null) : niko_MPC_dataLoggable {
    /** Should always and only be set true in [delete], and should cause it to return if true with an error. */
    var deleted: Boolean = false
    /** Worth noting that cosmetic satellites do not exist, at all, without a entity. They cannot even be
     * created.
     *
     * If changed, should call [handleEntityDesync] if the changed value is not the same as [cachedEntity]. */
    var entity: SectorEntityToken? = entity //entity should be detachable from market
        set(value) {
            field = value
            if (field !== cachedEntity) handleEntityDesync()
        }
    /** Should only be set in [handleEntityDesync] or it's called funcs. Should cause [handleEntityDesync] to be called
     * if, apon [entity] set(), [entity] is not the same as this. */
    var cachedEntity: SectorEntityToken? = this.entity
    /** If changed, should call [handleMarketDesync] if the changed value is not the same as [cachedMarket].
     * Should also set [currentSatelliteFactionId] to either this market's factionId if it isn't null and if it isnt a
     * planterary condition market, or [defaultSatelliteFactionId] if either of those are true. */
    var market: MarketAPI? = market
        set(value) {
            field = value
            if (field !== cachedMarket) handleMarketDesync()
            //the below is fine; SS is not multithreaded so this wont break. yet? i wish i could do this in a threadsafe way easily
            currentSatelliteFactionId = (if (market != null && !(market!!.isPlanetConditionMarketOnly)) market!!.factionId else defaultSatelliteFactionId)
        }
    /** Should only be set in [handleMarketDesync] or it's called funcs. Should cause [handleMarketDesync] to be called
     * if, apon [market] set(), [market] is not the same as this. */
    var cachedMarket: MarketAPI? = this.market
    /** Due to kotlin stuff, this can't be made abstract due to [currentSatelliteFactionId] being set to it on init.
     *  The factionId of the faction that this handler will be set to upon creation, and the faction id that it will use
     *  when it's [getPrimaryHolder] is uncolonized.*/
    val defaultSatelliteFactionId: String = Factions.DERELICT //todo: make abstract as a test to see if it npes?
    /** Defaults to [defaultSatelliteFactionId]. If [getPrimaryHolder] is uncolonized, this is set to [defaultSatelliteFactionId].
     *  Applied to satellite fleets post-creation, and cosmetic satellites on creation.
     *  This set() calls [updateSatelliteFactions] if the provided value is not the same as this's value.*/
    var currentSatelliteFactionId: String = defaultSatelliteFactionId
        set(value) {
            if (field != value) updateSatelliteFactions(value)
            // no !== because if we use that, "blah" !== "blah" return false, while
            // "blah" != "blah" returns true due to contents being the same
            field = value
        }
    /** The ID of the dummy faction used to construct our fleets. The faction should not ever be viewable by the player.
     *  Whenever a satellite fleet is created, this should be the faction provided to the [FleetParamsV3] instance.*/
    abstract val satelliteConstructionFactionId: String

    /** Should be a custom entity ID, as used in customEntities.json. The ID of the custom entity we will use for our
     * cosmetic satellites.*/
    abstract val cosmeticSatelliteId: String?

    /** [HashMap] of Fleet -> Float. While Float is > 0, any calls of [wantToFightFleet] will fail. Decremented by
     * [gracePeriodDecrementer] every advance call by the amount of time since last frame, in seconds.
     * A value of Fleet -> 15 will mean the fleet has 15 seconds of immunity to the satellites. */
    val gracePeriods = HashMap<CampaignFleetAPI, Float>()
    /** Should contain, exclusively, the customCampaignEntityAPI satellites orbitting our [getPrimaryHolder].*/
    val cosmeticSatellites: MutableList<CustomCampaignEntityAPI> = ArrayList()
    /** Should contain, exclusively, the satellitefleets we are currently using for combat. Excludes [dummyFleetForConditionalLogic].
     * TODO: what about [satelliteFleetForPlayerDialog]?*/
    val satelliteFleets: MutableList<CampaignFleetAPI> = ArrayList()
    /** TODO: DO i need this? see [createNewSatelliteFleet] for why we might not need it*/
    abstract val satelliteFleetName: String
    /** Very important. The absolute maximum FP a handler can generate for a satellite fleet. Any ships that push this
     * limit will be culled through [trimDownToFP]. Should be beholded to [niko_MPC_settings.BATTLE_SATELLITES_BASE], [niko_MPC_settings.BATTLE_SATELLITES_MULT]*/
    abstract val maximumSatelliteFleetFp: Float
    /** The absolute maximum cosmetic satellites that can be in orbit of [getPrimaryHolder]. By default, is proportional
     * to the radius of [getPrimaryHolder], due to [calculateMaxCosmeticSatellitesForEntity].*/
    var maxCosmeticSatellitesForEntity: Int = calculateMaxCosmeticSatellitesForEntity()
    /** A satellite fleet used for conditional logic, such as [wantToFightFleet]. If null, creates a new dummy fleet
     * through it's get(), using [createNewDummyFleet] in a param of [spawnGenericSatelliteFleet]. This exists because
     * there's no real reason to create a new satellite fleet every time we need to perform some conditional, comparative
     * logic. We can just use one with no AI. Excluded from [satelliteFleets].*/
    var dummyFleetForConditionalLogic: CampaignFleetAPI? = null
        get() {
            if (field == null) field = spawnGenericSatelliteFleet(createNewDummyFleet())
            return field
        }
    /** Come back to this. Theres a reason I made it. */
    var satelliteFleetForPlayerDialog: CampaignFleetAPI? = TODO()
    /** Creates a new satellite fleet, and calls [setDummyFleet].(true) on it, disabling it's AI and setting a memflag,
     * as well as setting [dummyFleetForConditionalLogic] to it. */
    protected fun createNewDummyFleet(): CampaignFleetAPI {
        val dummyFleet: CampaignFleetAPI = createNewSatelliteFleet()
        dummyFleet.setDummyFleet(true)

        this.dummyFleetForConditionalLogic = dummyFleet
        return dummyFleet
    }
    /** The distance, in "starsector units", that cosmetic satellites will be away from the [entity] they orbit. Careful to
     * make it flat, and not relative-[entity].getRadius()*yourValue will cause satellites to have excessively tight orbits
     * on small worlds, and huge orbits on large worlds.*/
    abstract var satelliteOrbitDistance: Float
    /** Added to [satelliteOrbitDistance] in [satelliteInterferenceDistance] get() before settings modifiers. */
    open val satelliteInterferenceDistanceMod = 0f
    /** Should be relative to [satelliteOrbitDistance]. Defaults to 0-it's get() uses [niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_BASE]
     * and [niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_MULT], plus [satelliteInterferenceDistanceMod], to determine the "starsector" units
     * away from [getPrimaryHolder] that this handler can interfere in battles and attack fleets for.*/
    var satelliteInterferenceDistance: Float
        get() {
            val interferenceDistance = (((satelliteOrbitDistance + satelliteInterferenceDistanceMod) + niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_BASE)*niko_MPC_settings.SATELLITE_INTERFERENCE_DISTANCE_MULT)
            if (field != interferenceDistance) field = interferenceDistance
            return field
        }
    /** Passed to [SectorEntityToken.setCircularOrbitPointingDown]'s "orbitDays" arg on [setupSatellite].*/
    val satelliteOrbitDays: Float = 15f

    //see the class's documentation
    lateinit var gracePeriodDecrementer: niko_MPC_gracePeriodDecrementer
    lateinit var proximityChecker: niko_MPC_satelliteFleetProximityChecker
    lateinit var stationBattleChecker: niko_MPC_satelliteStationBattleChecker

    protected open fun createNewProximityChecker(): niko_MPC_satelliteFleetProximityChecker {
        return niko_MPC_satelliteFleetProximityChecker(this)
    }

    protected open fun createNewDecrementorScript(): niko_MPC_gracePeriodDecrementer {
        return niko_MPC_gracePeriodDecrementer(this)
    }

    protected open fun createNewStationBattleChecker(): niko_MPC_satelliteStationBattleChecker {
        return niko_MPC_satelliteStationBattleChecker(this)
    }

    /** THIS METHOD IS WHY YOU USE THE HANDLER CREATOR. Runs non-final methods that would be in the constructor,
     *  if not for the fact that open funcs in constructors are dangerous. */
    protected open fun postConstructInit() {
        getAllSatelliteHandlers().add(this)
        log.info("$this added to global satellite handler list due to postconstructinit")

        gracePeriodDecrementer = createNewDecrementorScript()
        proximityChecker = createNewProximityChecker()
        stationBattleChecker = createNewStationBattleChecker()

        startScripts()
    }

    /** Should run [niko_MPC_baseNikoScript.start] for each script we have in a variable. */
    protected open fun startScripts() {
        gracePeriodDecrementer.start()
        proximityChecker.start()
        stationBattleChecker.start()
    }

    /** Should run [niko_MPC_baseNikoScript.delete] for each script we have in a variable. */
    protected open fun deleteScripts() {
        gracePeriodDecrementer.delete()
        proximityChecker.delete()
        stationBattleChecker.delete()
    }

    protected fun createNewCosmeticSatellites(amountToAdd: Int) {
        if (entity == null) return
        var index = amountToAdd
        while (index-- > 0 && cosmeticSatellites.size < maxCosmeticSatellitesForEntity) {
            instantiateNewCosmeticSatellite()
        }
        regenerateOrbitSpacing()
    }

    fun instantiateNewCosmeticSatellite(): CustomCampaignEntityAPI? {
        if (entity == null) return null
        val cosmeticSatellite: CustomCampaignEntityAPI = createNewCosmeticSatellite() ?: return null
        return setupSatellite(cosmeticSatellite)
    }

    open fun setupSatellite(cosmeticSatellite: CustomCampaignEntityAPI, regenerateOrbit: Boolean = false): CustomCampaignEntityAPI? {
        if (entity == null) return null

        cosmeticSatellite.applyCosmeticSatelliteOrbit(entity, 0f)

        if (regenerateOrbit) regenerateOrbitSpacing()
        return cosmeticSatellite
    }

    /** Literally will never make anything if entity is null. */
    open fun createNewCosmeticSatellite(): CustomCampaignEntityAPI? {
        if (entity == null) return null
        val ourLocation = getLocation() ?: return null
        val cosmeticSatellite: CustomCampaignEntityAPI = ourLocation.addCustomEntity(
            null, null, cosmeticSatelliteId, currentSatelliteFactionId)

        return cosmeticSatellite
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
    }

    /** Returns a combined list of [satelliteFleets] and [dummyFleetForConditionalLogic], but only if
     * [dummyFleetForConditionalLogic] is not null. In that case, it returns [satelliteFleets] exclusively.
     * Concurrency-safe.]*/
    fun getAllSatelliteFleets(): List<CampaignFleetAPI> {
        if (dummyFleetForConditionalLogic != null) {
            return (satelliteFleets + dummyFleetForConditionalLogic!!)
        }
        return ArrayList(satelliteFleets)
    }

    protected open fun getOptimalOrbitalOffsetForSatellites(): Float {
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
    fun regenerateOrbitSpacing() {
        val optimalOrbitAngleOffset = getOptimalOrbitalOffsetForSatellites()
        var orbitAngle = 0f
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc,
        // so its safe to not add a buffer to the calculation in the optimalangle method
        var errored = false
        for (cosmeticSatellite: CustomCampaignEntityAPI in ArrayList(cosmeticSatellites)) {
            if (orbitAngle >= 360) {
                if (!errored)  {
                    displayError("regenerateOrbitSpacing orbitAngle = $orbitAngle is above or equal to 360")
                    errored = true
                }
                cosmeticSatellite.deleteIfCosmeticSatellite()
            }
            cosmeticSatellite.applyCosmeticSatelliteOrbit(entity, orbitAngle)
            orbitAngle += optimalOrbitAngleOffset //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    fun CustomCampaignEntityAPI.applyCosmeticSatelliteOrbit(entity: SectorEntityToken?, orbitAngle: Float) {
        if (!isCosmeticSatellite()) return
        setCircularOrbitPointingDown(entity, orbitAngle, satelliteOrbitDistance, satelliteOrbitDays)
        if (orbit == null) displayError("$this has no orbit after applyCosmeticSatelliteOrbit", logType = Level.WARN)
    }

    protected fun calculateMaxCosmeticSatellitesForEntity(radiusDivisor: Float = 5f): Int {
        if (entity == null || entity!!.radius == 0f) return 0;
        val calculatedValue = niko_MPC_mathUtils.ensureIsJsonValidFloat(((entity!!.radius) / radiusDivisor).toDouble())
        return calculatedValue.roundToInt();
        // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    /** Should always ONLY create a new satellite fleet, should not add it or anything. Will be used to create
     * dummy fleets, so keep that in mind. */
    fun createNewSatelliteFleet(): CampaignFleetAPI {
        val constructionFaction = Global.getSector().getFaction(satelliteConstructionFactionId) // this can crash for all i care, isnt a big deal
        val quality: Float = constructionFaction.doctrine.shipQualityContribution
        val fleetParams = FleetParamsV3(null, null,
            satelliteConstructionFactionId,
            quality,
            FleetTypes.PATROL_SMALL, //todo come back to this
            maximumSatelliteFleetFp,
            0f, 0f, 0f, 0f, 0f, 0f)

        val satelliteFleet: CampaignFleetAPI = FleetFactoryV3.createFleet(fleetParams) //step 1: make the fleet, filled with ships
        setSatelliteFleetName(satelliteFleet) //todo: maybe not do this? i dunno.
        satelliteFleet.setFaction(currentSatelliteFactionId, true) // step 3: override the faction to be ours, and not the constructor
        satelliteFleet.trimDownToFP(maximumSatelliteFleetFp) // step 4: remove excess ships
        satelliteFleet.setSatelliteFleet(true) // step 5: mark as a satellite fleet
        satelliteFleet.setSatelliteEntityHandler(this) // step 6: associate it with us
        addDespawnListener(satelliteFleet) // step 7: add a listener that will clear important stuff and tell us when it dies
        addModifiersToNewlyCreatedFleet(satelliteFleet) // step 8: set custom, overriddable memflags
        satelliteFleet.ai = createSatelliteFleetAI() // step 9: set the ai so it's less cowardly
        assignCommanderToSatelliteFleet(satelliteFleet)

        return satelliteFleet
    }

    abstract fun assignCommanderToSatelliteFleet(satelliteFleet: CampaignFleetAPI): PersonAPI?

    protected open fun setSatelliteFleetName(satelliteFleet: CampaignFleetAPI) {
        satelliteFleet.name = satelliteFleetName
    }

    abstract fun createSatelliteFleetAI(): CampaignFleetAIAPI

    protected open fun addModifiersToNewlyCreatedFleet(satelliteFleet: CampaignFleetAPI) {
        val fleetMemory: MemoryAPI = satelliteFleet.getMemoryWithoutUpdate()
        fleetMemory[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true
        fleetMemory[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true
    }

    /** DO NOT USE TO SPAWN NORMAL SATELLITE FLEETS. USE [spawnSatelliteFleet] INSTEAD. ONLY USE TO SPAWN THE DUMMY FLEET. */
    protected fun spawnGenericSatelliteFleet(satelliteFleet: CampaignFleetAPI): CampaignFleetAPI {
        val ourLocation = getLocation() ?: return satelliteFleet
        ourLocation.addEntity(satelliteFleet)
        addDespawnScript(satelliteFleet)

        return satelliteFleet
    }

    fun spawnSatelliteFleet(satelliteFleet: CampaignFleetAPI): CampaignFleetAPI {
        spawnGenericSatelliteFleet(satelliteFleet)
        satelliteFleets.add(satelliteFleet)
        return satelliteFleet
    }

    protected open fun addDespawnScript(satelliteFleet: CampaignFleetAPI) {
        satelliteFleet.addScript(createNewSatelliteFleetDespawnScript(satelliteFleet, this))
    }

    protected open fun createNewSatelliteFleetDespawnScript(fleet: CampaignFleetAPI, handler: niko_MPC_satelliteHandlerCore): niko_MPC_temporarySatelliteFleetDespawner {
        return niko_MPC_temporarySatelliteFleetDespawner(fleet, handler)
    }

    protected open fun addDespawnListener(satelliteFleet: CampaignFleetAPI) {
        satelliteFleet.addEventListener(createNewDespawnListener())
    }
    protected open fun createNewDespawnListener(): niko_MPC_satelliteFleetDespawnListener = niko_MPC_satelliteFleetDespawnListener()
    /** Handles situations where we migrated entities, ex. our satellites, or our condition, moved to a new entity, or
     * market with a different entity.*/
    protected fun handleEntityDesync() {
        val oldEntity = cachedEntity
        val currentEntity = entity
        if (currentEntity === oldEntity) {
            displayError("desync attempt: $oldEntity, ${oldEntity?.name} is the same as the provided entity")
            logDataOf(oldEntity)
        }
        if (oldEntity != null) {
            if (oldEntity.hasSatelliteHandler(this)) {
                displayError("Desync check failure-$oldEntity still has $this" + "applied to it")
                logDataOf(oldEntity)
            }
            else migrateEntityFeatures(oldEntity, currentEntity)
        }
        cachedEntity = entity
    }

    /**Assumes the current entity is the entity to migrate things to.*/
    protected fun migrateEntityFeatures(oldEntity: SectorEntityToken?, migrationTarget: SectorEntityToken?) {
        deleteCosmeticSatellites()
        maxCosmeticSatellitesForEntity = calculateMaxCosmeticSatellitesForEntity()
        createNewCosmeticSatellites(maxCosmeticSatellitesForEntity)
        deleteAllFleets()
        createNewDummyFleet()

        TODO() // not done! might want to attach a few more entity-specific variables, like stations
        // (put those on market?)
    }

    /** Handles situations where we migrated markets, ex. our condition moved to a new market.*/
    protected fun handleMarketDesync() {
        val oldMarket = cachedMarket
        val currentMarket = market
        if (currentMarket === oldMarket) {
            displayError("Desync check failure: $oldMarket, ${oldMarket?.name} is the same as the provided market")
            logDataOf(market)
            return
        }
        if (oldMarket != null) {
            if (oldMarket.hasSatelliteHandler(this)) {
                displayError("Desync check failure-$oldMarket still has $this" + "applied to it")
                logDataOf(oldMarket)
            } else currentMarket?.let { migrateMarketFeatures(oldMarket, it) }

        }
        cachedMarket = market
    }

    protected fun migrateMarketFeatures(oldMarket: MarketAPI?, migrationTarget: MarketAPI?) {
        TODO("Not yet implemented")
    }

    fun tryToJoinBattle(battle: BattleAPI?): Boolean {
        if (battle == null || !this.wantToJoinBattle(battle)) return false

        joinBattle(battle)
        return true
    }

    fun wantToJoinBattle(battle: BattleAPI?): Boolean {
        if (battle == null) return false
        return true
    }

    protected fun joinBattle(battle: BattleAPI) {
        TODO("Not yet implemented")
    }

    fun tryToEngageFleet(fleet: CampaignFleetAPI): Boolean {
        if (isFleetValidEngagementTarget(fleet)) return false
        engageFleet(fleet)
        return true
    }

    fun isFleetValidEngagementTarget(fleet: CampaignFleetAPI?): Boolean {
        if (fleet == null || fleet.isSatelliteFleet()) return false
        val tracker: niko_MPC_satelliteBattleTracker = getSatelliteBattleTracker()

        return true
    }

    protected fun engageFleet(fleet: CampaignFleetAPI): BattleAPI {

    }

    fun adjustGracePeriod(fleet: CampaignFleetAPI, value: Float) {
        if (gracePeriods[fleet] == null) {
            gracePeriods -= fleet
            return
        }
        gracePeriods[fleet] = (gracePeriods[fleet]!! - value).coerceAtLeast((0f))
    }

    open fun delete() {
        if (isDeletedWrapper()) {
            displayError("$this deleted multiple times")
            niko_MPC_debugUtils.logDataOf(this)
        }
        deleted = true

        deleteAllFleets()
        deleteAllCosmeticSatellites()

        unassociateWithEntity()
        unassociateWithMarket()
        unassociateWithCondition()

        getAllSatelliteHandlers().remove(this)

        deleteScripts()
        TODO("Not yet implemented")
    }

    fun deleteAllFleets() {
        for (fleet: CampaignFleetAPI in ArrayList(getAllSatelliteFleets())) fleet.satelliteFleetDespawn()
    }

    open fun doSatelliteFleetPostDeletionGCPrep(satelliteFleet: CampaignFleetAPI) {
        satelliteFleets.remove(satelliteFleet)
        if (satelliteFleet.isDummyFleet()) dummyFleetForConditionalLogic = null
    }

    protected fun deleteAllCosmeticSatellites() {
        for (cosmeticSatellite: CustomCampaignEntityAPI in ArrayList(cosmeticSatellites)) {
            if (!cosmeticSatellite.isCosmeticSatellite())  {
                displayError("$cosmeticSatellite is" + "not a cosmetic satellite but managed to get into " +
                            "$this $cosmeticSatellite list")
                cosmeticSatellites -= cosmeticSatellite
            }
            cosmeticSatellite.deleteIfCosmeticSatellite()
        }
    }

    protected open fun unassociateWithEntity() {
        if (entity == null) return
        TODO("Not yet implemented")
    }

    protected open fun unassociateWithMarket() {
        if (market == null) return
        if (condition != null && market!!.hasSpecificCondition(condition!!.getCondition().idForPluginModifications)) {
            condition!!.handler = null
            market!!.removeSpecificCondition(condition!!.getCondition().idForPluginModifications)
        }
        market = null
    }

    protected open fun unassociateWithCondition() {
        if (condition == null) return
        condition!!.handler = null // do it before so we dont trigger the unapply script
        if (market != null && market!!.hasSpecificCondition(condition!!.getCondition().idForPluginModifications)) {
            market!!.removeSpecificCondition(condition!!.getCondition().idForPluginModifications)
        }
        condition = null
    }

    open fun isDeletedWrapper(): Boolean {
        return deleted
    }

    /** Prioritizes entity.containingLocation over market.containingLocation.*/
    open fun getLocation(): LocationAPI? {
        val locationToReturn: LocationAPI? = when {
            (entity != null) -> entity!!.containingLocation
            (market != null) -> market!!.containingLocation
            else -> {
                handleMarketAndEntityNull()
                return null
            }
        }
        return locationToReturn
    }

    fun getPrimaryHolder(): HasMemory? {
        val primaryHolder: HasMemory? = when {
            (entity != null) -> entity!!
            (market != null) -> market!!
            else -> {
                handleMarketAndEntityNull()
                null
            }
        }
        return primaryHolder
    }

    protected fun handleMarketAndEntityNull() {
        displayError("Market and entity null on $this, deleting", true)
        delete()
    }

    override fun provideLoggableData(): List<String> {
        return arrayListOf("$this.", "Entity: $entity, ${entity?.name}, location: ${entity?.containingLocation}, ${entity?.containingLocation?.name}",
                            "Market: $market, ${market?.name}, market faction: ${market?.factionId}, market location: ${market?.containingLocation}, ${market?.containingLocation?.name}",
                            "$this variables: Faction: $currentSatelliteFactionId, Deleted: $deleted",
                            "Cached market: $cachedMarket, ${cachedMarket?.name}",
                            "Cached entity: $cachedEntity, ${cachedEntity?.name}")
    }
}

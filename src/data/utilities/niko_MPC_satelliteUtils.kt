package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.misc.niko_MPC_satelliteHandler
import data.scripts.campaign.misc.niko_MPC_satelliteHandler.niko_MPC_satelliteParams
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer
import org.apache.log4j.Level
import org.jetbrains.annotations.Contract
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object niko_MPC_satelliteUtils {
    private val log = Global.getLogger(niko_MPC_satelliteUtils::class.java)

    init {
        log.level = Level.ALL
    }

    /**
     * If the tracker is null, creates a new one.
     * @return The savefile specific instance of the battle tracker.
     */
    @JvmStatic
    fun getSatelliteBattleTracker(): niko_MPC_satelliteBattleTracker {
        var tracker: niko_MPC_satelliteBattleTracker? = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.satelliteBattleTrackerId] as niko_MPC_satelliteBattleTracker
        if (tracker == null) {
            tracker = niko_MPC_memoryUtils.createNewSatelliteTracker()
        }
        return tracker
    }

    //////////////////////////
    //                      //
    // ADDITION AND REMOVAL //
    //                      //
    //////////////////////////
    fun initializeSatellitesOntoEntity(entity: SectorEntityToken, handler: niko_MPC_satelliteHandler?) {
        initializeSatellitesOntoEntity(entity, entity.market, handler)
    }

    /**
     * This is what should be called the FIRST TIME an entity gains satellites, or after satellites have been entirely removed.
     * @param entity The entity to add markets to.
     * @param market Will have satelliteMarketId set to this if not null.
     */
    @JvmStatic
    fun initializeSatellitesOntoEntity(entity: SectorEntityToken?, market: MarketAPI?, handler: niko_MPC_satelliteHandler?) {
        if (!niko_MPC_debugUtils.doEntityHasNoSatellitesTest(entity)) { //if the test fails, something fucked up, lets abort
            return
        }
        if (entity == null) return
        val entityMemory = entity.memoryWithoutUpdate
        if (market != null) {
            entityMemory.set(niko_MPC_ids.satelliteMarketId, market) // we're already protected from overwriting satellites with the above test
        }
        entityMemory[niko_MPC_ids.satelliteHandlerId] = handler //store our parameters onto the entity
        //LocationAPI containingLocation = entity.getContainingLocation();
        /*SectorEntityToken barrageTerrain = containingLocation.addTerrain(satelliteBarrageTerrainId, new niko_MPC_defenseSatelliteBarrageTerrainPlugin.barrageAreahandler(
            handler.satelliteBarrageDistance,
            "Bombardment zone",
            entity

        ));
        handler.satelliteBarrages.add(barrageTerrain);
        barrageTerrain.setCircularOrbit(entity, 0, 0, 100); */
        addSatellitesUpToMax(entity)
    }

    /**
     * Adds a new CustomCampaignEntity satellite of type id to entity and sets up an orbit around it.
     * @param entity The entity for the satellite to orbit around.
     * @param regenerateOrbit If true, repositions all satellites in orbit with the same ratio
     * of distance to eachother.
     * @param id The Id of the satellite to add.
     * @param factionId The faction id to set as the satellite's faction.
     */
    fun addSatellite(entity: SectorEntityToken, regenerateOrbit: Boolean, id: String?, factionId: String?) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.addSatellite(regenerateOrbit, id, factionId)
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to market through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param entity The entity to add the satellites to.
     * @param amountOfSatellitesToAdd The amount of satellites.
     */
    fun addSatellitesToEntity(entity: SectorEntityToken, amountOfSatellitesToAdd: Int) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        addSatellitesToEntity(
            entity,
            amountOfSatellitesToAdd,
            handler!!.getParams().satelliteId,
            handler.getParams().satelliteFactionId
        )
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to entity through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id                      The id to be assigned to the satellites.
     * @param faction                 The factionid to be given to the satellites.
     */
    fun addSatellitesToEntity(entity: SectorEntityToken, amountOfSatellitesToAdd: Int, id: String?, faction: String?) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.addSatellitesToEntity(amountOfSatellitesToAdd, id, faction)
    }

    // all this method should do is call addsatellites with the max shit
    fun addSatellitesUpToMax(entity: SectorEntityToken) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.addSatellitesUpToMax()
    }

    /**
     * Not only removes all satellites from entity, but also removes all objects and entities associated
     * with the existence of the satellites. This should only be used on removal of EVERYTHING.
     * @param entity
     */
    @JvmStatic
    fun purgeSatellitesFromEntity(entity: SectorEntityToken) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val entityMemory = entity.memoryWithoutUpdate
        removeSatellitesFromEntity(entity)
        niko_MPC_memoryUtils.deleteMemoryKey(entityMemory, niko_MPC_ids.satelliteMarketId)
        val handler = getSatelliteHandlerOfEntity(entity)
        for (terrain in ArrayList(handler!!.satelliteBarrages)) {
            removeSatelliteBarrageTerrain(entity, terrain)
        }
        for (satelliteFleet in ArrayList(handler.satelliteFleets)) { //avoid concurrentmod{
            niko_MPC_fleetUtils.despawnSatelliteFleet(satelliteFleet)
        }
        handler.prepareForGarbageCollection()
        niko_MPC_memoryUtils.deleteMemoryKey(entityMemory, niko_MPC_ids.satelliteHandlerId)
    }

    @JvmStatic
    fun getMaxBattleSatellites(primaryEntity: SectorEntityToken): Int {
        val handler = getSatelliteHandlerOfEntity(primaryEntity)
        return handler?.maxBattleSatellites
            ?: (niko_MPC_settings.BATTLE_SATELLITES_BASE * niko_MPC_settings.BATTLE_SATELLITES_MULT).toInt()
    }

    /**
     * Removes all satellites orbiting this entity.
     *
     * @param entity The target entity.
     */
    fun removeSatellitesFromEntity(entity: SectorEntityToken) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val satellitesInOrbit = getSatellitesInOrbitOfEntity(entity)
        removeSatellitesFromEntity(entity, satellitesInOrbit.size)
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from entity's orbit. Will end execution early if the list becomes empty.
     *
     * @param entity                     The entity to remove the satellite from.
     * @param amountOfSatellitesToRemove The amount of satellites to remove from entity.
     */
    fun removeSatellitesFromEntity(entity: SectorEntityToken, amountOfSatellitesToRemove: Int) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.removeSatellitesFromEntity(amountOfSatellitesToRemove)
    }

    /**
     * Removes a specific satellite from an entity.
     * @param entity The entity to remove the satellite from.
     * @param satellite The satellite to remove.
     * @param regenerateOrbit If true, satellite orbit will be regenerated.
     */
    fun removeSatelliteFromEntity(
        entity: SectorEntityToken,
        satellite: CustomCampaignEntityAPI?,
        regenerateOrbit: Boolean
    ) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.removeSatellite(satellite, regenerateOrbit, true)
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     *
     * @param satellite The satellite to remove.
     */
    fun removeSatellite(satellite: CustomCampaignEntityAPI) {
        val handler : niko_MPC_satelliteHandler? = satellite.getSatelliteHandler()
        if (handler != null) {
            handler.removeSatellite(satellite)
        }
        else {
            Misc.fadeAndExpire(satellite)
            satellite.containingLocation.removeEntity(satellite)
            niko_MPC_memoryUtils.deleteMemoryKey(satellite.memoryWithoutUpdate, niko_MPC_ids.satelliteHandlerIdAlt)
        }
    }

    /**
     * Places all satellites in orbit around the given entity, ensuring they are all equally spaced apart from eachother.
     * @param entity The entity to regenerate satellite orbit from.
     */
    fun regenerateOrbitSpacing(entity: SectorEntityToken) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.regenerateOrbitSpacing()
    }

    // MARKETS
    fun marketsDesynced(entity: SectorEntityToken): Boolean {
        return marketsDesynced(entity, entity.market)
    }

    /**
     * @param entity The entity to check.
     * @param market The market to compare entity's market to.
     * @return True if entity's satellite market != market.
     */
    @JvmStatic
    fun marketsDesynced(entity: SectorEntityToken, market: MarketAPI): Boolean {
        return memorySatellitesDesyncedWithMarket(entity.memoryWithoutUpdate, market)
    }

    /**
     * @param memory The memoryAPI to check.
     * @param market The market to compare memory's market to.
     * @return True if memory's satellite market != market.
     */
    fun memorySatellitesDesyncedWithMarket(memory: MemoryAPI, market: MarketAPI): Boolean {
        return getMemorySatelliteMarket(memory) !== market
    }

    /**
     * Sets entity's satellitemarket to market.
     */
    @JvmStatic
    fun syncMarket(entity: SectorEntityToken, market: MarketAPI?) {
        entity.memoryWithoutUpdate[niko_MPC_ids.satelliteMarketId] = market
    }
    //GETTERS AND SETTERS
    /**
     * @return Either null, or an instance of niko_MPC_satelliteHandler.
     */
    // no nullable, i use a method to nullcheck which intellij gets confused by
    @JvmStatic
    fun getSatelliteHandlerOfEntity(entity: SectorEntityToken): niko_MPC_satelliteHandler? {
        return entity.memoryWithoutUpdate[niko_MPC_ids.satelliteHandlerId] as niko_MPC_satelliteHandler?
    }

    fun HasMemory.getSatelliteHandler(): niko_MPC_satelliteHandler? {
        return memoryWithoutUpdate[niko_MPC_ids.satelliteHandlerId] as niko_MPC_satelliteHandler?
    }

    @JvmStatic
    fun getEntitySatelliteHandlerAlternate(entity: SectorEntityToken): niko_MPC_satelliteHandler? {
        return entity.memoryWithoutUpdate[niko_MPC_ids.satelliteHandlerIdAlt] as niko_MPC_satelliteHandler?
    }

    /**
     * @return Either null, or an instance of niko_MPC_satellitehandler.
     */
    fun getEntitySatelliteParams(entity: SectorEntityToken): niko_MPC_satelliteParams? {
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler?.getParams()
    }

    /**
     * Returns the "satellite market" of an entity, or the market that holds the condition. Can be null.
     * @param entity The entity to check.
     * @return The satellite market of the entity.
     */
    @JvmStatic
    fun getEntitySatelliteMarket(entity: SectorEntityToken): MarketAPI? {
        return getMemorySatelliteMarket(entity.memoryWithoutUpdate)
    }

    fun getMemorySatelliteMarket(memory: MemoryAPI): MarketAPI? {
        return memory[niko_MPC_ids.satelliteMarketId] as MarketAPI
    }

    /**
     * Gets a list of satellites in orbit around entity, using entity's satellite handler.
     * @param entity The entity of which the satellites are orbitting.
     * @return A arraylist of satellite in orbit around the entity. Can return an empty list.
     */
    fun getSatellitesInOrbitOfEntity(entity: SectorEntityToken): List<CustomCampaignEntityAPI> {
        //does not call ensureSatellites because this is intended to be called on things w/o satellites
        val handler = getSatelliteHandlerOfEntity(entity)
        return if (handler != null && handler.satellites != null) {
            handler.satellites
        } else ArrayList()
    }

    fun getCurrentSatelliteFactionId(handler: niko_MPC_satelliteHandler): String? {
        return getCurrentSatelliteFactionId(handler.entity)
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the entity's faction id whenever it's ran.
     * @param entity The entity to get the handler from.
     * @return A faction ID, in string form. Can return null if entity has no satellites.
     */
    @JvmStatic
    fun getCurrentSatelliteFactionId(entity: SectorEntityToken): String? {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.currentSatelliteFactionId
    }

    fun getCurrentSatelliteFaction(handler: niko_MPC_satelliteHandler): FactionAPI {
        return Global.getSector().getFaction(getCurrentSatelliteFactionId(handler))
    }

    /**
     * Generates an offset with which satellites in orbit of an entity will be spaced apart by.
     * Is based on the amount of satellites in the given list.
     * @param satellitesInOrbitOfEntity The list of satellites to use. Should be a complete list of satellites
     * in orbit of an entity.
     * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
     */
    @Contract(pure = true)
    fun getOptimalOrbitalOffsetForSatellites(satellitesInOrbitOfEntity: List<CustomCampaignEntityAPI?>): Float {
        val numOfSatellites = satellitesInOrbitOfEntity.size
        var optimalAngle = 360 / numOfSatellites.toFloat()
        // 1 satellite = offset of 360, so none. 2 satellites = offset or 180, so they are on opposite ends of the planet.
        // 3 satellites = offset of 120, meaning the satellites form a triangle around the entity. Etc.
        if (optimalAngle == 360f) {
            optimalAngle =
                0f //sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net
        }
        return optimalAngle
    }

    /**
     * Generates an offset with which satellites in orbit of an entity will be spaced apart by.
     * Is based on the amount of satellites in orbit of the planet.
     * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
     */
    fun getOptimalOrbitalOffsetForSatellites(entity: SectorEntityToken): Float {
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler?.optimalOrbitalOffsetForSatellites
            ?: getOptimalOrbitalOffsetForSatellites(ArrayList())
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    fun getMaxPhysicalSatellitesBasedOnEntitySize(entity: SectorEntityToken): Int {
        return getMaxPhysicalSatellitesBasedOnEntitySize(entity, 5f)
    }

    /**
     * Divides entity.getRadius() from radiusDivisor and returns the result.
     */
    fun getMaxPhysicalSatellitesBasedOnEntitySize(entity: SectorEntityToken, radiusDivisor: Float): Int {
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler?.getMaxPhysicalSatellitesBasedOnEntitySize(radiusDivisor)
            ?: Math.round(entity.radius / radiusDivisor)
        // divide the radius of the entity by 5, then round it up or down to the nearest whole number;
    }
    // GETTING ENTITIES AND CHECKING CAPABILITIES
    /**
     * @param location The location to scan.
     * @return A new set containing every entity that had defenseSatellitesApplied(entity) return true.
     */
    fun getEntitiesInLocationWithSatellites(location: LocationAPI): MutableSet<SectorEntityToken> {
        val entitiesWithSatellites: MutableSet<SectorEntityToken> = HashSet()
        for (entity in location.allEntities) {
            if (entity is CampaignFleetAPI) {
                if (niko_MPC_fleetUtils.fleetIsSatelliteFleet(entity)) continue  // if we dont do this, we create an infinite loop of
                // spawning fleets to check the f leets which then check those fleets which then check those fleets
                // does not account for the chance satellites have satellites themselves. if i ever do that, ill make a memkey for it or smthn
            }
            if (defenseSatellitesApplied(entity)) {
                entitiesWithSatellites.add(entity)
            }
        }
        return entitiesWithSatellites
    }

    fun getNearbyEntitiesWithSatellites(entity: SectorEntityToken, location: LocationAPI): Set<SectorEntityToken> {
        return getNearbyEntitiesWithSatellites(entity.location, location)
    }

    /**
     * First gets a list of entities in location with satellites, then does a range check using the entity's handler' satelliteInterferenceDistance variable.
     * @param coordinates The coordinates to compare the entity to.
     * @param location The location to scan.
     * @return A set containing every entity with satellites that has the coordinates within their interference distance.
     */
    @JvmStatic
    fun getNearbyEntitiesWithSatellites(coordinates: Vector2f?, location: LocationAPI): MutableSet<SectorEntityToken> {
        val entitiesWithSatellites = getEntitiesInLocationWithSatellites(location)
        val iterator = entitiesWithSatellites.iterator()

        // a ensureSatellites check is not needed as the set only has entities with handler
        while (iterator.hasNext()) {
            val entity = iterator.next()
            val handler = getSatelliteHandlerOfEntity(entity) //we can use this here because the previously used method only returns things with handler
            if (!MathUtils.isWithinRange(entity, coordinates, handler!!.satelliteInterferenceDistance)) {
                iterator.remove() //have to remove because we're using a full list already
            }
        }
        return entitiesWithSatellites
    }

    fun getEntitiesWithSatellitesCapableOfFighting(entities: Set<SectorEntityToken>): Set<SectorEntityToken> {
        return entities //todo: unfinished
    }

    /**
     * @param fleet The fleet to check.
     * @return A list of entities with satellites that are willing to fight the fleet, using doEntitySatellitesWantToFight(entity, fleet).
     */
    @JvmStatic
    fun getNearbyEntitiesWithSatellitesWillingToFight(fleet: CampaignFleetAPI): Set<SectorEntityToken> {
        val entitiesWithSatellites = getNearbyEntitiesWithSatellites(fleet.location, fleet.containingLocation)
        val iterator = entitiesWithSatellites.iterator()
        while (iterator.hasNext()) {
            val entity = iterator.next()
            if (!doEntitySatellitesWantToFight(entity, fleet)) {
                iterator.remove()
            }
        }
        return entitiesWithSatellites
    }

    /**
     * Spawns a temporary fleet that the battle uses to check the side it would join. If the side is not NO_JOIN, adds the entity
     * to the hashmap, associated with the side it'd join.
     * @param battle The battle to check.
     * @return A hashmap of entities associated with the side of the battle they'd take.
     */
    @JvmStatic
    fun getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle: BattleAPI): HashMap<SectorEntityToken, BattleSide?> {
        val entitiesWillingToFight = HashMap<SectorEntityToken, BattleSide?>()
        val containingLocation = niko_MPC_battleUtils.getContainingLocationOfBattle(battle)
            ?: return entitiesWillingToFight // in truth, if there is no containing location, then there would be no entities in range anyway
        val coordinates = battle.computeCenterOfMass()
        for (entity in getNearbyEntitiesWithSatellites(coordinates, containingLocation)) {
            val battleSide = getSideForSatellites(entity, battle)
            if (isSideValid(battleSide)) { //the entity doesnt want to join if its NO_JOIN
                entitiesWillingToFight[entity] = battleSide
            }
        }
        return entitiesWillingToFight
    }

    /**
     * Uses doEntitySatellitesWantToBlock/Fight and areEntitySatellitesCapableOfFBlocking/Fighting to determine
     * which fleets the satellites would want to fight when spawned.
     * @param entity The entity to get the handler from.
     * @param fleet The first fleet to check.
     * @param fleetTwo The second fleet to check.
     * @param capabilityCheck If true, runs an additional check that skips over a fleet if areEntitySatellitesCapableOfBlocking returns false.
     * @return Null if the satellites want to fight both or neither, otherwise, returns which of the two fleets they're willing to fight.
     */
    @JvmStatic
    fun getSideForSatellitesAgainstFleets(
        entity: SectorEntityToken,
        fleet: CampaignFleetAPI?,
        fleetTwo: CampaignFleetAPI?,
        capabilityCheck: Boolean
    ): CampaignFleetAPI? {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.getSideForSatellitesAgainstFleets(fleet, fleetTwo, capabilityCheck)
    }

    /**
     * Gets all entities within range of entity and gets which fleet they'd fight using getsideforsatellitesagainstfleets.
     * @param entity The entity to get coordinates from.
     * @param fleet The first fleet to check.
     * @param otherFleet The second fleet to check.
     * @return A new HashMap in format (entity -> fleet), where entity is the entity willing to fight, and fleet is which fleet they chose. Cannot return nulls.
     */
    fun getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(
        entity: SectorEntityToken,
        fleet: CampaignFleetAPI?,
        otherFleet: CampaignFleetAPI?
    ): HashMap<SectorEntityToken, CampaignFleetAPI> {
        val entitiesWithSatellites: Set<SectorEntityToken> =
            getNearbyEntitiesWithSatellites(entity.location, entity.containingLocation)
        val sidesTaken = HashMap<SectorEntityToken, CampaignFleetAPI>()
        for (entityWithSatellite in entitiesWithSatellites) {
            val fleetChosen = getSideForSatellitesAgainstFleets(entityWithSatellite, fleet, otherFleet, true)
            if (fleetChosen != null) {
                sidesTaken[entityWithSatellite] = fleetChosen
            }
        }
        return sidesTaken
    }

    /**
     * Gets the side the dummy fleet of entity's handler would enter.
     * @param entity The entity to get the satellites from.
     * @param battle The battle to get the side for.
     * @return The battleside that entity's satellites would pick. Can return null if the entity has no satellites.
     */
    @JvmStatic
    fun getSideForSatellites(entity: SectorEntityToken, battle: BattleAPI?): BattleSide? {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return null
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.getSideForBattle(battle)
    }

    @JvmStatic
    fun isSideValid(side: BattleSide?): Boolean {
        return side != BattleSide.NO_JOIN && side != null
    }

    /**
     * @param battle The battle to check.
     * @return A hashmap of nearby entities with satellites that are both willing and capable of joining battle.
     */
    fun getNearbyEntitiesWithSatellitesWillingAndCapableToJoinBattle(battle: BattleAPI): HashMap<SectorEntityToken, BattleSide?> {
        return getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle)
    }

    /**
     * @param side The side to use.
     * @param entityMap The hashmap of entities to battleside.
     * @return A new arraylist of entities on the given side.
     */
    fun getEntitiesOnSide(
        side: BattleSide,
        entityMap: HashMap<SectorEntityToken, BattleSide>
    ): List<SectorEntityToken> {
        val entitiesOnSide: MutableList<SectorEntityToken> = ArrayList()
        for ((entity, battleSide) in entityMap) {
            if (battleSide == side) {
                entitiesOnSide.add(entity)
            }
        }
        return entitiesOnSide
    }

    fun getNearbyEntitiesWithSatellitesWillingAndCapableToFight(fleet: CampaignFleetAPI): Set<SectorEntityToken> {
        return getEntitiesWithSatellitesCapableOfFighting(getNearbyEntitiesWithSatellitesWillingToFight(fleet))
    }

    // CAPABILITY CHECKING
    fun doEntitySatellitesWantToFight(handler: niko_MPC_satelliteHandler, fleet: CampaignFleetAPI?): Boolean {
        return doEntitySatellitesWantToFight(handler.entity, fleet)
    }

    /**
     * Used for generating battles and autoresolve and such.
     * @param entity The entity to get the satellite handler from.
     * @param fleet The fleet to check.
     * @return True, if the handler' dummy fleet is hostile to the given fleet. False otherwise. Can return null
     * if entity has no satellites.
     */
    @JvmStatic
    fun doEntitySatellitesWantToFight(entity: SectorEntityToken, fleet: CampaignFleetAPI?): Boolean {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.doSatellitesWantToFight(fleet)
    }

    /**
     * Used for things such as preventing the player from interacting with a market.
     * @param entity The entity to get the satellite handler from.
     * @param fleet The fleet to check.
     * @return True, if the satellite handler' faction is inhospitable or worse to fleets' faction, if the fleet has no transponder,
     * or if the satellites want to fight.
     * Can return false if handler are null.
     */
    @JvmStatic
    fun doEntitySatellitesWantToBlock(entity: SectorEntityToken, fleet: CampaignFleetAPI?): Boolean {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.doSatellitesWantToBlock(fleet!!)
    }

    /**
     * @param entity The entity to check.
     * @return True if the entity isn't already blocking the fleet, or if entity's satellite handler' grace period is
     * less or equal to 0. False otherwise.
     */
    @JvmStatic
    fun areEntitySatellitesCapableOfBlocking(entity: SectorEntityToken, fleet: CampaignFleetAPI?): Boolean {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.areSatellitesCapableOfBlocking(fleet!!)
    }

    /**
     * Unfinished.
     */
    private fun areEntitySatellitesCapableOfFighting(entity: SectorEntityToken, fleet: CampaignFleetAPI): Boolean {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return false
        val handler = getSatelliteHandlerOfEntity(entity)
        return handler!!.areSatellitesCapableOfFighting(fleet)
    }

    /**
     * Forces the given entity's satellite handler to spawn a full satellite fleet on the target, unless
     * it's already fighting them. Can fail if the entity has no handler.
     * @param entity The entity to get handler from.
     * @param fleet The fleet to check and engage.
     */
    @JvmStatic
    fun makeEntitySatellitesEngageFleet(entity: SectorEntityToken, fleet: CampaignFleetAPI?) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        handler!!.makeEntitySatellitesEngageFleet(fleet!!)
    }

    /**
     * Forces all nearby entities out of getNearbyEntitiesWithSatellitesWillingAndCapableToFight to
     * run makeEntitySatellitesEngageFleet.
     * @param fleet The fleet to force the entities to engage.
     */
    fun makeNearbyEntitySatellitesEngageFleet(fleet: CampaignFleetAPI) {
        for (entity in getNearbyEntitiesWithSatellitesWillingAndCapableToFight(fleet)) {
            makeEntitySatellitesEngageFleet(entity, fleet)
        }
    }
    //MISC
    /**
     * @param entity The entity to check.
     * @return true if getEntitySatellitehandler(entity) is not null.
     */
    @JvmStatic
    fun defenseSatellitesApplied(entity: SectorEntityToken): Boolean {
        return getSatelliteHandlerOfEntity(entity) != null
    }

    @JvmStatic
    @Deprecated("")
    fun removeSatelliteBarrageTerrain(relatedEntity: SectorEntityToken, terrain: SectorEntityToken) {
        val containingLocation = relatedEntity.containingLocation
        terrain.isExpired = true
        containingLocation.removeEntity(terrain)
    }

    /**
     * Runs handler.adjustGracePeriod(fleet, amount). Exists so that we add a new grace decrementer if none exists.
     * @param fleet The fleet to adjust the grace period of.
     * @param amount The amount to adjust the grace by.
     * @param entity The entity to get handler from.
     */
    @JvmStatic
    fun incrementSatelliteGracePeriod(fleet: CampaignFleetAPI?, amount: Float, entity: SectorEntityToken) {
        if (!niko_MPC_debugUtils.assertEntityHasSatellites(entity)) return
        val handler = getSatelliteHandlerOfEntity(entity)
        if (!entity.hasScriptOfClass(niko_MPC_gracePeriodDecrementer::class.java)) {
            val decrementerScript = niko_MPC_gracePeriodDecrementer(handler)
            handler!!.gracePeriodDecrementer = decrementerScript
            niko_MPC_scriptUtils.addScriptsAtValidTime(decrementerScript, entity, true)
        }
        handler!!.adjustGracePeriod(fleet, amount)
    }

    @JvmStatic
    val allDefenseSatellitePlanets: List<SectorEntityToken>
        get() {
            val entitiesWithSatellites: MutableList<SectorEntityToken> = ArrayList()
            val systems = Global.getSector().starSystems
            for (system in systems) {
                for (entity in system.allEntities) {
                    if (defenseSatellitesApplied(entity)) {
                        entitiesWithSatellites.add(entity)
                        continue
                    }
                }
            }
            return entitiesWithSatellites
        }

    @JvmStatic
    fun isCustomEntitySatellite(entity: SectorEntityToken): Boolean {
        return entity.tags.contains(niko_MPC_ids.satelliteTagId)
    }
}
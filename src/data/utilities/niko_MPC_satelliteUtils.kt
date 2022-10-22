package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.memKeyHasIncorrectType
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import org.jetbrains.annotations.Contract
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object niko_MPC_satelliteUtils {

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
    /** DOES NOT INSTANTIATE SATELLITES ONTO THE MARKET. Call [instantiateSatellitesOntoMarket] instead for that. That
     * automatically calls this func if the primaryentity is not null.*/
    fun instantiateSatellitesOntoEntity(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken) {
        entity.getSatelliteHandlers().add(handler)
    }
    /** Should be called when adding a handler to a market. */
    fun instantiateSatellitesOntoMarket(handler: niko_MPC_satelliteHandlerCore, market: MarketAPI) {
        val primaryEntity: SectorEntityToken? = market.primaryEntity
        if (primaryEntity != null) instantiateSatellitesOntoEntity(handler, primaryEntity)
        market.getSatelliteHandlers().add(handler)
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
        niko_MPC_memoryUtils.deleteMemoryKey(entityMemory, niko_MPC_ids.satelliteHandlersId)
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
        val handler : niko_MPC_satelliteHandlerCore? = satellite.getHandlerForCondition()
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

    /**
     * @param entity The entity to check.
     * @param market The market to compare entity's market to.
     * @return True if entity's satellite market != market.
     */
    @JvmStatic
    fun marketsDesynced(market: MarketAPI, entity: SectorEntityToken = market.primaryEntity): Boolean {
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

    fun HasMemory.hasSatelliteHandler(handler: niko_MPC_satelliteHandlerCore): Boolean {
        return getSatelliteHandlers().any { it === handler } ?: return false
    }

    fun HasMemory.getSatelliteHandlers(): MutableSet<niko_MPC_satelliteHandlerCore> {
        if (memKeyHasIncorrectType<MutableSet<niko_MPC_satelliteHandlerCore>>(this, key = niko_MPC_ids.satelliteHandlersId)) {
            memoryWithoutUpdate[niko_MPC_ids.satelliteHandlersId] = HashSet<niko_MPC_satelliteHandlerCore>()
        }
        // null-safe from the above is check
        return (memoryWithoutUpdate[niko_MPC_ids.satelliteHandlersId] as MutableSet<niko_MPC_satelliteHandlerCore>)
    }

    fun HasMemory?.hasSatellites(): Boolean {
        if (this == null) return false
        return (this.getSatelliteHandlers().isNotEmpty())
    }

    fun HasMemory.isSatelliteEntity(): Boolean {
        return (memoryWithoutUpdate.get(niko_MPC_ids.satelliteEntityHandler) != null)
    }

    @JvmStatic
    fun getEntitySatelliteHandlerAlternate(entity: SectorEntityToken): niko_MPC_satelliteHandlerCore? {
        TODO()
        return entity.memoryWithoutUpdate[niko_MPC_ids.satelliteHandlerIdAlt] as niko_MPC_satelliteHandlerCore?
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

    fun getCurrentSatelliteFactionId(handler: niko_MPC_satelliteHandlerCore): String? {
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

    fun getCurrentSatelliteFaction(handler: niko_MPC_satelliteHandlerCore): FactionAPI {
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
    /**
     * @param side The side to use.
     * @param entityMap The hashmap of entities to battleside.
     * @return A new arraylist of entities on the given side.
     */
    fun getEntitiesOnBattleSide(
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
    //MISC
    /**
     * @param entity The entity to check.
     * @return true if getEntitySatellitehandler(entity) is not null.
     */
    @JvmStatic
    fun defenseSatellitesApplied(entity: SectorEntityToken): Boolean {
        return entity.getSatelliteHandlers().isEmpty()
    }

    @JvmStatic
    fun getAllDefenseSatellitePlanets(): List<SectorEntityToken> {
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
    /** If this returns null, the sector doesnt exist yet. */
    fun getAllSatelliteHandlers(): MutableSet<niko_MPC_satelliteHandlerCore> {
        val sector: SectorAPI? = Global.getSector()
        if (sector == null) {
            displayError("Sector not initialized yet, oh fuck i didnt account for this", true)
            return HashSet()
        }
        if (memKeyHasIncorrectType<MutableSet<niko_MPC_satelliteHandlerCore>>(
                sector.memoryWithoutUpdate, niko_MPC_ids.globalSatelliteHandlerListId)) {
            sector.memoryWithoutUpdate[niko_MPC_ids.globalSatelliteHandlerListId] = HashSet<niko_MPC_satelliteHandlerCore>()
        }
        return sector.memoryWithoutUpdate[niko_MPC_ids.globalSatelliteHandlerListId] as MutableSet<niko_MPC_satelliteHandlerCore>
    }

    fun CustomCampaignEntityAPI.deleteIfCosmeticSatellite() {
        if (!isCosmeticSatellite()) return
        val handler: niko_MPC_satelliteHandlerCore? = getSatelliteEntityHandler()
        if (handler != null) {
            handler.cosmeticSatellites.remove(this)
        } else displayError("$this, despite being a cosmetic satellite, had no handler")
        Misc.fadeAndExpire(this) //this causes a removeentity after a bit
        //containingLocation.removeEntity(this)
    }

    fun CustomCampaignEntityAPI.isCosmeticSatellite(): Boolean {
        return (hasTag(niko_MPC_ids.cosmeticSatelliteTagId))
    }
}


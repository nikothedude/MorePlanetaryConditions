package data.utilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.rules.Memory
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_battleUtils.getContainingLocation
import data.utilities.niko_MPC_battleUtils.isSideValid
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_debugUtils.memKeyHasIncorrectType
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

object niko_MPC_satelliteUtils {

    /**
     * If the tracker is null, creates a new one.
     * @return The savefile specific instance of the battle tracker.
     */
    @JvmStatic
    fun getSatelliteBattleTracker(): niko_MPC_satelliteBattleTracker? {
        val sectorMemory: MemoryAPI = Global.getSector()?.memoryWithoutUpdate ?: return null
        var tracker: niko_MPC_satelliteBattleTracker? = sectorMemory[niko_MPC_ids.satelliteBattleTrackerId] as niko_MPC_satelliteBattleTracker
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
    @JvmStatic
    fun instantiateSatellitesOntoEntity(handler: niko_MPC_satelliteHandlerCore, entity: SectorEntityToken) {
        entity.getSatelliteHandlers().add(handler)
    }
    @JvmStatic
    /** Should be called when adding a handler to a market. */
    fun instantiateSatellitesOntoMarket(handler: niko_MPC_satelliteHandlerCore, market: MarketAPI) {
        val primaryEntity: SectorEntityToken? = market.primaryEntity
        if (primaryEntity != null) instantiateSatellitesOntoEntity(handler, primaryEntity)
        market.getSatelliteHandlers().add(handler)
    }

    //GETTERS AND SETTERS

    @JvmStatic
    fun HasMemory.hasSatelliteHandler(handler: niko_MPC_satelliteHandlerCore): Boolean {
        return getSatelliteHandlers().any { it === handler }
    }

    @JvmStatic
    fun HasMemory.getSatelliteHandlers(): MutableSet<niko_MPC_satelliteHandlerCore> {
        if (memKeyHasIncorrectType<MutableSet<niko_MPC_satelliteHandlerCore>>(this, key = niko_MPC_ids.satelliteHandlersId)) {
            memoryWithoutUpdate[niko_MPC_ids.satelliteHandlersId] = HashSet<niko_MPC_satelliteHandlerCore>()
        }
        // null-safe from the above is check
        return (memoryWithoutUpdate[niko_MPC_ids.satelliteHandlersId] as MutableSet<niko_MPC_satelliteHandlerCore>)
    }

    @JvmStatic
    fun obliterateSatellites() {
        for (handler: niko_MPC_satelliteHandlerCore in ArrayList(getAllSatelliteHandlers())) {
            handler.delete()
        }
    }

    @JvmStatic
    fun HasMemory?.hasSatellites(): Boolean {
        if (this == null) return false
        return (this.getSatelliteHandlers().isNotEmpty())
    }

    @JvmStatic
    fun HasMemory.isSatelliteEntity(): Boolean {
        return (memoryWithoutUpdate.get(niko_MPC_ids.satelliteEntityHandler) != null)
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

    @JvmStatic
    fun getMemorySatelliteMarket(memory: MemoryAPI): MarketAPI? {
        return memory[niko_MPC_ids.satelliteMarketId] as MarketAPI
    }
    // GETTING ENTITIES AND CHECKING CAPABILITIES
    @JvmStatic
    /**
     * @param location The location to scan.
     * @return A new set containing every entity that had defenseSatellitesApplied(entity) return true.
     */
    fun getEntitiesInLocationWithSatellites(location: LocationAPI): MutableSet<SectorEntityToken> {
        val entitiesWithSatellites: MutableSet<SectorEntityToken> = HashSet()
        for (entity in location.allEntities) {
            if (entity is CampaignFleetAPI) {
                if (entity.isSatelliteFleet()) continue  // if we dont do this, we create an infinite loop of
                // spawning fleets to check the f leets which then check those fleets which then check those fleets
                // does not account for the chance satellites have satellites themselves. if i ever do that, ill make a memkey for it or smthn
            }
            if (entity.hasSatellites()) {
                entitiesWithSatellites.add(entity)
            }
        }
        return entitiesWithSatellites
    }
    @JvmStatic
    fun getNearbyEntitiesWithSatellites(entity: SectorEntityToken): Set<SectorEntityToken> {
        return getNearbyEntitiesWithSatellites(entity.location, entity.containingLocation)
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
            for (handler: niko_MPC_satelliteHandlerCore in entity.getSatelliteHandlers()) {
                if (!MathUtils.isWithinRange(entity, coordinates, handler.satelliteInterferenceDistance)) {
                    iterator.remove() //have to remove because we're using a full list already
                }
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
    fun getNearbyEntitiesWithSatellitesWillingToFight(fleet: CampaignFleetAPI): MutableSet<SectorEntityToken> {
        val entitiesWithSatellites = HashSet(getNearbyEntitiesWithSatellites(fleet.location, fleet.containingLocation))
        for (entity: SectorEntityToken in entitiesWithSatellites) {
            var keepEntity: Boolean = false
            for (handler: niko_MPC_satelliteHandlerCore in entity.getSatelliteHandlers()) {
                if (handler.wantToFight(fleet)) {
                    keepEntity = true
                    break
                }
            }
            if (!keepEntity) {
                entitiesWithSatellites.remove(entity)
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
    fun getNearbyEntitiesWithSatellitesWillingToJoinBattle(battle: BattleAPI): HashMap<SectorEntityToken, HashMap<niko_MPC_satelliteHandlerCore, BattleSide>> {
        val entitiesWillingToFight = HashMap<SectorEntityToken, HashMap<niko_MPC_satelliteHandlerCore, BattleSide>>()
        val containingLocation = battle.getContainingLocation() ?: return entitiesWillingToFight // in truth, if there is no containing location, then there would be no entities in range anyway
        val coordinates = battle.computeCenterOfMass()
        for (entity in getNearbyEntitiesWithSatellites(coordinates, containingLocation)) {
            for (handler: niko_MPC_satelliteHandlerCore in entity.getSatelliteHandlers()) {
                val battleSide = handler.getSideForBattle(battle)
                if (isSideValid(battleSide)) {
                    if (entitiesWillingToFight[entity] == null) entitiesWillingToFight[entity] = HashMap()
                    entitiesWillingToFight[entity]!![handler] = battleSide!!
                }
            }
        }
        return entitiesWillingToFight
    }

    @JvmStatic
            /**
             * Gets all entities within range of entity and gets which fleet they'd fight using getsideforsatellitesagainstfleets.
             * @param entity The entity to get coordinates from.
             * @param fleet The first fleet to check.
             * @param otherFleet The second fleet to check.
             * @return A new HashMap in format (entity -> fleet), where entity is the entity willing to fight, and fleet is which fleet they chose. Cannot return nulls.
             */
    fun getNearbyEntitiesWithSatellitesWillingAndCapableToFightFleets(entity: SectorEntityToken, fleet: CampaignFleetAPI?, otherFleet: CampaignFleetAPI?)
    : HashMap<SectorEntityToken, HashMap<niko_MPC_satelliteHandlerCore, CampaignFleetAPI>> {
        val entitiesWithSatellites: Set<SectorEntityToken> = getNearbyEntitiesWithSatellites(entity.location, entity.containingLocation)
        val sidesTaken = HashMap<SectorEntityToken, HashMap<niko_MPC_satelliteHandlerCore, CampaignFleetAPI>>()
        for (entityWithSatellite in entitiesWithSatellites) {
            for (handler: niko_MPC_satelliteHandlerCore in entityWithSatellite.getSatelliteHandlers()) {
                val fleetChosen = handler.getSideAgainstFleets(fleet, otherFleet)
                if (fleetChosen != null) {
                    if (sidesTaken[entityWithSatellite] == null) {
                        sidesTaken[entityWithSatellite] = HashMap()
                    }
                    sidesTaken[entityWithSatellite]!![handler] = fleetChosen
                }
            }
        }
        return sidesTaken
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

    @JvmStatic
    fun CustomCampaignEntityAPI.deleteIfCosmeticSatellite() {
        if (!isCosmeticSatellite()) return
        val handler: niko_MPC_satelliteHandlerCore? = getSatelliteEntityHandler()
        if (handler != null) {
            handler.cosmeticSatellites.remove(this)
        } else displayError("$this, despite being a cosmetic satellite, had no handler")
        Misc.fadeAndExpire(this) //this causes a removeentity after a bit
        //containingLocation.removeEntity(this)
    }

    @JvmStatic
    fun CustomCampaignEntityAPI.isCosmeticSatellite(): Boolean {
        return (hasTag(niko_MPC_ids.cosmeticSatelliteTagId))
    }
}


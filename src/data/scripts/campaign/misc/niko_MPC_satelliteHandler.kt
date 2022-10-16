package data.scripts.campaign.misc

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.BattleAPI.BattleSide
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.campaign.AI.niko_MPC_satelliteFleetAI
import data.scripts.campaign.listeners.niko_MPC_satelliteFleetDespawnListener
import data.scripts.campaign.misc.niko_MPC_satelliteHandler
import data.scripts.everyFrames.niko_MPC_gracePeriodDecrementer
import data.scripts.everyFrames.niko_MPC_satelliteBattleCheckerForStation
import data.scripts.everyFrames.niko_MPC_satelliteFleetProximityChecker
import data.scripts.everyFrames.niko_MPC_temporarySatelliteFleetDespawner
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_fleetUtils.attemptToFillFleetWithVariants
import data.utilities.niko_MPC_fleetUtils.despawnSatelliteFleet
import data.utilities.niko_MPC_fleetUtils.isFleetValidEngagementTarget
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_ids.satelliteHandlerId
import data.utilities.niko_MPC_memoryUtils.deleteMemoryKey
import data.utilities.niko_MPC_orbitUtils.addOrbitPointingDownWithRelativeOffset
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteMarket
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandler
import data.utilities.niko_MPC_satelliteUtils.isSideValid
import data.utilities.niko_MPC_scriptUtils.addScriptsAtValidTime
import org.apache.log4j.Level
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*

class niko_MPC_satelliteHandler @JvmOverloads constructor(
    entity: SectorEntityToken, satelliteId: String, satelliteFactionId: String, satelliteFleetName: String,
    maxPhysicalSatellites: Int, maxBattleSatellites: Int,
    satelliteOrbitDistance: Float, satelliteInterferenceDistance: Float, barrageDistance: Float,
    weightedVariantIds: HashMap<String?, Float?>, orbitalSatellites: MutableList<CustomCampaignEntityAPI>? =
        ArrayList()
) {
    var fleetForPlayerDialog: CampaignFleetAPI? = null
    @JvmField
    var entityStationBattleChecker: niko_MPC_satelliteBattleCheckerForStation? = null
    @JvmField
    var done = false
    fun setEntity(primaryEntity: SectorEntityToken) {
        entity = primaryEntity
    }

    inner class niko_MPC_satelliteParams(
        var satelliteId: String,
        var satelliteFactionId: String,
        var satelliteFleetName: String,
        var maxPhysicalSatellites: Int,
        var maxBattleSatellites: Int,
        var satelliteOrbitDistance: Float,
        var satelliteInterferenceDistance: Float,
        var satelliteBarrageDistance: Float,
        var weightedVariantIds: HashMap<String?, Float?>
    ) {
        fun prepareForGarbageCollection() {
            return
        }
    }

    var params: niko_MPC_satelliteParams
    private var entity: SectorEntityToken?
    var satellites: MutableList<CustomCampaignEntityAPI>?
    var satelliteBarrages: List<SectorEntityToken> = ArrayList()
    var gracePeriods: HashMap<CampaignFleetAPI, Float?>? = HashMap()
    @JvmField
    var gracePeriodDecrementer: niko_MPC_gracePeriodDecrementer? = null
    @JvmField
    var satelliteFleetProximityChecker: niko_MPC_satelliteFleetProximityChecker? = null
    var satelliteFleets: MutableList<CampaignFleetAPI>? = ArrayList()
    private var dummyFleet: CampaignFleetAPI? = null

    init {
        params = niko_MPC_satelliteParams(
            satelliteId,
            satelliteFactionId,
            satelliteFleetName,
            maxPhysicalSatellites,
            maxBattleSatellites,
            satelliteOrbitDistance,
            satelliteInterferenceDistance,
            barrageDistance,
            weightedVariantIds
        )
        this.entity = entity
        satellites = orbitalSatellites
        init()
    }

    private fun init() {
        if (getEntity() == null) {
            prepareForGarbageCollection()
            return
        }
        gracePeriodDecrementer = niko_MPC_gracePeriodDecrementer(this)
        satelliteFleetProximityChecker = niko_MPC_satelliteFleetProximityChecker(this, getEntity())
        entityStationBattleChecker = niko_MPC_satelliteBattleCheckerForStation(this, getEntity().market)
        val scriptsToAdd: List<EveryFrameScript> = ArrayList<EveryFrameScript>(
            Arrays.asList(
                gracePeriodDecrementer,
                satelliteFleetProximityChecker,
                entityStationBattleChecker
            )
        )
        addScriptsAtValidTime(scriptsToAdd, getEntity(), true)
    }

    private val satelliteFaction: FactionAPI
        private get() = Global.getSector().getFaction(satelliteFactionId)

    fun prepareForGarbageCollection() {
        if (done) {
            log.info("handler rejected GC preparations due to aleady having done it")
            return
        }
        done = true
        log.info("handler preparing for gc")
        if (satelliteFleetProximityChecker != null) {
            satelliteFleetProximityChecker!!.prepareForGarbageCollection()
        }
        if (gracePeriodDecrementer != null) {
            gracePeriodDecrementer!!.prepareForGarbageCollection()
        }
        if (entityStationBattleChecker != null) {
            entityStationBattleChecker!!.prepareForGarbageCollection()
        }
        satelliteFleetProximityChecker = null
        gracePeriodDecrementer = null
        entityStationBattleChecker = null
        if (entity != null) {
            val entityMemory = entity.memoryWithoutUpdate
            if (entityMemory[satelliteHandlerId] === this) {
                deleteMemoryKey(entityMemory, satelliteHandlerId)
            } else if (entityMemory.contains(satelliteHandlerId)) {
                displayError("unsynced satellite handler on " + entity.name + " on handler GC attempt")
            }
            //todo: EXPERIMENTAL: not nulling entity
        } else {
            displayError("entity was null on handler GC attempt", true)
        }
        if (satellites != null) {
            satellites!!.clear()
        }
        /*if (satelliteBarrages != null) {
            satelliteBarrages.clear();
        }*/if (gracePeriods != null) {
            gracePeriods!!.clear()
        }
        if (satelliteFleets != null) {
            for (fleet in ArrayList(satelliteFleets)) {
                despawnSatelliteFleet(fleet!!, false)
            }
            satelliteFleets.clear()
        }
        if (dummyFleet != null) {
            despawnSatelliteFleet(dummyFleet!!, true)
            dummyFleet = null
        }
        if (getParams() != null) {
            getParams()!!.prepareForGarbageCollection()
        } else {
            displayError("null params on handler GC attempt, entity: " + entity.name)
        }
        val tracker = getSatelliteBattleTracker()
        tracker.removeHandlerFromAllBattles(this)
    }

    /**
     * Sets the faction ID of the params to factionId, and updates the faction of all satellite entities.
     * @param factionId The factionId to set.
     */
    fun setSatelliteId(factionId: String) {
        setSatelliteId(factionId, true)
    }

    /**
     * Sets the faction ID of the params to factionId. Optionally updates the faction of all satellite entities.
     * @param factionId The factionId to set.
     * @param withUpdate If true, sets the faction of all satellite entities to the new faction id.
     */
    fun setSatelliteId(factionId: String, withUpdate: Boolean) {
        getParams()!!.satelliteFactionId = factionId
        if (withUpdate) {
            updateSatelliteFactions()
        }
    }

    /**
     * Iterates through every satellite entity we hold, and sets their
     * faction to our factionId.
     */
    fun updateSatelliteFactions() {
        for (fleet in satelliteFleets!!) {
            fleet.setFaction(satelliteFactionId)
        }
        for (satellite in satellites!!) {
            satellite.setFaction(satelliteFactionId)
        }
        if (dummyFleet != null) {
            dummyFleet!!.setFaction(satelliteFactionId)
        }
    }

    private val satelliteFactionId: String
        private get() = if (getParams() == null) {
            "derelict"
        } else getParams()!!.satelliteFactionId

    fun getParams(): niko_MPC_satelliteParams? {
        return params
    }

    /**
     * More or less just a safer way to access the satellite faction of an entity.
     * Updates the factionId of the params it is called on.
     * @return A faction ID, in string form.
     */
    val currentSatelliteFactionId: String
        get() {
            updateFactionId()
            return satelliteFactionId
        }
    val currentSatelliteFaction: FactionAPI
        get() = Global.getSector().getFaction(currentSatelliteFactionId)

    /**
     * Updates the factionId, by syncing it with the market, or setting it to derelict
     * if the market is uncolonized.
     * Updates the faction of all satellite entities.
     */
    fun updateFactionId() {
        val market = getEntitySatelliteMarket(entity)
        if (market != null) {
            if (market.isPlanetConditionMarketOnly) {
                if (satelliteFactionId != "derelict") {
                    setSatelliteId("derelict") //its relatively expensive to run this (due to iterations), so we try to minimize it
                }
            } else if (satelliteFactionId != market.factionId) {
                setSatelliteId(market.factionId)
            }
        } else if (satelliteFactionId != entity.faction.id) {
            setSatelliteId(getEntity().faction.id)
        }
    }

    fun updateFactionForSelfAndSatellites() {
        updateFactionId()
        updateSatelliteFactions()
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to get the value from.
     * @return the gracePeriod associated value to fleet.
     */
    fun getGracePeriod(fleet: CampaignFleetAPI): Float {
        addFleetRefToGracePeriodsIfNonePresent(fleet)
        return gracePeriods!![fleet]!!
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to adjust the grace period of.
     * @param amount The amount to adjust the grace period of fleet of.
     */
    fun adjustGracePeriod(fleet: CampaignFleetAPI, amount: Float) {
        addFleetRefToGracePeriodsIfNonePresent(fleet) //todo: make this work with player battles
        gracePeriods!![fleet] = Math.max(0f, gracePeriods!![fleet]!! + amount)
    }

    /**
     * Adds a new entry to gracePeriod, of (Fleet>0f) if none is present.
     * @param fleet The fleet to check.
     */
    private fun addFleetRefToGracePeriodsIfNonePresent(fleet: CampaignFleetAPI) {
        if (gracePeriods!![fleet] == null) {
            gracePeriods!![fleet] =
                0f // we dont use a amount arg here because we only exist here to initialize a new entry
        }
    }

    val satelliteFleetName: String
        get() = getParams()!!.satelliteFleetName

    /**
     * Should be called whenever a new non-dummy satellite fleet is created.
     */
    fun newSatellite(satelliteFleet: CampaignFleetAPI) {
        satelliteFleets!!.add(satelliteFleet)
    }

    val satelliteInterferenceDistance: Float
        get() = getParams()!!.satelliteInterferenceDistance//sanity. im not sure if an angle offset of 360 breaks anything, but in case it does, this is here as a safety net// 1 satellite = offset of 360, so none. 2 satellites = offset or 180, so they are on opposite ends of the planet.
    // 3 satellites = offset of 120, meaning the satellites form a triangle around the entity. Etc.
    /**
     * Generates an offset with which satellites in orbit of our entity will be spaced apart by.
     * Is based on the amount of satellites in orbit.
     * @return The optimal offset with which the satellites in orbit of the entity should be spaced apart by.
     */
    val optimalOrbitalOffsetForSatellites: Float
        get() {
            val numOfSatellites = satellites!!.size
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
     * Places all satellites in orbit around our entity, ensuring they are all equally spaced apart from eachother.
     */
    fun regenerateOrbitSpacing() {
        val optimalOrbitAngleOffset = optimalOrbitalOffsetForSatellites
        var orbitAngle = 0f
        // this for loop won't apply an offset if theres only 1, and only the 1st calculated offset if 2, etc, so its safe to not add a buffer to the calculation in the optimalangle method
        val iterator = satellites!!.iterator()
        while (iterator.hasNext()) {
            val satellite = iterator.next()
            if (orbitAngle >= 360) {
                displayError("regenerateOrbitSpacing orbitAngle = $orbitAngle")
                removeSatellite(satellite, false, false) //we dont want these weirdos overlapping
                iterator.remove()
            }
            addOrbitPointingDownWithRelativeOffset(
                satellite,
                getEntity(),
                orbitAngle,
                getParams()!!.satelliteOrbitDistance
            )
            orbitAngle += optimalOrbitAngleOffset //no matter what, this should end up less than 360 when the final iteration runs
        }
    }

    /**
     * fadeAndExpires the satellite, before removing it from it's containing location, effectively deleting it.
     *
     * @param satellite The satellite to remove.
     */
    fun removeSatellite(satellite: CustomCampaignEntityAPI, regenerateOrbit: Boolean = true, removeFromList: Boolean = true) {
        if (removeFromList) {
            satellites!!.remove(satellite)
        }
        Misc.fadeAndExpire(satellite)
        satellite.containingLocation.removeEntity(satellite)
        deleteMemoryKey(satellite.memoryWithoutUpdate, satelliteHandlerIdAlt)
        if (regenerateOrbit) {
            regenerateOrbitSpacing()
        }
    }

    /**
     * Removes amountOfSatellitesToRemove satellites from entity's orbit. Will end execution early if the list becomes empty.
     *
     * @param amountOfSatellitesToRemove The amount of satellites to remove from entity.
     */
    fun removeSatellitesFromEntity(amountOfSatellitesToRemove: Int) {
        val iterator = satellites!!.iterator()
        while (iterator.hasNext()) {
            val satellite = iterator.next()
            removeSatellite(
                satellite,
                false,
                false
            ) //we cant directly modify the list, hence why we use the straight removal method here
            iterator.remove() // and run iterator.remove
        }
        regenerateOrbitSpacing()
    }

    /**
     * Adds amountOfSatellitesToAdd satellites to our entity through a for loop. Runs addSatellite amountOfSatellitesToAdd times.
     *
     * @param amountOfSatellitesToAdd The amount of satellites.
     * @param id                      The id to be assigned to the satellites.
     * @param faction                 The factionid to be given to the satellites.
     */
    fun addSatellitesToEntity(amountOfSatellitesToAdd: Int, id: String, faction: String?) {
        for (i in 1..amountOfSatellitesToAdd) { //if the /current/ iteration is more than the max satellites in here, stop and regen
            addSatellite(false, id, faction)
        }
        regenerateOrbitSpacing() //only needs to be done once, after all the satellites are added, this does not generate the error
    }

    /**
     * Adds a new CustomCampaignEntity satellite of type id to entity and sets up an orbit around it.
     * @param regenerateOrbit If true, repositions all satellites in orbit with the same ratio
     * of distance to eachother.
     * @param id The Id of the satellite to add.
     * @param factionId The faction id to set as the satellite's faction.
     */
    fun addSatellite(regenerateOrbit: Boolean, id: String, factionId: String?) {
        val satelliteNumber = satellites!!.size + 1
        val orderedId = "$id $satelliteNumber" // the 1st satellite becomes "id 1", etc
        // i dont do this orderedid for any particular reason, i just wanted to. it causes no issues but
        // can safely be removed
        val containingLocation = getEntity().containingLocation
        // instantiate the satellite in the system
        val satellite = containingLocation.addCustomEntity(orderedId, null, id, factionId)
        addOrbitPointingDownWithRelativeOffset(
            satellite,
            getEntity(),
            0f,
            params.satelliteOrbitDistance
        ) //set up the orbit
        satellite.memoryWithoutUpdate[satelliteHandlerIdAlt] = this
        satellites!!.add(satellite) //now add the satellite to the params' list
        if (regenerateOrbit) regenerateOrbitSpacing() //and set up the orbital angles
    }

    fun addSatellitesUpToMax() {
        addSatellitesToEntity(
            getParams()!!.maxPhysicalSatellites,
            getParams()!!.satelliteId,
            getParams()!!.satelliteFactionId
        )
    }

    /**
     * Gets the side our dummy fleet would enter.
     * @param battle The battle to get the side for.
     * @return The battleside that entity's satellites would pick. Can return null if the entity has no satellites.
     */
    fun getSideForBattle(battle: BattleAPI): BattleSide {
        val dummyFleet = dummyFleetWithUpdate
        updateFactionId()
        updateSatelliteFactions()
        val tracker = getSatelliteBattleTracker()
        return if (tracker.areSatellitesInvolvedInBattle(battle, this)) {
            BattleSide.NO_JOIN
        } else battle.pickSide(dummyFleet)
    }

    /**
     * Used for generating battles and autoresolve and such.
     * @param fleet The fleet to check.
     * @return True, if the params' dummy fleet is hostile to the given fleet. False otherwise.
     */
    fun doSatellitesWantToFight(fleet: CampaignFleetAPI): Boolean {
        var marketUncolonized = false
        val market = getEntity().market
        if (market != null) {
            if (market.isPlanetConditionMarketOnly) {
                marketUncolonized = true
            }
        }
        val wantsToFight = dummyFleetWithUpdate!!.isHostileTo(fleet)

        // uncolonized markets are derelict and hostile to everyone
        return wantsToFight || marketUncolonized && fleet.faction.id != "derelict"
    }

    /**
     * Unfinished.
     */
    fun areSatellitesCapableOfFighting(fleet: CampaignFleetAPI): Boolean {
        return areSatellitesCapableOfBlocking(fleet)
    }

    /**
     * Used for things such as preventing the player from interacting with a market.
     * @param fleet The fleet to check.
     * @return True, if the satellite params' faction is inhospitable or worse to fleets' faction, if the fleet has no transponder,
     * or if the satellites want to fight.
     */
    fun doSatellitesWantToBlock(fleet: CampaignFleetAPI): Boolean {
        return !fleet.isTransponderOn ||
                currentSatelliteFaction.isAtBest(fleet.faction, RepLevel.INHOSPITABLE) ||
                doSatellitesWantToFight(fleet)
    }

    /**
     * @return True if the entity isn't already blocking the fleet, or if entity's satellite params' grace period is
     * less or equal to 0. False otherwise.
     */
    fun areSatellitesCapableOfBlocking(fleet: CampaignFleetAPI): Boolean {
        val battle = fleet.battle
        val tracker = getSatelliteBattleTracker()
        return if (battle != null && tracker.areSatellitesInvolvedInBattle(battle, this)) {
            false
        } else getGracePeriod(fleet) <= 0
    }

    /**
     * Uses doEntitySatellitesWantToBlock/Fight and areEntitySatellitesCapableOfFBlocking/Fighting to determine
     * which fleets the satellites would want to fight when spawned.
     * @param fleet The first fleet to check.
     * @param fleetTwo The second fleet to check.
     * @param capabilityCheck If true, runs an additional check that skips over a fleet if areEntitySatellitesCapableOfBlocking returns false.
     * @return Null if the satellites want to fight both or neither, otherwise, returns which of the two fleets they're willing to fight.
     */
    fun getSideForSatellitesAgainstFleets(
        fleet: CampaignFleetAPI?,
        fleetTwo: CampaignFleetAPI?,
        capabilityCheck: Boolean
    ): CampaignFleetAPI? {
        var wantsToFightOne = false
        var wantsToFightTwo = false
        if (fleet === fleetTwo) {
            displayError("getSideForSatellitesAgainstFleets same fleet, fleet: $fleet")
            if (fleet != null) {
                if (doSatellitesWantToFight(fleet) && areSatellitesCapableOfFighting(fleet)) {
                    return fleet
                }
            }
            return null
        }
        if (fleet != null && doSatellitesWantToFight(fleet) && areSatellitesCapableOfFighting(fleet)) wantsToFightOne =
            true
        if (fleetTwo != null && doSatellitesWantToFight(fleetTwo) && areSatellitesCapableOfFighting(fleetTwo)) wantsToFightTwo =
            true
        if (wantsToFightOne && wantsToFightTwo) {
            return null
        }
        if (wantsToFightOne) {
            if (!capabilityCheck || areSatellitesCapableOfBlocking(fleet!!)) {
                return fleet
            }
        } else if (wantsToFightTwo) {
            if (!capabilityCheck || areSatellitesCapableOfBlocking(fleetTwo!!)) {
                return fleetTwo
            }
        }
        return null
    }

    /**
     * Forces us to spawn a full satellite fleet on the target, unless
     * we're already fighting them or they have grace.
     * @param fleet The fleet to check and engage.
     */
    fun makeEntitySatellitesEngageFleet(fleet: CampaignFleetAPI) {
        if (!shouldAndCanEngageFleet(fleet) || !isFleetValidEngagementTarget(fleet)) {
            return
        }
        var battleJoined: BattleAPI? = null
        val battle = fleet.battle
        val satelliteFleet = createNewFullSatelliteFleet(fleet.location, fleet.containingLocation, true, false)
        if (battle != null) {
            if (!battle.join(satelliteFleet)) {
                displayError("makeEntitySatellitesEngageFleet battle join failure, fleet: $fleet, battle: $battle")
            } else {
                battleJoined = battle
            }
        } else { //no battle? fine, i'll MAKE MY OWN
            satelliteFleet.clearAssignments() // just in case the hold assignment all satellite fleets get is fucking with a few things
            satelliteFleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, fleet, 999999999f, null) // again, sanity
            fleet.addAssignmentAtStart(FleetAssignment.INTERCEPT, satelliteFleet, 1f, null)
            satelliteFleet.setCircularOrbit(
                entity, VectorUtils.getAngle(fleet.location, entity.location),
                Misc.getDistance(satelliteFleet, entity), 999999999f
            )
            //todo: remove the above, its a stopgap so that fleets dont drift away, and it sucks ass
            val newBattle =
                Global.getFactory().createBattle(satelliteFleet, fleet) // force the satellite to engage the enemy

            // removing the createBattle doesnt fix the god damn issue where fleets drift
            battleJoined = newBattle
        }
        if (battleJoined != null) {
            val tracker = getSatelliteBattleTracker()
            tracker.associateSatellitesWithBattle(battleJoined, this, battleJoined.pickSide(satelliteFleet))
        }
    }

    fun shouldAndCanEngageFleet(fleet: CampaignFleetAPI): Boolean {
        val tracker = getSatelliteBattleTracker()
        val battle = fleet.battle
        if (battle != null) {
            if (tracker.areSatellitesInvolvedInBattle(battle, this)) return false
            if (!isSideValid(getSideForBattle(battle))) return false
        }
        if (!doSatellitesWantToFight(fleet)) return false
        return if (!areSatellitesCapableOfFighting(fleet)) false else true
    }

    /**
     * Creates an empty fleet with absolutely nothing in it, except for the memflags satellite fleets must have.
     * @return A new satellite fleet.
     */
    fun createSatelliteFleetTemplate(): CampaignFleetAPI {
        val fleet = Global.getFactory().createEmptyFleet(currentSatelliteFactionId, satelliteFleetName, true)
        // fleet.setFaction(getCurrentSatelliteFactionId());
        setTemplateMemoryKeys(fleet)
        fleet.ai = niko_MPC_satelliteFleetAI(fleet as CampaignFleet)
        fleet.addEventListener(niko_MPC_satelliteFleetDespawnListener())
        val aiCaptain = AICoreOfficerPluginImpl().createPerson(Commodities.GAMMA_CORE, "derelict", null)
        fleet.setCommander(aiCaptain)
        return fleet
    }

    fun cleanUpSatelliteFleetBeforeDeletion(satelliteFleet: CampaignFleetAPI) {
        val battle = satelliteFleet.battle
        if (battle != null) {
            val tracker = getSatelliteBattleTracker()
            if (tracker.areSatellitesInvolvedInBattle(battle, this)) {
                tracker.removeHandlerFromBattle(battle, this)
            }
        }
        if (fleetForPlayerDialog === satelliteFleet) {
            fleetForPlayerDialog = null
        }
        satelliteFleets!!.remove(satelliteFleet)
        deleteMemoryKey(satelliteFleet.memoryWithoutUpdate, satelliteHandlerId)
    }

    fun spawnSatelliteFleet(
        coordinates: Vector2f,
        location: LocationAPI,
        temporary: Boolean,
        dummy: Boolean
    ): CampaignFleetAPI {
        val satelliteFleet = createSatelliteFleetTemplate()
        location.addEntity(satelliteFleet)
        satelliteFleet.setLocation(coordinates.x, coordinates.y)
        if (temporary) {
            val script = niko_MPC_temporarySatelliteFleetDespawner(satelliteFleet, this)
            satelliteFleet.addScript(script)
            satelliteFleet.memoryWithoutUpdate[niko_MPC_ids.temporaryFleetDespawnerId] = script
        }
        satelliteFleet.addAssignment(FleetAssignment.HOLD, location.createToken(coordinates), 99999999f)
        if (dummy) {
            newDummySatellite(satelliteFleet)
        } else {
            newSatellite(satelliteFleet)
        }
        return satelliteFleet
    }

    fun createNewFullSatelliteFleet(
        coordinates: Vector2f,
        location: LocationAPI,
        temporary: Boolean,
        dummy: Boolean
    ): CampaignFleetAPI {
        val satelliteFleet = spawnSatelliteFleet(coordinates, location, temporary, dummy)
        nameFleetMembers(attemptToFillFleetWithVariants(maxBattleSatellites, satelliteFleet, weightedVariantIds, true))
        return satelliteFleet
    }

    fun nameFleetMembers(fleetMembers: List<FleetMemberAPI>) {
        for (fleetMember in fleetMembers) {
            var name = Global.getSector().getFaction(Factions.DERELICT).pickRandomShipName()
            if (name == null) {
                displayError("deploySatellite null name, fleetMember: $fleetMember")
                name = "this name is an error, please report this to niko"
            }
            fleetMember.shipName = name
        }
    }

    fun createNewFullDummySatelliteFleet(coordinates: Vector2f, location: LocationAPI): CampaignFleetAPI {
        return createNewFullSatelliteFleet(coordinates, location, false, true)
    }

    fun createDummyFleet(entity: SectorEntityToken): CampaignFleetAPI {
        val satelliteFleet = createNewFullDummySatelliteFleet(Vector2f(99999999f, 99999999f), entity.containingLocation)
        satelliteFleet.isDoNotAdvanceAI = true
        return satelliteFleet
    }

    private fun setTemplateMemoryKeys(fleet: CampaignFleetAPI) {
        val fleetMemory = fleet.memoryWithoutUpdate
        fleetMemory[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true
        fleetMemory[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true
        fleetMemory[isSatelliteFleetId] = true
        fleetMemory[satelliteHandlerId] = this
    }

    fun getEntity(): SectorEntityToken? {
        if (entity == null) {
            displayError("entity somehow null on handler getEntity()", true, false)
        }
        return entity
    }

    val maxPhysicalSatellites: Int
        get() = getParams()!!.maxPhysicalSatellites

    fun getMaxPhysicalSatellitesBasedOnEntitySize(radiusDivisor: Float): Int {
        return if (getEntity() == null) 0 else Math.round(getEntity().radius / radiusDivisor)
        // divide the radius of the entity by 5, then round it up or down to the nearest whole number
    }

    val maxBattleSatellites: Int
        get() = getParams()!!.maxBattleSatellites
    val weightedVariantIds: HashMap<String?, Float?>
        get() = getParams()!!.weightedVariantIds
    val satelliteOrbitDistance: Float
        get() = getParams()!!.satelliteOrbitDistance
    /*public FactionAPI getFakeSatelliteFaction() {
        return Global.getSector().getFaction(getParams().fakeSatelliteFactionId);
    }*/// a strange hack i have to do, since this method is called before factions /exist/?
    // update da faction
    /**
     * Instantiates a new dummy fleet is none is present, but ONLY if getSatelliteFaction() doesn't return null.
     * @return the dummyFleet used for things such as targetting and conditional attack logic. Can return a standard
     * satellite fleet if getSatelliteFaction() == null.
     */
    val dummyFleetWithUpdate: CampaignFleetAPI?
        get() {
            val faction = satelliteFaction
            if (dummyFleet == null) {
                if (faction != null) { // a strange hack i have to do, since this method is called before factions /exist/?
                    createDummyFleet(getEntity())
                } else {
                    return spawnSatelliteFleet(getEntity().location, getEntity().containingLocation, true, false)
                }
            }
            dummyFleet!!.setFaction(currentSatelliteFactionId) // update da faction
            return dummyFleet
        }

    /**
     * Instantiates a new dummy fleet is none is present, but ONLY if getSatelliteFaction() doesn't return null.
     * @param fleet The fleet to check.
     * @return dummy.isHostileTo(fleet).
     */
    fun dummyFleetWantsToFight(fleet: CampaignFleetAPI?): Boolean {
        val dummy = dummyFleetWithUpdate
        updateFactionId()
        return dummy!!.isHostileTo(fleet)
    }

    fun newDummySatellite(satelliteFleet: CampaignFleetAPI?) {
        dummyFleet = satelliteFleet
    }

    /** Should compare the current market with an unupdated, "cached" market, and if it's different, remove stuff (industries)
     * from the cached market, like the luddic path suppressor, for example. The cached market should be assigned
     * only when the value of market is initially assigned or a desync is found.*/
    fun handleMarketDesync() {
        val ourMarket = getCurrentMarket() ?: return
        val satelliteMarket = getCachedMarket()
        if (ourMarket != satelliteMarket) {
            if (satelliteMarket != null) {
                if (satelliteMarket.hasSatellites(javaClass)) {
                    displayError("Desync check failure-$satelliteMarket, ${satelliteMarket.name} still has $this" + "applied to it")
                } else for (id: String in getIndustryIds()) satelliteMarket.removeIndustry(id, null, false)
            }
        }
        setCachedMarket(getCurrentMarket())
    }

    companion object {
        private val log = Global.getLogger(niko_MPC_satelliteHandler::class.java)

        init {
            log.level = Level.ALL
        }
    }
}
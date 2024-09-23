package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.sun.org.apache.xpath.internal.operations.Bool
import data.scripts.campaign.econ.conditions.derelictEscort.MPC_derelictEscortAssignmentAI.Companion.MAX_DAYS_ESCORTING_TIL_END
import data.scripts.campaign.econ.conditions.derelictEscort.MPC_derelictEscortAssignmentAI.Companion.MIN_DAYS_ESCORTING_TIL_END
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.getDerelictEscortTimeouts
import data.utilities.niko_MPC_fleetUtils.getRepLevelForArrayBonus
import data.utilities.niko_MPC_fleetUtils.isSatelliteFleet
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getEscortFleetList
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils.isStationFleet
import data.utilities.niko_MPC_reflectionUtils.set
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_stringUtils
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.magiclib.kotlin.isPatrol

class niko_MPC_derelictEscort: niko_MPC_baseNikoCondition() {

    val interval = IntervalUtil(0.5f, 1f)

    companion object {
        const val SELF_MARKET_ACCESSABILITY_INCREMENT = -0.30f
        const val OTHER_MARKET_ACCESSIBILITY_INCREMENT = 0.15f

        const val DAYS_BETWEEN_ESCORTS = 60f
        const val CHANCE_TO_SKIP_SPAWNING_PLAYER_UNINHABITED = 0.75f

        const val UNINHABITED_TIMEOUT_MULT = 5f

        // helps escorts keep up with their escortees even in hyperspace
        const val ESCORT_FLEET_MAX_BURN_MULT = 5f

        const val INHABITED_BASE_FLEET_POINTS = 50f
        const val UNINHABITED_BASE_FLEET_POINTS = 50f
    }

    var cachedFaction: String? = null

    override fun apply(id: String) {
        super.apply(id)

        applyConditionAttributes(id)

        if (market.isDeserializing() || market?.containingLocation?.isDeserializing() == true/* || Global.getCurrentState() == GameState.TITLE || market.isPlanetConditionMarketOnly || id == "fake_Colonize"*/) return
        syncFaction()
        //val market = getMarket() ?: return
        //val listener = getListener() ?: return
        //listener.market = market
        //listener.start()
    }

    private fun applyConditionAttributes(id: String) {
        val market = getMarket() ?: return
        market.accessibilityMod.modifyFlat(id, SELF_MARKET_ACCESSABILITY_INCREMENT, name)
        val markets = Misc.getMarketsInLocation(market.containingLocation) - market
        val ourFaction = market.faction
        for (iterMarket in markets) {
            val theirFaction = iterMarket.faction
            if (ourFaction.getRelationshipLevel(theirFaction) < RepLevel.FRIENDLY) continue
            iterMarket.accessibilityMod.modifyFlat(id, OTHER_MARKET_ACCESSIBILITY_INCREMENT, "${market.name} $name")
            getAffectedMarketList() += iterMarket
        }
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        //Global.getSector().removeListener(this)
        if (id == null) return
        unapplyConditionAttributes(id)

       //getListener()?.stop()
    }

    private fun unapplyConditionAttributes(id: String) {
        val market = getMarket() ?: return
        market.accessibilityMod.unmodify(id)
        market.hazard.unmodify(id)
        val affectedMarkets = getAffectedMarketList()
        for (iterMarket in affectedMarkets) {
            iterMarket.accessibilityMod.unmodify(id)
        }
        affectedMarkets.clear()
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)

        interval.advance(days)
        if (interval.intervalElapsed()) {
            tryToSpawnEscortsOnAllFleets()
        }

        val market = getMarket() ?: return
        val timeouts = market.getDerelictEscortTimeouts()
        if (timeouts.isNotEmpty()) {
            for (entry in timeouts.toMap()) {
                if (!entry.key.isAlive) {
                    timeouts -= entry.key
                    continue
                }

                var remaining = entry.value
                remaining -= days

                if (remaining <= 0) {
                    timeouts -= entry.key
                    continue
                }

                timeouts[entry.key] = remaining
            }
        }

        val fleetMap = market.getEscortFleetList()
        if (fleetMap.isEmpty()) return
        val iterator = fleetMap.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()

            val escorter = entry.key
            val escortee = entry.value

            if (!escorter.isAlive || !escortee.isAlive) {
                iterator.remove()
                continue
            }
        }
    }

    private fun syncFaction() {
        if (Global.getCurrentState() == GameState.TITLE) return // people have reported syncfaction loading crashes, its probs safe to do this anyway
        val market = getMarket() ?: return

        val fleetList = market.getEscortFleetList()
        if (cachedFaction != Factions.NEUTRAL && !market.isInhabited()) { // we just changed to being uncolonized
            for (entry in fleetList.toMap()) {
                val escortee = entry.key
                val escorter = entry.value

                if (escorter.fleetData?.membersListCopy != null) {
                    for (member in escorter.fleetData.membersListCopy) {
                        escorter.removeFleetMemberWithDestructionFlash(member)
                    }
                }
                fleetList -= escortee
            }
        } else {
            for (entry in fleetList) {
                entry.value.setFaction(getFactionIdForFleets())
            }
            if (market.factionId != null && market.factionId != cachedFaction) {
                tryToSpawnEscortsOnAllFleets()
            }
        }
        cachedFaction = market.factionId
    }

    private fun getFactionIdForFleets(): String {
        val market = getMarket() ?: return Factions.NEUTRAL
        return if (market.isInhabited()) market.factionId else Factions.PLAYER
    }

    private fun tryToSpawnEscortsOnAllFleets() {
        val market = getMarket() ?: return

        val containingLocation = market.containingLocation ?: return
        val routeManager = RouteManager.getInstance()
        val fleetMap: MutableMap<CampaignFleetAPI, RouteData?> = HashMap()
        for (fleet in containingLocation.fleets) {
            fleetMap[fleet] = null
        }
        if (niko_MPC_settings.DERELICT_ESCORT_SIMULATE_FLEETS) {
            val routes = routeManager.getRoutesInLocation(containingLocation)
            for (route in routes) {
                if (route.delay > 0) continue
                if (route.spawner == null) continue
                if (route.isExpired) continue
                if (route.activeFleet != null) continue
                val newFleet = route.spawner?.spawnFleet(route) ?: continue // AFTER THIS, WE SHOULD NOT RETURN WITHOUT DELETING THE FLEET IF WE FAIL
                fleetMap[newFleet] = route
            }
        }
        if (fleetMap.isEmpty()) return // we can return freely, since theres no fleets to worry about
        for ((fleet, route) in fleetMap) {
            tryToSpawnEscortOn(fleet, route)
            if (fleet.isAlive && fleet.isPatrol() && route != null && route.activeFleet != fleet) {
                niko_MPC_debugUtils.displayError("patrol fleet managed to be created and not despawned with no proper activefleet")
                niko_MPC_debugUtils.log.error("${fleet.name}, ${fleet.market?.name}, ${fleet.containingLocation?.name}")
                fleet.despawn(CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY, null)
            }
        }
    }

    private fun getListener(): MPC_derelictEscortListener? {
        val market = getMarket() ?: return null
        var listener = market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_LISTENER_MEMID] as? MPC_derelictEscortListener
        if (listener !is MPC_derelictEscortListener) {
            market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_LISTENER_MEMID] = MPC_derelictEscortListener(market)
            listener = market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_LISTENER_MEMID] as MPC_derelictEscortListener
        }
        return listener
    }

    private fun getAffectedMarketList(): MutableSet<MarketAPI> {
        val market = getMarket() ?: return HashSet()

        var marketList = market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_AFFECTED_MARKETS_MEMID] as? HashSet<MarketAPI>
        if (marketList !is HashSet<*>) {
            market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_AFFECTED_MARKETS_MEMID] = HashSet<MarketAPI>()
            marketList = market.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_AFFECTED_MARKETS_MEMID] as HashSet<MarketAPI>
        }
        return marketList
    }

    /** IN FAIL STATES, DO NOT RETURN NULL! RETURN [handleFailedEscortSpawn]!!!!!!!*/
    fun tryToSpawnEscortOn(fleet: CampaignFleetAPI, route: RouteData?): CampaignFleetAPI? {
        val market = getMarket() ?: return handleFailedEscortSpawn(fleet, route)

        if (fleet.isSatelliteFleet()) return handleFailedEscortSpawn(fleet, route)
        if (fleet.isStationFleet()) return handleFailedEscortSpawn(fleet, route)
        if (fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] != null) return handleFailedEscortSpawn(fleet, route)
        if (fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS] == true) return handleFailedEscortSpawn(fleet, route)
        if (fleet.ai?.currentAssignment?.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN) return handleFailedEscortSpawn(fleet, route)
        val timeout = market.getDerelictEscortTimeouts()[fleet]
        if (fleet in market.getEscortFleetList().keys) return handleFailedEscortSpawn(fleet, route)
        if (timeout != null) return handleFailedEscortSpawn(fleet, route)
        if (!niko_MPC_settings.DERELICT_ESCORT_SPAWN_ON_PATROLS && fleet.isPatrol()) return handleFailedEscortSpawn(fleet, route)

        var factionToUse = market.faction
        if (!market.isInhabited() && fleet.isPlayerFleet) {
            val nextFloat = MathUtils.getRandom().nextFloat()
            if (CHANCE_TO_SKIP_SPAWNING_PLAYER_UNINHABITED > nextFloat) return handleFailedEscortSpawn(fleet, route)
            factionToUse = Global.getSector().playerFaction
        }
        val repLevelNeeded = fleet.getRepLevelForArrayBonus()
        if (factionToUse.getRelationshipLevel(fleet.faction) >= repLevelNeeded) {
            if (route != null) {
                if (route.activeFleet != null) {
                    if (route.activeFleet == fleet) {
                        niko_MPC_debugUtils.displayError("route activefleet was the same as fleet??? what")
                    } else {
                        niko_MPC_debugUtils.displayError("attempted to escort a fleet with an exsisting activeFleet on the route, activeFleet: ${route.activeFleet.name}, spawned fleet: ${fleet.name}")
                        return handleFailedEscortSpawn(fleet, route)
                    }
                }
                set("activeFleet", route, fleet)
                fleet.addEventListener(RouteManager.getInstance())
            }
            return spawnEscortOn(fleet, factionToUse)
        }
        return handleFailedEscortSpawn(fleet, route)
    }

    fun handleFailedEscortSpawn(fleet: CampaignFleetAPI, route: RouteData?): CampaignFleetAPI? {
        if (route != null) {
            val containingLocation = fleet.containingLocation
            fleet.despawn(CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY, null)
            containingLocation.removeEntity(fleet)
            if (fleet == route.activeFleet) {
                set("activeFleet", route, null)
            } else if (route.activeFleet != null) {
                niko_MPC_debugUtils.log.warn("$route had activeFleet that was not null when we tried to abort an escort, this shouldnt happen. activeFleet: ${route.activeFleet.name}, spawned fleet = ${fleet.name}")
            }
        }
        return null
    }

    private fun spawnEscortOn(target: CampaignFleetAPI, faction: FactionAPI = market.faction): CampaignFleetAPI? {
        val market = getMarket() ?: return null

        val inhabited = market.isInhabited()
        val combatPoints = if (inhabited) INHABITED_BASE_FLEET_POINTS else UNINHABITED_BASE_FLEET_POINTS
        val marketForParams = if (!inhabited) null else market
        val qualityOverride = 0.8f
        val params = FleetParamsV3(
            marketForParams,
            market.locationInHyperspace,
            niko_MPC_ids.overgrownNanoforgeFleetFactionId,
            qualityOverride,
            FleetTypes.PATROL_SMALL,
            combatPoints,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f
        )
        params.maxShipSize = 2
        val doctrine = Global.getSector().getFaction(niko_MPC_ids.overgrownNanoforgeFleetFactionId).doctrine.clone()
        doctrine.shipSize = 1
        params.doctrineOverride = doctrine
        val fleetSize = market.stats.dynamic.getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f)
        if (fleetSize < 1) {
            params.ignoreMarketFleetSizeMult = true
        }

        val fleet = FleetFactoryV3.createFleet(params) ?: return null

        market.containingLocation.addEntity(fleet)
        fleet.containingLocation = market.containingLocation
        val primaryEntityLoc = market.primaryEntity.location
        fleet.setLocation(primaryEntityLoc.x, primaryEntityLoc.y)
        var facingToUse = VectorUtils.getAngle(market.primaryEntity.location, target.location)
        if (facingToUse.isNaN()) facingToUse = 0f
        fleet.facing = facingToUse

        setupEscortFleet(fleet, target, faction)
        return fleet
    }

    private fun setupEscortFleet(fleet: CampaignFleetAPI, target: CampaignFleetAPI, faction: FactionAPI) {
        val market = getMarket() ?: return

        fleet.name = "Derelict Escorts"

        fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] = market
        fleet.memoryWithoutUpdate[MemFlags.FLEET_FIGHT_TO_THE_LAST] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE] = false
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.memoryWithoutUpdate[MemFlags.MAY_GO_INTO_ABYSS] = true // need to follow our escorts EVERYWHERE
        fleet.isNoAutoDespawn = true
        fleet.setFaction(faction.id)
        fleet.commander?.setFaction(faction.id)

        fleet.stats.fleetwideMaxBurnMod.modifyFlat(modId, ESCORT_FLEET_MAX_BURN_MULT, "${market.name} $name")

        fleet.removeAbility(Abilities.INTERDICTION_PULSE)
        fleet.removeAbility(Abilities.SENSOR_BURST)

        fleet.removeAbility(Abilities.SUSTAINED_BURN)
        fleet.removeAbility(Abilities.GO_DARK)
        fleet.removeAbility(Abilities.TRANSPONDER)

        fleet.addAbility("MPC_escort_sustained_burn")
        fleet.addAbility("MPC_escort_go_dark")
        fleet.addAbility("MPC_escort_transponder")

        MPC_derelictEscortAssignmentAI(fleet, target, market).start()
        if (!target.isPlayerFleet) target.isNoAutoDespawn = true

        var timeoutMult = 1f
        if (!market.isInhabited()) {
            timeoutMult *= UNINHABITED_TIMEOUT_MULT
        }
        market.getDerelictEscortTimeouts()[target] = DAYS_BETWEEN_ESCORTS * timeoutMult
        market.getEscortFleetList()[target] = fleet
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return
        val market = getMarket() ?: return

        val patrolString = if (niko_MPC_settings.DERELICT_ESCORT_SPAWN_ON_PATROLS) "" else " non-patrol"
        tooltip.addPara(
            "%s for all friendly$patrolString/trade fleets in system that follow for %s (affected by fleet size)",
            10f,
            Misc.getHighlightColor(),
            "Creates escorts", "${MIN_DAYS_ESCORTING_TIL_END.toInt()} - ${MAX_DAYS_ESCORTING_TIL_END.toInt()} days"
        )
        if (!market.isInhabited()) {
            tooltip.addPara(
                "Until ${market.name} is %s, escort fleets will %s if the escortee is more than %s from ${market.name}",
                10f,
                Misc.getHighlightColor(),
                "colonized", "return", "${MPC_derelictEscortAssignmentAI.MAX_FOLLOW_PLAYER_DIST_LY.toInt()} ly"
            )
        }
        tooltip.addPara(
            "%s accessibility",
            10f,
            Misc.getHighlightColor(),
            niko_MPC_stringUtils.toPercent(SELF_MARKET_ACCESSABILITY_INCREMENT)
        )
        tooltip.addPara(
            "%s accessibility to all other friendly markets in-system",
            10f,
            Misc.getHighlightColor(),
            "+${niko_MPC_stringUtils.toPercent(OTHER_MARKET_ACCESSIBILITY_INCREMENT)}"
        )
    }

    // LISTENER CODE BELOW

    /*override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {

        if (fleet == null || Global.getCurrentState() == GameState.TITLE) return
        if (to?.destination?.containingLocation == market.containingLocation && !market.isDeserializing()) {
            tryToSpawnEscortOn(fleet)
        }
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        if (fleet == null || Global.getCurrentState() == GameState.TITLE) return
        if (fleet.containingLocation == market.containingLocation && !market.isDeserializing()) {
            tryToSpawnEscortOn(fleet)
        }
    }

    override fun reportFleetDespawned(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {

        if (fleet == null) return

        if (fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_MEMID] == market) {
            getFleetList() -= fleet
            return
        }
        val escortingFleets = fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_TARGET_MEMID] as? HashMap<MarketAPI, CampaignFleetAPI> ?: return
        escortingFleets.clear() // the assignment ai handles what happens if they despawn
    }

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        return
    }

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        return
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        return
    }

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        return
    }

    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        return
    }

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI?) {
        return
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        return
    }

    override fun reportPlayerReputationChange(person: PersonAPI?, delta: Float) {
        return
    }

    override fun reportPlayerActivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDeactivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDumpedCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportPlayerDidNotTakeCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportEconomyTick(iterIndex: Int) {
        return
    }

    override fun reportEconomyMonthEnd() {
        return
    }*/
}
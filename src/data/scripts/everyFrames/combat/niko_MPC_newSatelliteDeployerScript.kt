package data.scripts.everyFrames.combat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatFleetManagerAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.HullModSpecAPI
import com.fs.starfarer.api.mission.FleetSide
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_fleetUtils.getSatelliteEntityHandler
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_satelliteBattleTracker
import data.utilities.niko_MPC_satelliteUtils
import data.utilities.niko_MPC_satelliteUtils.getSatelliteBattleTracker
import org.lwjgl.util.vector.Vector2f

class niko_MPC_newSatelliteDeployerScript : BaseEveryFrameCombatPlugin() {
    private var framesToWait = 2f
    var escapeBattle = false

    private class satelliteDeploymentParams {
        var mapWidth = 0f
        var mapHeight = 0f
        var initialXCoord = 0f
        var xOffset = 0f
        var xThresholdForWrapping = 0f
        var initialYCoord = 0f
        var yOffset = 0f
        var facing = 0f
        var yCoord = 0f
        var xCoord = 0f
        fun invertCoordinates() {
            initialYCoord *= -1f
            yOffset *= -1f
            facing = if (facing == 90f) {
                270f
            } else 90f
        }
    }

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        super.advance(amount, events)
        if (framesToWait > 0) {
            framesToWait--
            return
        }
        val engine = Global.getCombatEngine()
        if (engine.isSimulation) {
            return engine.removePlugin(this)
        }
        val playerGoal = engine.context.playerGoal
        val otherGoal = engine.context.otherGoal
        if (otherGoal == FleetGoal.ESCAPE || playerGoal == FleetGoal.ESCAPE) escapeBattle = true
        deploySatellitesForSide(FleetSide.PLAYER, engine)
        deploySatellitesForSide(FleetSide.ENEMY, engine)
        prepareForGarbageCollection()
        engine.removePlugin(this)
    }

    private fun prepareForGarbageCollection() {
        return
    }

    private fun deploySatellitesForSide(side: FleetSide, engine: CombatEngineAPI) {
        val satellites: MutableList<FleetMemberAPI> = ArrayList()
        val intOwner = if (side == FleetSide.PLAYER) 0 else 1
        val fleetManager = engine.getFleetManager(side)
        val reserves = fleetManager.reservesCopy
        val ships = engine.ships
        val playerFleet = Global.getSector().playerFleet
        if (playerFleet != null && playerFleet.battle != null) { //we do this because theres no fleets in a mission
            val thisBattle = playerFleet.battle
            val usePlayerSide = side == FleetSide.PLAYER
            // WHY IS THIS SO FUCKING OBTUSE TO DO
            val fleetsOnSide = if (usePlayerSide) thisBattle.playerSide else thisBattle.nonPlayerSide
            for (potentialSatelliteFleet in fleetsOnSide) {
                val handler: niko_MPC_satelliteHandlerCore =
                    potentialSatelliteFleet.getSatelliteEntityHandler() ?: continue
                // else, they have satellites
                val tracker: niko_MPC_satelliteBattleTracker? = getSatelliteBattleTracker()
                var hasSatelliteShips = false
                for (member in potentialSatelliteFleet.fleetData.membersListCopy) {
                    var memberHullSpec = member.hullSpec
                    if (member.hullSpec.isDHull) memberHullSpec = member.hullSpec.dParentHull ?: member.hullSpec
                    if (memberHullSpec.hasTag(niko_MPC_ids.niko_MPC_isSatelliteHullId)) {
                        satellites.add(member)
                        hasSatelliteShips = true
                    }
                }
                if (tracker != null) {
                    if (hasSatelliteShips) if (!tracker.areSatellitesInvolvedInBattle(thisBattle, handler)) {
                        tracker.associateSatellitesWithBattle(
                            thisBattle,
                            handler,
                            thisBattle.pickSide(potentialSatelliteFleet)
                        ) //sanity
                    }
                }
            }
        } else {
            for (ship in reserves) { //missions dont have fleets
                if (ship.hullSpec.hasTag(niko_MPC_ids.niko_MPC_isSatelliteHullId)) { //so we go with the second best thing
                    if (satellites.contains(ship)) {
                        continue
                    }
                    satellites.add(ship)
                }
            }
        }
        for (ship in ships) { // ongoing battles sometimes have them already deployed
            if (ship.hullSpec.hasTag(niko_MPC_ids.niko_MPC_isSatelliteHullId) && ship.owner == intOwner) {
                val shipFleetMember = ship.fleetMember
                if (satellites.contains(shipFleetMember)) {
                    continue
                }
                satellites.add(shipFleetMember)
            }
        }
        forceDeploySatellites(engine, fleetManager, side, satellites)
    }

    private fun forceDeploySatellites(
        engine: CombatEngineAPI,
        fleetManager: CombatFleetManagerAPI,
        side: FleetSide,
        satellites: List<FleetMemberAPI>
    ) {
        val params = satelliteDeploymentParams()
        val size = satellites.size
        if (size == 0) return
        params.mapWidth = engine.mapWidth
        params.mapHeight = engine.mapHeight
        params.initialXCoord = 0f
        if (size > 1) {
            params.initialXCoord =
                (params.mapWidth * -0.39).toFloat() // if we have more than one, we set it to be at the very edge of the map
        }
        params.xOffset = 0f
        if (size > 1) {
            params.xOffset =
                params.initialXCoord * -2 / (size - 1) //ex. if we start at 400, 2 satellites causes it to be 200, then 100, 50, etc
        }
        params.xThresholdForWrapping = params.mapWidth
        params.initialYCoord = (params.mapHeight * -0.27).toFloat()
        params.yOffset = 200f
        params.facing = 90f
        if (side == FleetSide.ENEMY) {
            params.invertCoordinates() //enemies start at the top
        } else if (escapeBattle) {
            params.invertCoordinates() //pursuit battles would otherwise have friendly satellites spawn inside hostile fleets
        }
        params.yCoord = params.initialYCoord
        params.xCoord = params.initialXCoord
        for (satellite in satellites) {
            if (params.xCoord > params.xThresholdForWrapping) {
                params.xCoord = params.initialXCoord
                params.yCoord += params.yOffset
            }
            var satelliteShip: ShipAPI? = null
            if (fleetManager.deployedCopy.contains(satellite)) {
                //niko_MPC_debugUtils.displayError("satellite already deployed");
                satelliteShip = fleetManager.getShipFor(satellite)
            }
            deploySatellite(
                satellite,
                Vector2f(params.xCoord, params.yCoord),
                params.facing,
                fleetManager,
                side,
                satelliteShip
            )
            params.xCoord += params.xOffset
        }
    }

    private fun deploySatellite(
        satelliteFleetMember: FleetMemberAPI,
        vector2f: Vector2f,
        facing: Float,
        fleetManager: CombatFleetManagerAPI,
        side: FleetSide,
        satellite: ShipAPI?
    ) {
        var satellite = satellite
        if (satellite == null) {
            satellite = fleetManager.spawnFleetMember(satelliteFleetMember, vector2f, facing, 0f)
        }
        satellite!!.location.set(vector2f)
        satellite.facing = facing
        var owner = 1
        if (side == FleetSide.PLAYER) {
            owner = 0
        }
        val modulesWithSatellite = satellite.childModulesCopy
        for (module in modulesWithSatellite) {
            if (module!!.fleetMember != null) {
                module.fleetMember.isAlly = true
                module.fleetMember.owner = owner
            }
            module.owner = owner
            if (owner == 0) {
                module.isAlly = true
            }
            if (Global.getSettings().modManager.isModEnabled("automatic-orders")) {
                module.variant.addMod("automatic_orders_no_retreat")
            }
        }
        for (wing in satellite.allWings) {
            for (fighter in wing.wingMembers) {
                fighter.location.set(vector2f) // so that they dont spawn in wacky places, at least not always
                if (owner == 0) {
                    fighter.isAlly = true
                }
            }
        }
        satellite.fixedLocation = vector2f //todo: how the fuck am i gonna handle the possibility that the satellites spawn in other ships
        Global.getCombatEngine().addPlugin(niko_MPC_satellitePositionDebugger(satellite, vector2f, facing))
        // ^ i found that satellites can spawn in really really wacky places and facings, this is to fix that
    }
}

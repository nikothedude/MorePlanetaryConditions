package data.scripts.campaign.magnetar.crisis.assignments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.DisposableAggroAssignmentAI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.getStationPoint
import data.scripts.campaign.magnetar.crisis.MPC_spyFleetScript
import data.utilities.niko_MPC_ids
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.addSalvageEntity
import org.magiclib.kotlin.getSourceMarket

class MPC_spyAssignmentDeliverResourcesToCache(): MPC_spyAssignment() {

    var targetLoc: SectorEntityToken? = null
    var timeSpentUnloading = 0f
    var unloading = false
    var wandering = false
    var wanderingInterval = IntervalUtil(6f, 7f) // days
    var done = false
    var finishedUnloading = false

    var fakeTravelTarget: MarketAPI? = null
    var fakeCommodityTypeOne: String? = null
    var fakeCommodityTypeTwo: String? = null

    var ranOnce = false
    var playerSawCache = false

    companion object {
        const val DISTANCE = 7f
        const val DISTANCE_TO_TARGET_TO_DELIVER = 300f

        const val TIME_NEEDED_TO_FINISH_UNLOADING = 1.2f // days
        const val CHANCE_TO_DESPAWN = 40f
        val cacheTypeToWeight = hashMapOf(
            Pair("MPC_spySupplyCacheOne", 2f),
            Pair("MPC_spySupplyCacheTwo", 5f),
            Pair("MPC_spySupplyCacheThree", 5f),
        )
        val possibleCommoditiesToUse = listOf(
            Commodities.SUPPLIES, Commodities.CREW, Commodities.FOOD
        )
    }

    override fun init(script: MPC_spyFleetScript) {
        fakeTravelTarget = Global.getSector().playerFaction.getMarketsCopy().firstOrNull { it.primaryEntity?.containingLocation == script.system }
        fakeCommodityTypeOne = possibleCommoditiesToUse.random()
        fakeCommodityTypeTwo = possibleCommoditiesToUse.random()

        targetLoc = script.system.createToken(getStationPoint(script.system))
        script.fleet.clearAssignments()
        script.fleet.addAssignment(FleetAssignment.DELIVER_RESOURCES, targetLoc, 99999f, "delivering $fakeCommodityTypeOne to ${fakeTravelTarget?.name}")

        script.fleet.isTransponderOn = false
        script.fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FORCE_TRANSPONDER_OFF] = true
    }

    override fun advance(amount: Float, script: MPC_spyFleetScript) {
        if (!ranOnce) {
            ranOnce = true
            script.fleet.removeScriptsOfClass(DisposableAggroAssignmentAI::class.java)
        }
        if (done)  {
            if (script.fleet.assignmentsCopy.first().assignment != FleetAssignment.GO_TO_LOCATION_AND_DESPAWN) {
                script.fleet.addAssignment(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    script.fleet.getSourceMarket().primaryEntity,
                    999999f,
                    "delivering $fakeCommodityTypeTwo to ${script.fleet.getSourceMarket().name}"
                )
            }
            return
        }
        val days = Misc.getDays(amount)
        if (wandering) {
            wanderingInterval.advance(days)
            if (wanderingInterval.intervalElapsed()) {
                wandering = false
            }
        } else {
            wanderingInterval.elapsed = 0f
        }

        if (wandering) {
            return
        }
        if (script.fleet.visibilityLevelToPlayerFleet >= SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS && script.fleet.visibilityLevelOfPlayerFleet >= SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS) {
            // they can see us. abort
            if (script.fleet.currentAssignment.assignment != FleetAssignment.PATROL_SYSTEM) {
                script.fleet.clearAssignments()
                script.fleet.addAssignmentAtStart(
                    FleetAssignment.PATROL_SYSTEM,
                    script.system.planets.randomOrNull(),
                    999999f,
                    "delivering $fakeCommodityTypeOne to ${fakeTravelTarget?.name}",
                    null
                )
            }
            unloading = false
            wandering = true
            return
        } else if (script.fleet.currentAssignment?.assignment != FleetAssignment.DELIVER_RESOURCES) {
            script.fleet.clearAssignments()
            script.fleet.addAssignment(FleetAssignment.DELIVER_RESOURCES, targetLoc, 99999f, "delivering $fakeCommodityTypeOne to ${fakeTravelTarget?.name}")
        }
        val distToThing = MathUtils.getDistance(script.fleet, targetLoc)
        if (distToThing <= DISTANCE_TO_TARGET_TO_DELIVER) {
            //script.fleet.clearAssignments()
            script.fleet.currentAssignment.actionText = "unloading cargo"
            unloading = true
        }

        if (unloading) {
            timeSpentUnloading += days
        }
        if (timeSpentUnloading >= TIME_NEEDED_TO_FINISH_UNLOADING) {
            script.fleet.clearAssignments()
            script.fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, script.fleet.getSourceMarket().primaryEntity, 999999f, "delivering $fakeCommodityTypeTwo to ${script.fleet.getSourceMarket().name}")
            finishedUnloading = true
            done = true
            script.fleet.memoryWithoutUpdate[niko_MPC_ids.SPY_FLEET_LAID_CACHE] = true
            finishedLoadingCache(script)
        }
    }

    private fun finishedLoadingCache(script: MPC_spyFleetScript) {
        if (script.fleet.visibilityLevelToPlayerFleet >= SectorEntityToken.VisibilityLevel.SENSOR_CONTACT) {
            val picker = WeightedRandomPicker<String>()
            cacheTypeToWeight.forEach { picker.add(it.key, it.value) }

            val type = picker.pick()
            val cache = script.system.addSalvageEntity(MathUtils.getRandom(), type, Factions.NEUTRAL, null)
            /*val dist = MathUtils.getDistance(script.fleet, script.system.center)
            val angle = VectorUtils.getAngle(script.system.center.location, script.fleet.location)
            cache.setCircularOrbitWithSpin(script.system.center, angle, dist, 200f, 3f, 4f)*/
            cache.setLocation(script.fleet.location.x, script.fleet.location.y)
            MPC_distanceBasedDespawnScript(cache, 3500f).start()
            cache.memoryWithoutUpdate[niko_MPC_ids.SPY_CACHE_FLEET] = script.fleet
            script.fleet.memoryWithoutUpdate[niko_MPC_ids.SPY_FLEET_CACHE] = cache
            playerSawCache = true
        } else {
            val randFloat = MathUtils.getRandom().nextFloat()
            if (randFloat <= CHANCE_TO_DESPAWN * 0.01f) {
                script.fleet.despawn()
            }
        }
    }


}
package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator.TaskInterval
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RouteFleetAssignmentAI
import com.fs.starfarer.api.util.CountingMap
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import kotlin.math.max

class MPC_IAIICPatrolAssignmentAI(fleet: CampaignFleetAPI, route: RouteData): RouteFleetAssignmentAI(fleet, route), FleetActionTextProvider {

    companion object {
        const val PREP_STAGE: String = "a"
        const val TRAVEL_TO_STAGE: String = "b"
        const val PATROL_STAGE: String = "c"
        const val RETURN_STAGE: String = "d"
        const val STAND_DOWN_STAGE: String = "e"
    }

    override fun giveInitialAssignments() {
        //super.giveInitialAssignments();


        val target = pickEntityToGuard() ?: return

        val current = route.getCurrent()
        val source = route.getMarket().primaryEntity

        val intervals: Array<TaskInterval> = arrayOf(
            TaskInterval.days(3f + Math.random().toFloat() * 3f),
            TaskInterval.travel(),
            TaskInterval.remaining(1f),
            TaskInterval.travel(),
            TaskInterval.days(3f + Math.random().toFloat() * 3f),
        )

        RouteLocationCalculator.computeIntervalsAndSetLocation(
            fleet, current.elapsed, current.daysMax,
            false, intervals,
            source, source, target, target, source, source
        )

        fleet.clearAssignments()


        // time to spend traveling to location and patrolling it
        var combinedTravelAndPatrolTime = intervals[1]!!.value + intervals[2]!!.value

        if (intervals[0]!!.value > 0) {
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, source, intervals[0]!!.value, PREP_STAGE)
        }
        if (intervals[1]!!.value > 0) {
            fleet.addAssignment(
                FleetAssignment.GO_TO_LOCATION, target, combinedTravelAndPatrolTime, TRAVEL_TO_STAGE,
                true, null, null
            )
            combinedTravelAndPatrolTime = 0f
        }
        if (intervals[2]!!.value > 0) {
            fleet.addAssignment(
                FleetAssignment.PATROL_SYSTEM, target, combinedTravelAndPatrolTime, PATROL_STAGE,
                false,
                if (target.isSystemCenter) object : Script {
                    override fun run() {
                        fleet.memoryWithoutUpdate.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true)
                    }
                } else null,
                if (target.isSystemCenter) object : Script {
                    override fun run() {
                        fleet.memoryWithoutUpdate.unset(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT)
                    }
                } else null
            )
        }


        if (intervals[3]!!.value > 0) { // return for as long as it takes, not just the interval value
            fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, source, 1000f, RETURN_STAGE)
        }


        // if there was no return stage, that means we spawned right in orbit, since
        // here always justSpawned == true
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, source, intervals[4]!!.value, STAND_DOWN_STAGE)
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 1000f,
            STAND_DOWN_STAGE, goNextScript(current)
        )

        fleet.ai.actionTextProvider = this
    }

    override fun getActionText(fleet: CampaignFleetAPI): String? {
//		if (Misc.getDistance(Global.getSector().getPlayerFleet(), fleet) < fleet.getRadius()) {
//			System.out.println("ewfwefwe");
//		}
        val curr = fleet.currentAssignment
        if (curr == null) return null

        val stage = curr.actionText
        val target = curr.target

        var name = ""
        if (target != null) {
            name = target.name
            if (target is CustomCampaignEntityAPI) {
                val cce = target
                if (name == cce.customEntitySpec.defaultName) {
                    //name = name.toLowerCase();
                    name = cce.customEntitySpec.nameInText
                }
            }
        }

        val pirate = fleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_PIRATE)

        if (pirate) {
            if (PREP_STAGE == stage) {
                return "preparing for patrol duty"
            } else if (TRAVEL_TO_STAGE == stage && target != null && !target.isSystemCenter && !target.isInHyperspace) {
                return "traveling to $name"
            } else if (TRAVEL_TO_STAGE == stage) {
                return "traveling"
            } else if (PATROL_STAGE == stage && target != null) {
                if (target.hasTag(Tags.OBJECTIVE)) {
                    return "guarding $name"
                } else if (target.hasTag(Tags.JUMP_POINT)) {
                    return "guarding $name"
                } else if (target.market != null) {
                    return "defending $name"
                } else {
                    return "patrolling"
                }
            } else if (RETURN_STAGE == stage && target != null && !target.isSystemCenter) {
                return "returning to $name"
            } else if (STAND_DOWN_STAGE == stage) {
                return "standing down"
            }
        } else {
            if (PREP_STAGE == stage) {
                return "preparing for patrol duty"
            } else if (TRAVEL_TO_STAGE == stage && target != null && !target.isSystemCenter && !target.isInHyperspace) {
                return "traveling to $name"
            } else if (TRAVEL_TO_STAGE == stage) {
                return "traveling"
            } else if (PATROL_STAGE == stage && target != null) {
                if (target.hasTag(Tags.OBJECTIVE)) {
                    return "guarding $name"
                } else if (target.hasTag(Tags.JUMP_POINT)) {
                    return "guarding $name"
                } else if (target.market != null) {
                    return "patrolling around $name"
                } else {
                    return "patrolling"
                }
            } else if (RETURN_STAGE == stage && target != null && !target.isSystemCenter) {
                return "returning to $name"
            } else if (STAND_DOWN_STAGE == stage) {
                return "standing down from patrol duty"
            }
        }


        //"traveling to " + target.getName()
        //return "patrolling";
        return null
    }

    override fun advance(amount: Float) {
//		if (fleet.isInCurrentLocation() &&
//				Misc.getDistance(Global.getSector().getPlayerFleet(), fleet) < fleet.getRadius()) {
//			System.out.println("ewfwefwe");
//		}
        super.advance(amount)

        checkCapture(amount)
        checkBuild(amount)
    }


    fun pickEntityToGuard(): SectorEntityToken? {
        val random = route.getRandom(1)

        val custom = route.getCustom() as MilitaryBase.PatrolFleetData
        val type = custom.type

        val loc = fleet.containingLocation ?: return null

        val picker = WeightedRandomPicker<SectorEntityToken?>(random)

        val existing = CountingMap<SectorEntityToken?>()
        for (data in RouteManager.getInstance().getRoutesForSource(route.getSource())) {
            val other = data.getActiveFleet() ?: continue
            val curr = other.currentAssignment
            if (curr == null || curr.target == null || curr.assignment != FleetAssignment.PATROL_SYSTEM) {
                continue
            }
            existing.add(curr.target)
        }

        val markets = Misc.getMarketsInLocation(fleet.containingLocation)
        var hostileMax = 0
        var ourMax = 0
        for (market in markets) {
            if (market.faction.isHostileTo(fleet.faction)) {
                hostileMax = max(hostileMax, market.size)
            } else if (market.faction === fleet.faction) {
                ourMax = max(ourMax, market.size)
            }
        }
        val inControl = ourMax > hostileMax

        for (entity in loc.getEntitiesWithTag(Tags.OBJECTIVE)) {
            if (entity.faction !== fleet.faction) continue

            var w = 2f
            for (i in 0..<existing.getCount(entity)) w *= 0.1f

            if (type == FleetFactory.PatrolType.HEAVY) w *= 0.1f

            picker.add(entity, w)
        }


        // patrol stable locations, will build there
        for (entity in loc.getEntitiesWithTag(Tags.STABLE_LOCATION)) {
            var w = 2f
            for (i in 0..<existing.getCount(entity)) w *= 0.1f

            if (type == FleetFactory.PatrolType.HEAVY) w *= 0.1f

            picker.add(entity, w)
        }

        if (inControl) {
            for (entity in loc.jumpPoints) {
                var w = 2f
                for (i in 0..<existing.getCount(entity)) w *= 0.1f

                if (type == FleetFactory.PatrolType.HEAVY) w *= 0.1f

                picker.add(entity, w)
            }

            if (loc is StarSystemAPI && custom.type == FleetFactory.PatrolType.HEAVY) {
                val system = loc
                if (system.hyperspaceAnchor != null) {
                    var w = 3f
                    for (i in 0..<existing.getCount(system.hyperspaceAnchor)) w *= 0.1f
                    picker.add(system.hyperspaceAnchor, w)
                }
            }
        }

        for (market in markets) {
            if (market.faction.isHostileTo(fleet.faction)) continue

            var w = 0f
            if (market === route.getMarket()) {
                w = 5f
            } else {
                // defend on-hostile non-military markets; prefer own faction
                //if (!market.hasSubmarket(Submarkets.GENERIC_MILITARY)) {
                if (market.memoryWithoutUpdate.getBoolean(MemFlags.MARKET_PATROL)) {
                    if (market.faction !== fleet.faction) {
                        w = 0f // don't patrol near patrolHQ/military markets of another faction
                    } else {
                        w = 4f
                    }
                }
            }

            for (i in 0..<existing.getCount(market.primaryEntity)) w *= 0.1f
            picker.add(market.primaryEntity, w)
        }

        if (fleet.containingLocation is StarSystemAPI && type != FleetFactory.PatrolType.HEAVY) {
            val system = fleet.containingLocation as StarSystemAPI
            var w = 1f
            for (i in 0..<existing.getCount(system.center)) w *= 0.1f
            picker.add(system.center, w)
        }

        var target = picker.pick()
        if (target == null) {
            target = route.market.primaryEntity
        }

        return target
    }

}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript.Companion.NANOFORGE_BOMBARDMENT_DEFAULT_FACTION_ID
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*

class overgrownNanoforgeFleetAssignmentAI(
    protected var fleet: CampaignFleetAPI,
    protected var homeSystem: StarSystemAPI,
    protected var source: SectorEntityToken?
) :EveryFrameScript {
    init {
        giveInitialAssignments()
    }

    val originalFP = fleet.effectiveStrength
    val percentOfFpToStandDown = 0.3f

    private val chanceForStandDown: Float = 0.2f
    var thingsOrbitted = 0
    var orbitThresholdToStandDown = 3
    val chanceForObjective: Float = 0.05f

    protected fun giveInitialAssignments() {
        val playerInSameLocation = fleet.containingLocation === Global.getSector().currentLocation

        // launch from source if player is in-system, or sometimes
        if ((playerInSameLocation || Math.random().toFloat() < 0.1f) && source != null) {
            fleet.setLocation(source!!.location.x, source!!.location.y)
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, source, 3f + Math.random().toFloat() * 2f)
        } else {
            // start at random location
            val target = RemnantSeededFleetManager.pickEntityToGuard(
                Random(),
                homeSystem,
                fleet
            )
            if (target != null) {
                val loc = Misc.getPointAtRadius(target.location, target.radius + 100f)
                fleet.setLocation(loc.x, loc.y)
            } else {
                val loc = Misc.getPointAtRadius(Vector2f(), 5000f)
                fleet.setLocation(loc.x, loc.y)
            }
            pickNext()
        }
    }

    protected fun pickNext() {
        val standDown = shouldStandDown()
        if (!standDown) {

            val target = pickEntityToGuard(
                Random(),
                homeSystem,
                fleet
            )
            thingsOrbitted++
            if (target != null) {
                val speed = Misc.getSpeedForBurnLevel(8f)
                val dist = Misc.getDistance(fleet.location, target.location)
                val seconds = dist / speed
                var days = seconds / Global.getSector().clock.secondsPerDay
                days += 5f + 5f * Math.random().toFloat()
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, target, days, "patrolling")
            } else {
                val days = 5f + 5f * Math.random().toFloat()
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, null, days, "patrolling")
            }
            return
        }
        if (source != null) {
            val dist = Misc.getDistance(fleet.location, source!!.location)
            if (dist > 1000) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, source, 3f, "returning from patrol")
            } else {
                fleet.addAssignment(
                    FleetAssignment.ORBIT_PASSIVE,
                    source, 3f + Math.random().toFloat() * 2f, "standing down"
                )
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 5f)
            }
        }
    }

    private fun shouldStandDown(): Boolean {
        return (source != null &&
                fleet.effectiveStrength/originalFP <= percentOfFpToStandDown) ||
                (thingsOrbitted >= orbitThresholdToStandDown && Math.random().toFloat() < chanceForStandDown)
    }

    private fun pickEntityToGuard(
        random: Random,
        system: StarSystemAPI,
        fleet: CampaignFleetAPI
    ): SectorEntityToken? {
        val picker = WeightedRandomPicker<SectorEntityToken>(random)

        if (isHostileToAll()) {
            for (entity in system.getEntitiesWithTag(Tags.SALVAGEABLE)) {
                if (entity.hasTag(Tags.EXPIRES)) continue
                var w = 1f
                if (entity.hasTag(Tags.NEUTRINO_HIGH)) w = 3f
                if (entity.hasTag(Tags.NEUTRINO_LOW)) w = 0.33f
                picker.add(entity, w)
            }
        }

        for (planet in system.planets) {
            if (planet == source) continue
            picker.add(planet, planetWeight)
        }

        for (entity in system.jumpPoints) {
            picker.add(entity, 1f)
        }

        for (entity in system.getEntitiesWithTag(Tags.OBJECTIVE)) {
            picker.add(entity, objectiveWeight)
        }

        picker.add(source, sourceWeight)

        return picker.pick()
    }

    private fun isHostileToAll(): Boolean {
        return fleet.faction.id == NANOFORGE_BOMBARDMENT_DEFAULT_FACTION_ID
    }

    private fun shouldTryToTargetObjective(): Boolean {
        val objectives = homeSystem.getEntitiesWithTag(Tags.OBJECTIVE)
        if (objectives.isEmpty()) return false

        return (MathUtils.getRandom().nextFloat() < chanceForObjective)
    }

    override fun advance(amount: Float) {
        if (fleet.currentAssignment == null) {
            pickNext()
        }
    }

    override fun isDone(): Boolean {
        return false
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    companion object {
        const val objectiveWeight = 1f
        const val sourceWeight = 1f
        const val planetWeight = 0.5f
    }
}

package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeSpawnFleetScript
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.util.*

class MPC_magnetarFleetAssignmentAI(
    protected var fleet: CampaignFleetAPI,
    protected var homeSystem: StarSystemAPI,
    protected var source: SectorEntityToken?
): niko_MPC_baseNikoScript() {

    val originalFP = fleet.effectiveStrength
    val percentOfFpToStandDown = 0.3f

    private val chanceForStandDown: Float = 0.2f
    var thingsOrbitted = 0
    var orbitThresholdToStandDown = 3
    val chanceForObjective: Float = 0.05f

    init {
        giveInitialAssignments()
    }

    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    protected fun giveInitialAssignments() {
        val playerInSameLocation = fleet.containingLocation === Global.getSector().currentLocation

        // launch from source if player is in-system
        if ((playerInSameLocation) && source != null) {
            fleet.setLocation(source!!.location.x, source!!.location.y)
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, source, 3f + Math.random().toFloat() * 2f)
        } else {
            val field = homeSystem.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_FIELD_MEMID] as? niko_MPC_magnetarField ?: return
            val radius = field.auroraOuterRadius * 0.35f
            val randX = MathUtils.getRandomNumberInRange(-radius, radius)
            val randY = MathUtils.getRandomNumberInRange(-radius, radius)

            fleet.setLocation(randX, randY)
            goToRandLocationInField()

            pickNext()
        }
    }

    protected fun pickNext() {
        val standDown = shouldStandDown()
        if (!standDown) {

            goToRandLocationInField()
            thingsOrbitted++
            return
        }
        if (source != null) {
            val dist = Misc.getDistance(fleet.location, source!!.location)
            if (dist > 1000) {
                fleet.addAssignment(FleetAssignment.PATROL_SYSTEM, source, 3f, "performing unknown maneuvers")
            } else {
                fleet.addAssignment(
                    FleetAssignment.ORBIT_PASSIVE,
                    source, 3f + Math.random().toFloat() * 2f, "performing unknown maneuvers"
                )
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source, 5f, "performing unknown maneuvers")
            }
        }

        /*val standDown = shouldStandDown()
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
        }*/
    }

    private fun goToRandLocationInField() {
        val field =
            homeSystem.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_FIELD_MEMID] as? niko_MPC_magnetarField ?: return
        val radius = field.auroraOuterRadius * 0.35f

        val randXTwo = MathUtils.getRandomNumberInRange(-radius, radius)
        val randYTwo = MathUtils.getRandomNumberInRange(-radius, radius)

        val movementToken = homeSystem.createToken(randXTwo, randYTwo)

        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, movementToken, 30f, "performing unknown maneuvers")
    }

    private fun shouldStandDown(): Boolean {
        return (source != null &&
                fleet.effectiveStrength/originalFP <= percentOfFpToStandDown) ||
                (thingsOrbitted >= orbitThresholdToStandDown && Math.random().toFloat() < chanceForStandDown)
    }


    override fun advance(amount: Float) {
        if (fleet.currentAssignment == null) {
            pickNext()
        }
    }
}
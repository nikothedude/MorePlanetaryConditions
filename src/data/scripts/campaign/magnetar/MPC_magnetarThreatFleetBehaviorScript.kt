package data.scripts.campaign.magnetar

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.OutpostStats
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

class MPC_magnetarThreatFleetBehaviorScript(fleet: CampaignFleetAPI?, system: StarSystemAPI?): ThreatFleetBehaviorScript(fleet, system) {

    var initialized = false
        get() {
            if (field == null) field = true
            return field
        }
    var currBehavior: Behaviors = Behaviors.OUTSKIRT

    enum class Behaviors(val weight: Float) {
        OUTSKIRT(20f),
        DIVE(1f);
    }

    override fun pickNext() {
        if (currBehavior == Behaviors.DIVE) return // itll die.
        currBehavior = pickBehavior()
        if (currBehavior == Behaviors.DIVE) {
            fleet.clearAssignments()
            fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, fleet.starSystem.star, Float.MAX_VALUE, "diving")
        }
    }

    private fun pickBehavior(): Behaviors {
        val picker = WeightedRandomPicker<Behaviors>()
        Behaviors.values().forEach { picker.add(it, it.weight) }
        return picker.pick()
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        if (!fleet.isAlive) return

        if (!initialized) {
            pickBehavior()
            initialized = true
        }


        when (currBehavior) {
            Behaviors.OUTSKIRT -> {
                val field = system.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_FIELD_MEMID] as? niko_MPC_magnetarField ?: return
                val currAngle = VectorUtils.getAngle(system.star.location, fleet.location)
                val newAngle = Misc.getUnitVectorAtDegreeAngle(Misc.normalizeAngle(currAngle + 1f))
                val distance = field.getMaxEffectRadius(newAngle) + 4000f

                val point = newAngle.scale(distance) as? Vector2f ?: return

                var last = fleet.currentAssignment?.maxDurationInDays ?: MathUtils.getRandomNumberInRange(30f, 90f)
                if (fleet.currentAssignment != null) {
                    last -= fleet.currentAssignment.elapsedDays
                }
                fleet.clearAssignments()
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION, system.createToken(point), last, "outskirting") {
                    pickNext()
                }
            }

            Behaviors.DIVE -> {  }
        }
    }

}
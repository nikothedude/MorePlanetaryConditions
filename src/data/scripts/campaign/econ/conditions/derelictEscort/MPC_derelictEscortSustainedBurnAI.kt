package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.FleetAIFlags
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.impl.campaign.abilities.ai.SustainedBurnAbilityAI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

// Same as vanilla's, except doesn't stop burning till much closer than normal, and burns if target is also burning
// a lot of this was taken from tiandong
class MPC_derelictEscortSustainedBurnAI: SustainedBurnAbilityAI() {

    protected var interval = IntervalUtil(0.05f, 0.15f)

    override fun advance(days: Float) {
        interval.advance(days * AI_FREQUENCY_MULT)
        if (!interval.intervalElapsed()) {
            return
        }
        val mem = fleet.memoryWithoutUpdate
        if (ability.isActiveOrInProgress) {
            mem[FleetAIFlags.HAS_SPEED_BONUS, true] = 0.2f
            mem[FleetAIFlags.HAS_HIGHER_DETECTABILITY, true] = 0.2f
        }
        /*val smuggler = mem.getBoolean(MemFlags.MEMORY_KEY_SMUGGLER) // irrelevant - only goes on escort fleets
        if (smuggler) {
            if (ability.isActive) {
                ability.deactivate()
            }
            return
        }*/
        if (fleet.ai is ModularFleetAIAPI) {
            val ai = fleet.ai as ModularFleetAIAPI
            if (ai.tacticalModule.isMaintainingContact) {
                if (ability.isActive) {
                    ability.deactivate()
                }
                return
            }
        }
        if (mem.getBoolean(FleetAIFlags.HAS_LOWER_DETECTABILITY) && !ability.isActive) {
            return
        }
        val pursueTarget = mem.getFleet(FleetAIFlags.PURSUIT_TARGET)
        val fleeingFrom = mem.getFleet(FleetAIFlags.NEAREST_FLEEING_FROM)
        val burn = Misc.getBurnLevelForSpeed(fleet.velocity.length())
        val activationTime = ability.spec.activationDays * Global.getSector().clock.secondsPerDay
        if (fleeingFrom != null) {
            val dist = Misc.getDistance(fleet.location, fleeingFrom.location)
            val speed = Math.max(1f, fleeingFrom.travelSpeed)
            val time = dist / speed
            if (!ability.isActive) {   // Far enough to wind up and get away
                if (time >= activationTime + 5f) {
                    ability.activate()
                }
            } else {   // Too close to wind up, better chance of getting away by turning SB off
                if (burn <= 3 && time < 5f) {
                    ability.deactivate()
                }
            }
            return
        }
        if (pursueTarget != null) {
            if (ability.isActive) {
                val toTarget = Misc.getAngleInDegrees(fleet.location, pursueTarget.location)
                val velDir = Misc.getAngleInDegrees(fleet.velocity)
                val diff = Misc.getAngleDiff(toTarget, velDir)
                if (diff > 60f) {
                    ability.deactivate()
                }
            }
            return
        }

        // CHANGED BIT
        var target: SectorEntityToken? = null
        var targetIsBurning = false
        if (fleet.ai != null && fleet.ai.currentAssignment != null) {
            val curr = fleet.ai.currentAssignmentType
            target = fleet.ai.currentAssignment.target
            targetIsBurning = isTargetBurning(target)
        }
        if (target != null) {
            // CHANGED BIT
            val curr = fleet.ai.currentAssignmentType
            val inSameLocation = target.containingLocation === fleet.containingLocation
            var distToTarget = 100000f
            if (inSameLocation) {
                distToTarget = Misc.getDistance(target.location, fleet.location)
            }
            if (targetIsBurning) {
                ability.activate()
                return
            }

            // ANOTHER CHANGED BIT
            val close = distToTarget < 600 // 2000;
            if (close
                && !targetIsBurning
                && (curr == FleetAssignment.ORBIT_PASSIVE || curr == FleetAssignment.ORBIT_AGGRESSIVE || curr == FleetAssignment.DELIVER_CREW || curr == FleetAssignment.DELIVER_FUEL || curr == FleetAssignment.DELIVER_MARINES || curr == FleetAssignment.DELIVER_PERSONNEL || curr == FleetAssignment.DELIVER_RESOURCES || curr == FleetAssignment.DELIVER_SUPPLIES || curr == FleetAssignment.RESUPPLY || curr == FleetAssignment.GO_TO_LOCATION || curr == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN)
            ) {
                if (ability.isActive) {
                    ability.deactivate()
                }
                return
            }
            if (inSameLocation && (curr == FleetAssignment.RAID_SYSTEM
                        || curr == FleetAssignment.PATROL_SYSTEM)
            ) {
                if (ability.isActive) {
                    ability.deactivate()
                }
                return
            }
        }
        val travelDest = mem.getVector2f(FleetAIFlags.TRAVEL_DEST)
        if (travelDest != null) {
            val dist = Misc.getDistance(fleet.location, travelDest)
            val speed = Math.max(1f, fleet.travelSpeed)
            val time = dist / speed
            if (!ability.isActive) {
                if (time > activationTime * 2f) {
                    ability.activate()
                }
            }
        }
    }

    protected fun isTargetBurning(target: SectorEntityToken?): Boolean {
        if (target !is CampaignFleetAPI) {
            return false
        }
        if (target.getAbility(Abilities.SUSTAINED_BURN) != null
            && target.getAbility(Abilities.SUSTAINED_BURN).isActiveOrInProgress
        ) {
            return true
        }
        return (target.getAbility("MPC_escort_sustained_burn") != null
                && target.getAbility("MPC_escort_sustained_burn").isActiveOrInProgress)
    }

}
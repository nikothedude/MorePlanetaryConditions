package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import data.utilities.niko_MPC_ids
import org.lazywizard.console.clear

enum class derelictEscortStates {
    CATCHING_UP_TO {
        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            val targetText = if (target.isPlayerFleet) "your fleet" else "escortee"
            val actionText = if (!assignmentAI.shouldDoScaryMessages) "catching up to $targetText" else "pursuing $targetText" // i want to scare people.
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, actionText, null)
            //fleet.getAbility(Abilities.SUSTAINED_BURN)?.activate()
        }
        override fun unapply(fleet: CampaignFleetAPI) {
            //fleet.getAbility(Abilities.SUSTAINED_BURN)?.deactivate()
        }

        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment = FleetAssignment.DELIVER_CREW
    },
    ESCORTING {
        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            val targetText = if (target.isPlayerFleet) "your fleet" else target.fullName
            val actionText = if (!assignmentAI.shouldDoScaryMessages) "escorting $targetText" else "stalking $targetText" // i want to scare people.
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, actionText, null)
        }

        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment {
            return FleetAssignment.ORBIT_PASSIVE
        }
    },
    WAITING_FOR_BATTLE_TO_END {
        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            val targetText = if (target.isPlayerFleet) "your fleet" else "escortree"
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, "waiting for $targetText to finish fighting", null)
        }

        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment {
            return FleetAssignment.ORBIT_PASSIVE
        }
    },
    JOINING_BATTLE {
        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            val targetText = if (target.isPlayerFleet) "your fleet" else "escortee"
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, "moving to assist $targetText in battle", null)
            fleet.setMoveDestinationOverride(target.location.x, target.location.y)
        }

        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment {
            return FleetAssignment.DELIVER_CREW
        }
    },
    MEETING_ESCORT {
        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment {
            return FleetAssignment.INTERCEPT
        }

        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            val targetText = if (target.isPlayerFleet) "your fleet" else "escortee"
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, "meeting $targetText", null)
        }

    },
    RETURNING_TO_BASE {
        override fun addAssignment(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ) {
            if (target == null) return
            fleet.addAssignmentAtStart(getAssignmentType(fleet, assignmentAI, target), target, 1000f, null)
        }

        override fun getAssignmentType(
            fleet: CampaignFleetAPI,
            assignmentAI: MPC_derelictEscortAssignmentAI,
            target: SectorEntityToken?
        ): FleetAssignment {
            return FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
        }
    };

    fun overrideAssignment(fleet: CampaignFleetAPI, assignmentAI: MPC_derelictEscortAssignmentAI, target: SectorEntityToken?) {
        val existingState = fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_STATE_MEMFLAG] as? derelictEscortStates
        var clearAssignments = true
        if (existingState == this) {
            val curr = fleet.ai?.currentAssignment?.assignment
            if (curr == this.getAssignmentType(fleet, assignmentAI, target)) {
                return
            }
            clearAssignments = false
        }
        existingState?.unapply(fleet)
        if (clearAssignments) {
            fleet.clearAssignments()
        }
        addAssignment(fleet, assignmentAI, target)
        fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_STATE_MEMFLAG] = this
    }

    open fun unapply(fleet: CampaignFleetAPI) {
        return
    }

    abstract fun getAssignmentType(fleet: CampaignFleetAPI, assignmentAI: MPC_derelictEscortAssignmentAI, target: SectorEntityToken?): FleetAssignment
    abstract fun addAssignment(fleet: CampaignFleetAPI, assignmentAI: MPC_derelictEscortAssignmentAI, target: SectorEntityToken?)
}
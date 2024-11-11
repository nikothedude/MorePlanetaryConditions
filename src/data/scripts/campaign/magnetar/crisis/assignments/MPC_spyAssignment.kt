package data.scripts.campaign.magnetar.crisis.assignments

import data.scripts.campaign.magnetar.crisis.MPC_spyFleetScript

abstract class MPC_spyAssignment {

    abstract fun init(script: MPC_spyFleetScript)
    abstract fun advance(amount: Float, script: MPC_spyFleetScript)
}

enum class MPC_spyAssignmentTypes {
    DELIVER_RESOURCES {
        override fun getInstance(): MPC_spyAssignment {
            return MPC_spyAssignmentDeliverResourcesToCache()
        }
    };

    abstract fun getInstance(): MPC_spyAssignment
}

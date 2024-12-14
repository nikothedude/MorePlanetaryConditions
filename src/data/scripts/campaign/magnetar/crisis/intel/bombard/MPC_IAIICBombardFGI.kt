package data.scripts.campaign.magnetar.crisis.intel.bombard

import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.intel.group.SindrianDiktatPunitiveExpedition
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

class MPC_IAIICBombardFGI(params: GenericRaidParams?) : GenericRaidFGI(params) {

    companion object {
        const val MPC_IAIIC_BOMBARD_FLEET = "\$MPC_IAIIC_BOMBARD_FLEET"
    }

    protected var interval = IntervalUtil(0.1f, 0.3f)

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            if (isCurrent(PAYLOAD_ACTION)) {
                val reason = "MPC_IAIICBombard"
                for (curr in getFleets()) {
                    Misc.setFlagWithReason(
                        curr.memoryWithoutUpdate, MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                        reason, true, 1f
                    )
                }
            }
        }
    }

    override fun preConfigureFleet(size: Int, m: FleetCreatorMission) {
        m.fleetTypeMedium = FleetTypes.TASK_FORCE // default would be "Patrol", don't want that
    }

    override fun configureFleet(size: Int, m: FleetCreatorMission) {
        m.triggerSetFleetFlag(MPC_IAIIC_BOMBARD_FLEET)
        if (size >= 8) {
            m.triggerSetFleetDoctrineOther(5, 0) // more capitals in large fleets
        }
    }

    override fun abort() {
        if (!isAborted) {
            for (curr in getFleets()) {
                curr.memoryWithoutUpdate.unset(MPC_IAIIC_BOMBARD_FLEET)
            }
        }
        super.abort()
    }
}
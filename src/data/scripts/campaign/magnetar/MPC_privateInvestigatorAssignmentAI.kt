package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.magiclib.kotlin.getSourceMarket

class MPC_privateInvestigatorAssignmentAI(val fleet: CampaignFleetAPI, val target: MarketAPI, val source: MarketAPI): niko_MPC_baseNikoScript() {

    init {
        giveInitialAssignments()
    }

    protected fun giveInitialAssignments() {
        fleet.addAssignment(FleetAssignment.ORBIT_PASSIVE, source.primaryEntity, 15f, "preparing for departure", ASSIGNMENT_COMPLETE_ONE(fleet, target, this))
    }

    class ASSIGNMENT_COMPLETE_ONE(val fleet: CampaignFleetAPI, val target: MarketAPI, val script: MPC_privateInvestigatorAssignmentAI): Script {
        override fun run() {
            fleet.clearAssignments()
            fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, target.primaryEntity, Float.MAX_VALUE, ASSIGNMENT_COMPLETE_TWO(fleet, target, script))
        }
    }
    class ASSIGNMENT_COMPLETE_TWO(val fleet: CampaignFleetAPI, val target: MarketAPI, val script: MPC_privateInvestigatorAssignmentAI): Script {
        override fun run() {
            fleet.memoryWithoutUpdate["\$MPC_privateInvestigatorCurrentlyInvestigating"] = true
            fleet.clearAssignments()
            fleet.addAssignmentAtStart(FleetAssignment.ORBIT_PASSIVE, target.primaryEntity, Float.MAX_VALUE, "investigating ${target.name}",  ASSIGNMENT_COMPLETE_THREE(fleet, target, script))
        }
    }
    class ASSIGNMENT_COMPLETE_THREE(val fleet: CampaignFleetAPI, val target: MarketAPI, val script: MPC_privateInvestigatorAssignmentAI): Script {
        override fun run() {
            script.abortAndReturnToBase()
        }
    }

    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)

        refreshAssignments()
    }

    private fun refreshAssignments() {

        if (target.primaryEntity == null || !target.isInhabited()) {
            return abortAndReturnToBase()
        }
        if (target.admin.aiCoreId != niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
            return abortAndReturnToBase()
        }
    }

    fun abortAndReturnToBase() {
        delete()
    }

    override fun delete(): Boolean {
        if (!super.delete())
            return false

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            fleet.getSourceMarket().primaryEntity ?: Global.getSector().economy.marketsCopy.random().primaryEntity,
            Float.MAX_VALUE,
            null
        )
        return true
    }

}

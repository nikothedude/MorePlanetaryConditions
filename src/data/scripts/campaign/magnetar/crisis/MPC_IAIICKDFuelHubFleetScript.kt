package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.campaign.fleet.CampaignFleet
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.magiclib.kotlin.hasUnexploredRuins

class MPC_IAIICKDFuelHubFleetScript(
    val fleet: CampaignFleetAPI,
    val target: SectorEntityToken
): niko_MPC_baseNikoScript() {
    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (fleet.isExpired || fleet.isDespawning || fleet.isEmpty) {
            return stop()
        }
        if (target.market?.hasUnexploredRuins() != true) {
            fleet.clearAssignments()
            val sindria = Global.getSector().economy.getMarket("sindria")?.primaryEntity ?: Global.getSector().economy.marketsCopy.randomOrNull()?.primaryEntity ?: return stop()
            fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, sindria, Float.MAX_VALUE, null)
            stop()
        }
    }
}
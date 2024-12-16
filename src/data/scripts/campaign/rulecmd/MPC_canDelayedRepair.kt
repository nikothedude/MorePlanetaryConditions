package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.bombard.MPC_IAIICBombardFGI
import org.lazywizard.lazylib.MathUtils

class MPC_canDelayedRepair: BaseCommandPlugin() {

    companion object {
        const val RANGE_NEEDED = 4000f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false
        val playerFleet = Global.getSector().playerFleet ?: return false

        val intel = MPC_IAIICFobIntel.get() ?: return false
        if (intel.currentAction == null) return false
        val currentAction = intel.currentAction
        if (currentAction is GenericRaidFGI) {
            for (fleet in currentAction.fleets) {
                if (isInRange(fleet, playerFleet)) return true
            }
        } else if (currentAction is RaidIntel) {
            for (fleet in RouteManager.getInstance().getRoutesForSource(currentAction.routeSourceId).map { it.activeFleet }) {
                if (isInRange(fleet, playerFleet)) return true
            }
        }

        return false
    }

    private fun isInRange(fleet: CampaignFleetAPI?, playerFleet: CampaignFleetAPI): Boolean {
        if (fleet == null) return false
        val dist = MathUtils.getDistance(fleet, playerFleet)
        if (dist <= RANGE_NEEDED) return true
        return false
    }
}
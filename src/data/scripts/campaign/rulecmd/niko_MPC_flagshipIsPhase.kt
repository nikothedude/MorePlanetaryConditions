package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_debugUtils

class niko_MPC_flagshipIsPhase: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI,
        params: MutableList<Misc.Token>,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {

        val fleet: CampaignFleetAPI
        val string: String? = params[0].getString(memoryMap)
        if (string != null && string == "getPlayerFleet") {
            fleet = Global.getSector().playerFleet
        } else {
            fleet = params[0].getObject(memoryMap) as? CampaignFleetAPI ?: return false
        }

        return fleet.flagship.isPhaseShip
    }
}
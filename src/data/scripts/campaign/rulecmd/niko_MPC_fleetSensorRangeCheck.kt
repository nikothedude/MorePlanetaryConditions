package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_debugUtils

class niko_MPC_fleetSensorRangeCheck: BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: MutableList<Misc.Token>,
        memoryMap: MutableMap<String, MemoryAPI>
    ): Boolean {

        val fleet: CampaignFleetAPI
        val string: String? = params[0].getString(memoryMap)
        if (string != null && string == "getPlayerFleet") {
            fleet = Global.getSector().playerFleet
        } else {
            fleet = params[0].getObject(memoryMap) as? CampaignFleetAPI ?: return false
        }
        val operator: String = params[1].getString(memoryMap) ?: return false
        val threshold: Float = params[2].getFloat(memoryMap) ?: return false

        val strength = fleet.sensorStrength

        when (operator) {
            "==" -> return (strength == threshold)
            ">" -> return (strength > threshold)
            ">=" -> return (strength >= threshold)
            "<" -> return (strength < threshold)
            "<=" -> return (strength <= threshold)
            else -> {
                niko_MPC_debugUtils.log.error("niko_MPC_fleetSensorProfileCheck has invalid operator of $operator passed")
                return false
            }
        }
    }
}
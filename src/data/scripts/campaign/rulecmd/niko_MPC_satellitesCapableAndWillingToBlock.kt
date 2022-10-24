package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_dialogUtils.digForSatellitesInEntity
import data.utilities.niko_MPC_satelliteUtils.getSatelliteHandlers
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

class niko_MPC_satellitesCapableAndWillingToBlock : BaseCommandPlugin() {
    override fun execute(ruleId: String, dialog: InteractionDialogAPI?, params: List<Misc.Token>, memoryMap: Map<String, MemoryAPI>): Boolean {
        if (dialog == null) return false

        var entity = dialog.interactionTarget
        entity = digForSatellitesInEntity(entity)
        if (!entity.hasSatellites()) return false
        val playerFleet: CampaignFleetAPI = Global.getSector().playerFleet ?: return false

        for (handler: niko_MPC_satelliteHandlerCore in entity.getSatelliteHandlers()) {
            if (handler.wantToBlock(playerFleet) && handler.capableOfBlocking(playerFleet)) return true
        }
        return false
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_dialogUtils.digForSatellitesInEntity
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

class niko_MPC_entityHasSatellites : BaseCommandPlugin() {
    override fun execute(ruleId: String, dialog: InteractionDialogAPI?, params: List<Misc.Token>, memoryMap: Map<String, MemoryAPI>): Boolean {
        if (dialog == null) return false

        var entity = dialog.interactionTarget
        entity = digForSatellitesInEntity(entity)

        return entity.hasSatellites()
    }
}
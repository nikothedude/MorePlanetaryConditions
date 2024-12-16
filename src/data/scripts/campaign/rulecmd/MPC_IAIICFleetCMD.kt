package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.isPatrol

class MPC_IAIICFleetCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)
        val fleet = dialog.interactionTarget as? CampaignFleetAPI ?: return false
        if (fleet.faction.id != niko_MPC_ids.IAIIC_FAC_ID) return false

        when (command) {
            "isIAIIC" -> return true // already checked
            "isPatrol" -> {
                return (fleet.isPatrol())
            }
            "isInspector" -> {
                return (fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.IAIIC_INSPECTION_FLEET))
            }
        }

        return false
    }
}
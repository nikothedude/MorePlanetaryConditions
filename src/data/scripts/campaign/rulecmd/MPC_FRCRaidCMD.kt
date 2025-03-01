package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids

class MPC_FRCRaidCMD: BaseCommandPlugin() {

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (ruleId == null || dialog == null || params == null || memoryMap == null) return false

        val interactionTarget = dialog.interactionTarget ?: return false
        val command = params[0].getString(memoryMap) ?: return false

        when (command) {
            "canAddOption" -> {
                val market = interactionTarget.market ?: return false
                if (market.memoryWithoutUpdate.getBoolean("\$MPC_raidedFRC")) return false
                if (!market.isPlayerOwned) return false
                if (!market.hasCondition("niko_MPC_derelictEscort")) return false

                return true
            }
            "setTempFac" -> {
                dialog.interactionTarget.setFaction(Factions.NEUTRAL)
            }
            "restoreFac" -> {
                dialog.interactionTarget.setFaction(dialog.interactionTarget.market.factionId)
            }
        }
        return false
    }

}
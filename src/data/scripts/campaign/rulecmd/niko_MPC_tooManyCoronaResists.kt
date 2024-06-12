package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_coronaResistScript

class niko_MPC_tooManyCoronaResists: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        val entity = dialog?.interactionTarget ?: return false
        val containingLocation = entity.containingLocation ?: return false

        return MPC_coronaResistScript.interferenceDetected(containingLocation)
    }
}
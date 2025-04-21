package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

class MPC_addOptionConfirmation: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token?>?,
        memoryMap: Map<String?, MemoryAPI?>?
    ): Boolean {
        val optionId = params!![0]!!.getString(memoryMap)
        val text = params[1]!!.getStringWithTokenReplacement(ruleId, dialog, memoryMap)
        val yes = params[2]!!.getStringWithTokenReplacement(ruleId, dialog, memoryMap)
        val no = params[3]!!.getStringWithTokenReplacement(ruleId, dialog, memoryMap)

        dialog!!.optionPanel.addOptionConfirmation(optionId, text, yes, no)

        return true
    }
}
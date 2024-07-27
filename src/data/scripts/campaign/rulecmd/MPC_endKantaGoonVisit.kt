package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_ids

class MPC_endKantaGoonVisit: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DID_KANTA_GOON_VISIT] = true
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.KANTA_EXPECTING_PLAYER] = true
        val textPanel = dialog.textPanel
        val intel = MPC_magnetarQuest()
        Global.getSector().intelManager.addIntel(intel, false, textPanel)

        return true
    }
}
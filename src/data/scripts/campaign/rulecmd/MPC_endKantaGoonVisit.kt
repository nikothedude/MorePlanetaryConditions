package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_debugUtils
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
        if (Global.getSector().intelManager.hasIntelOfClass(MPC_magnetarQuest::class.java)) {
            niko_MPC_debugUtils.displayError("tried to add a 2nd instance of the magnetar quest, this shouldnt happen")
            return true
        }
        Global.getSector().intelManager.addIntel(intel, false, textPanel)

        return true
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_ids

class niko_MPC_kantaCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "chairGivenToKanta" -> {
                val playerFleet = Global.getSector().playerFleet
                playerFleet.fleetData.addFleetMember("MPC_atlas2_kanta_firepower")

                val intel = Global.getSector().intelManager.getFirstIntel(MPC_magnetarQuest::class.java) as? MPC_magnetarQuest
                intel?.stage = MPC_magnetarQuest.Stage.DONE
                intel?.endAfterDelay()
                intel?.sendUpdate(intel.stage, dialog.textPanel);
                Global.getSector().memoryWithoutUpdate["\$MPC_gaveChairToKanta"] = true
            }
            "canVisitKantaAgain" -> {
                val playerFleet = Global.getSector().playerFleet ?: return false
                val hasThrone = playerFleet.cargo.stacksCopy.any { (it.data as? SpecialItemData)?.id == "MPC_specialChair" }

                return (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.KANTA_MAGNETAR_QUEST_STARTED] == true && hasThrone)
            }
        }

        return true
    }
}
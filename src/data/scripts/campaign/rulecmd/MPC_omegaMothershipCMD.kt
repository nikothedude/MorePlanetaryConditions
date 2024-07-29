package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveAnyItem
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_ids

class MPC_omegaMothershipCMD: BaseCommandPlugin() {

    companion object {
        const val RADIATION_CR_LOSS_PERCENT = 0.4f
        const val RADIATION_CREW_LOSS = 20f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)
        when (command) {
            "addChair" -> {
                val rule = AddRemoveAnyItem()
                val tokens = Misc.tokenize("SPECIAL") + Misc.tokenize("MPC_specialChair") + Misc.tokenize("1")
                rule.execute(ruleId, dialog, tokens, memoryMap)
                val intel = Global.getSector().intelManager.getFirstIntel(MPC_magnetarQuest::class.java) as? MPC_magnetarQuest
                intel?.stage = MPC_magnetarQuest.Stage.RETURN_CHAIR
                intel?.sendUpdate(intel.stage, dialog.textPanel);
                Global.getSector().memoryWithoutUpdate["\$MPC_didMothershipExposition"] = true
            }
            "irradiateFleet" -> {
                val playerFleet = Global.getSector().playerFleet
                playerFleet.fleetData.membersListCopy.forEach { it.repairTracker.cr *= (1 - RADIATION_CR_LOSS_PERCENT) }

                val rule = AddRemoveAnyItem()
                val tokens = Misc.tokenize("RESOURCES") + Misc.tokenize(Commodities.ALPHA_CORE) + Misc.tokenize("3")
                val tokensTwo = Misc.tokenize("RESOURCES") + Misc.tokenize(Commodities.CREW) + Misc.tokenize(RADIATION_CREW_LOSS.toString())
                rule.execute(ruleId, dialog, tokens, memoryMap)
                rule.execute(ruleId, dialog, tokensTwo, memoryMap)
            }
        }

        return true
    }
}
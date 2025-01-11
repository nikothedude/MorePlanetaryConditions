package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.MPC_incomeTallyListener
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids

class MPC_IAIICJillCMD: BaseCommandPlugin() {

    companion object {
        const val RECENT_INCOME_SHARE = 4f
        const val MINIMUM_CREDIT_COST = 4000000f
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
            "daredJillToRetaliate" -> {
                MPC_IAIICFobIntel.get()?.retaliate(MPC_IAIICFobIntel.RetaliateReason.PISSED_OFF_JILL, dialog.textPanel)
            }
            "isAtPeace" -> {
                return !Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).relToPlayer.isHostile
            }

            "peacePossibilityIs" -> {
                val target = params[1].getString(memoryMap)
                return MPC_IAIICFobIntel.getPeacePossibility().name == target
            }
            "generateCreditConcessionAmount" -> {
                val tally = MPC_incomeTallyListener.MPC_incomeTally.get(true) ?: return false
                val highestRecentIncome = tally.getHighestIncome()
                val optimalIncomeShare = (highestRecentIncome * RECENT_INCOME_SHARE).coerceAtLeast(MINIMUM_CREDIT_COST)

                Global.getSector().memoryWithoutUpdate.set("\$MPC_creditConcessionAmount", optimalIncomeShare, 0f)
                Global.getSector().memoryWithoutUpdate.set("\$MPC_creditConcessionAmountDGS", Misc.getDGSCredits(optimalIncomeShare), 0f)

                return true
            }
            "disarm" -> {
                MPC_IAIICFobIntel.get()?.disarm()
            }
            "doPeace" -> {
                MPC_IAIICFobIntel.get()?.tryPeace(dialog)
            }
        }
        return false
    }
}
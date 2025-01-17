package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_ids

class MPC_RACACMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)
        val market = dialog.interactionTarget?.market

        when (command) {
            "isFractalCore" -> {
                return dialog.interactionTarget.market?.admin?.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID
            }
            "fightingIAIIC" -> {
                return MPC_IAIICFobIntel.get() != null
            }
            "sacrifice" -> {
                if (market == null) return false
                market.admin = null
                MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED, dialog)

                return true
            }
        }

        return false
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.lazywizard.console.BaseCommand

class MPC_hegemonySpyCMD: BaseCommandPlugin() {

    companion object {
        const val DAYS_NEEDED_FOR_SPY_TO_APPEAR = 15
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val interactionTarget = dialog.interactionTarget
        val market = interactionTarget.market ?: return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "canExecute" -> {
                if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DID_HEGEMONY_SPY_VISIT] == true) return false
                if (!market.isPlayerOwned) return false
                if (market.admin?.aiCoreId != niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) return false
                val aiCoreCondition = AICoreAdmin.get(market) ?: return false
                if (aiCoreCondition.daysActive < DAYS_NEEDED_FOR_SPY_TO_APPEAR) return false

                return true
            }
        }
        return false
    }
}
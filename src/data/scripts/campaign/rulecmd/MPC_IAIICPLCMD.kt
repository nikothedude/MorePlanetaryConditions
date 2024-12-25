package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.intel.PerseanLeagueMembership
import com.fs.starfarer.api.impl.campaign.intel.events.PerseanLeagueHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel

class MPC_IAIICPLCMD: BaseCommandPlugin() {
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
            "canAccuse" -> {
                val fobIntel = MPC_IAIICFobIntel.get() ?: return false
                if (dialog.interactionTarget.activePerson?.id != "reynard_hannan") return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_accusedPL")) return false

                return true
            }
            "canAskForSupport" -> {
                val fobIntel = MPC_IAIICFobIntel.get() ?: return false
                if (!PerseanLeagueMembership.isLeagueMember()) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_gotPLSupportFleets")) return false

                return true
            }
        }

        return false
    }
}
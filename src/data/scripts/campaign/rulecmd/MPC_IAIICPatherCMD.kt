package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_luddicContributionIntel
import data.utilities.niko_MPC_marketUtils.isFractalMarket

class MPC_IAIICPatherCMD: BaseCommandPlugin() {
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
            "canCreateFirstBarEvent" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.LUDDIC_PATH }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) return false
                if (!market.isFractalMarket()) return false
                return true
            }
            "canCreateSecondBarEvent" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.LUDDIC_PATH }) return false
                if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) return false
                if (market.faction.id != Factions.LUDDIC_PATH) return false
                return true
            }
            "createIntel" -> {
                val intel = MPC_luddicContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel("Rumors of involvement", dialog.textPanel)
            }
        }

        return false
    }
}
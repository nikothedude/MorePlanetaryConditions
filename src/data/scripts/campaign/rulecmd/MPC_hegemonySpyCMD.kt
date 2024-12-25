package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.Faction
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import org.lazywizard.console.BaseCommand
import org.magiclib.kotlin.getFactionMarkets

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
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_debugHegemonySpy")) return true
                if (Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.DID_HEGEMONY_SPY_VISIT)) return false
                if (Global.getSector().getFaction(Factions.HEGEMONY)?.getFactionMarkets()?.isNotEmpty() != true) return false
                if (!market.isPlayerOwned) return false
                if (!market.isFractalMarket()) return false
                val aiCoreCondition = AICoreAdmin.get(market) ?: return false
                if (aiCoreCondition.daysActive < DAYS_NEEDED_FOR_SPY_TO_APPEAR) return false

                return true
            }
            "isFreePort" -> {
                return market.isFreePort
            }
            "shouldRepatriate" -> {
                return (Global.getSector().playerFaction.getRelationshipLevel(Factions.HEGEMONY) > RepLevel.SUSPICIOUS)
            }
            "endAmbience" -> {
                val ambiencePlayer = BarCMD.getAmbiencePlayer()
                ambiencePlayer?.stop()
                return true
            }
            "startAmbience" -> {
                val ambiencePlayer = BarCMD.getAmbiencePlayer()
                ambiencePlayer?.done = false
                return true
            }
        }
        return false
    }
}
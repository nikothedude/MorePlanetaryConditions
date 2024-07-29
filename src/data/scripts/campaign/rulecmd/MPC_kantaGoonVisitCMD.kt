package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.impl.items.WormholeScannerPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited

class MPC_kantaGoonVisitCMD: BaseCommandPlugin() {
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
            "canVisit" ->  {
                if (Global.getSector().memoryWithoutUpdate["\$MPC_debugKantaGoonVisit"] == true) {
                    Global.getSector().memoryWithoutUpdate["\$MPC_debugKantaGoonVisit"] = false
                    return true
                }

                val faction = market.faction
                if (faction.isHostileTo(Factions.PIRATES)) return false

                val magnetarSystem = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as? StarSystemAPI ?: return false
                val kantasDen = Global.getSector().getEntityById("kantas_den")
                if (kantasDen.market?.isInhabited() == false || kantasDen.market.faction.id != Factions.PIRATES) return false
                val playerLevel = Global.getSector().playerPerson?.stats?.level
                if (playerLevel == null || playerLevel < 14) return false

                if (!WormholeScannerPlugin.canPlayerUseWormholes()) return false
                if (Global.getSector().memoryWithoutUpdate["\$gaATG_missionCompleted"] == false) return false // post-galatia academy
                if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.KANTA_MAGNETAR_QUEST_STARTED] == true) return false
                if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DID_KANTA_GOON_VISIT] == true) return false

                return true
            }
            "confront" -> {
                val ambiencePlayer = BarCMD.getAmbiencePlayer()
                ambiencePlayer?.stop()
                Global.getSoundPlayer().playCustomMusic(1, 1, "music_pirates_market_hostile", true)
            }
            "end" -> {
                Global.getSoundPlayer().playCustomMusic(1, 1, null, false)
            }

        }

        return false
    }
}
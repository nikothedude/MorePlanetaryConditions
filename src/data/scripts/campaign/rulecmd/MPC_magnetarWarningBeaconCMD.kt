package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_addDelayedMusicScript

class MPC_magnetarWarningBeaconCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "startMusic" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
                MPC_addDelayedMusicScript("music_encounter_mysterious_non_aggressive").start() // doesnt work if you go through a wormhole otehrwise
            }
            "endMusic" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
                Global.getSoundPlayer().playCustomMusic(1, 1, null, false)
            }
        }
        return true
    }
}
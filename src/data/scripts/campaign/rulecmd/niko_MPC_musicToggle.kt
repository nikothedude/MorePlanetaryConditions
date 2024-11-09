package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_addDelayedMusicScript
import data.scripts.campaign.magnetar.interactionPlugins.MPC_delayedClearMusicScript

class niko_MPC_musicToggle: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null || memoryMap == null) return false

        var musicId: String? = params[0].getString(memoryMap)
        if (musicId == "") musicId = null
        val toggleMode = params[1].getBoolean(memoryMap)
        val useDelayedScript = if (params.size >= 3) params[2].getBoolean(memoryMap) else false

        if (useDelayedScript) {
            if (musicId == null && !toggleMode) {
                MPC_delayedClearMusicScript().start()
            } else if (musicId != null) {
                MPC_addDelayedMusicScript(musicId).start()
            }
            return true
        }

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(toggleMode)
        Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true)
        return true
    }
}
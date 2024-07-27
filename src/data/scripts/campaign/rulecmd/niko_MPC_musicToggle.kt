package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

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

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(toggleMode)
        Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true)
        return true
    }
}
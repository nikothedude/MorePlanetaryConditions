package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

class niko_MPC_playECMSatellitesSound: BaseCommandPlugin() {
    override fun execute(
        ruleId: String,
        dialog: InteractionDialogAPI,
        params: MutableList<Misc.Token>,
        memoryMap: MutableMap<String, MemoryAPI>
    ): Boolean {
        Global.getSoundPlayer().playUISound("world_interdict_off", 1f, 1f)
        return true
    }
}
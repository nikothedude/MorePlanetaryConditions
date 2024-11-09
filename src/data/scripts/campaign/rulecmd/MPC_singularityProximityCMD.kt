package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids

class MPC_singularityProximityCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false
        val command = params[0].getString(memoryMap)
        when (command) {
            "reroute" -> {
                val singularitySystem = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.SINGULARITY_SYSTEM_MEMID] as StarSystemAPI
                Global.getSector().layInCourseFor(Global.getSector().hyperspace.createToken(singularitySystem.hyperspaceAnchor.location))

                return true
            }
        }

        return false
    }
}
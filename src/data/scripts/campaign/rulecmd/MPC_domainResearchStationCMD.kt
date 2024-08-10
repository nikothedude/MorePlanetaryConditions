package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc

class MPC_domainResearchStationCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "grantPassiveBuff" -> {
                val playerFleet = Global.getSector().playerFleet
                playerFleet.stats.fleetwideMaxBurnMod.modifyFlat("MPC_optimizationsIntegrated", 2f, "Drive core optimizations")
                playerFleet.stats.detectedRangeMod.modifyFlat("MPC_optimizationsIntegrated", 200f, "Drive field bleedout")
                Global.getSector().memoryWithoutUpdate["\$MPC_alternateDomResearchOptionPicked"] = true
            }
        }

        return true
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils

class MPC_randomChanceCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false
        val chance = (params[0].getFloat(memoryMap) ?: return false) / 100f
        val ignoreNext = params[1].getBoolean(memoryMap)

        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_ignoreRandomChance")) return false
        if (ignoreNext) {
            Global.getSector().memoryWithoutUpdate.set("\$MPC_ignoreRandomChance", true, 0f)
        }
        return (MathUtils.getRandom().nextFloat() < chance)
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_disruptedDeliveryFactor
import org.lazywizard.console.BaseCommand

class MPC_disruptedDelivery: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false
        if (params == null) return false

        val points = params[0].getInt(memoryMap) ?: 5

        val ha = HostileActivityEventIntel.get() ?: return false
        ha.addFactor(MPC_disruptedDeliveryFactor(points), dialog)

        return true
    }
}
package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.cargoPicker.MPC_CargoPickerListener

class MPC_pickerCMD: BaseCommandPlugin() {
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
            "donateCores" -> {
                val picker = Global.getSector().memoryWithoutUpdate["\$MPC_pickerInstance"] as? MPC_CargoPickerListener
                    ?: return false
                val playerFleet = Global.getSector().playerFleet
                val playerCargo = playerFleet.cargo

                val copy = picker.getAvailableCargo(playerCargo)

                val width = 310f
                picker.width = width
                dialog.showCargoPickerDialog(
                    picker.title,
                    picker.confirmText,
                    picker.cancelText,
                    true, width, copy, picker)
                return true
            }
        }

        return false
    }
}
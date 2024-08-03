package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveAnyItem
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_addDelayedMusicScript
import data.scripts.campaign.magnetar.interactionPlugins.MPC_delayedClearMusicScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings

class MPC_omegaManufactorumCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "sensorBurst" -> {
                Global.getSoundPlayer().playUISound("ui_sensor_burst_on", 1f, 1f)
            }
            "doBombard" -> {
                Global.getSoundPlayer().playUISound("MPC_explosion", 1f, 1f)
                val interactionTarget = dialog.interactionTarget
                val defenderFleet = interactionTarget.memoryWithoutUpdate["\$defenderFleet"] as? CampaignFleetAPI
                defenderFleet?.despawn()

                interactionTarget.memoryWithoutUpdate["\$defenderFleet"] = null
                interactionTarget.memoryWithoutUpdate["\$hasDefenders"] = null
                interactionTarget.memoryWithoutUpdate["\$defenderFleetDefeated"] = true
                interactionTarget.memoryWithoutUpdate["\$MPC_groundDefensesDisabled"] = true

                val rule = AddRemoveAnyItem()
                rule.execute(ruleId, dialog, Misc.tokenize("RESOURCES") + Misc.tokenize(Commodities.FUEL) + Misc.tokenize("${-(niko_MPC_settings.OMAN_BOMBARD_COST)}"), memoryMap)
            }
            "playMusic" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
                MPC_addDelayedMusicScript("music_encounter_mysterious").start()
            }
            "stopMusic" -> {
                Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
                Global.getSoundPlayer().playCustomMusic(1, 1, null, true)
            }
            "prepStopMusic" -> {
                MPC_delayedClearMusicScript().start()
            }
            "genLoot" -> {
                val interactionTarget = dialog.interactionTarget
                if (Global.getSector().memoryWithoutUpdate["\$MPC_bombardedOmegaManufactorum"] == true) {
                    interactionTarget.memoryWithoutUpdate[MemFlags.SALVAGE_SPEC_ID_OVERRIDE] = "MPC_omegaManufactorum_bombarded"
                } else {
                    interactionTarget.memoryWithoutUpdate[MemFlags.SALVAGE_SPEC_ID_OVERRIDE] = "MPC_omegaManufactorum_pristine"
                }
            }
        }

        return true
    }
}
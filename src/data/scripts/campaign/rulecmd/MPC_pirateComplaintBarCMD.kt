package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.RuleBasedDialog
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.derelictEscort.niko_MPC_derelictEscort
import lunalib.lunaExtensions.getMarketsCopy

class MPC_pirateComplaintBarCMD: BaseCommandPlugin() {

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)
        when (command) {
            "canDoEvent" -> {
                val market = dialog.interactionTarget.market ?: return false
                if (market.factionId != Factions.PIRATES) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_sawPiratesComplainingAboutFRC")) return false
                if (!Global.getSector().getFaction(Factions.PIRATES).relToPlayer.isHostile) return false
                var isOppressive = false
                for (iterMarket in Global.getSector().playerFaction.getMarketsCopy()) {
                    val FRC = niko_MPC_derelictEscort.get(iterMarket) ?: continue
                    if (FRC.daysActive >= niko_MPC_derelictEscort.DAYS_NEEDED_FOR_PIRATE_EVENT) {
                        isOppressive = true
                        Global.getSector().memoryWithoutUpdate.set("\$MPC_oppressiveMarketName", iterMarket.name, 0f)
                        break
                    }
                }
                if (!isOppressive) return false

                return true
            }
            "setupPeople" -> {
                val globalMem = Global.getSector().memoryWithoutUpdate

                val personOne = Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(FullName.Gender.MALE)

                dialog.interactionTarget.activePerson = personOne
                (dialog.plugin as RuleBasedDialog).notifyActivePersonChanged()
                dialog.visualPanel.showPersonInfo(personOne, false, true)
                dialog.visualPanel.showSecondPerson(Global.getSector().getFaction(Factions.PIRATES).createRandomPerson(FullName.Gender.FEMALE))
            }
        }
        return false
    }

}
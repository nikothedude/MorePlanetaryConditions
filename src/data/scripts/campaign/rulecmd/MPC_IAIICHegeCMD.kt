package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_benefactorDataStore
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyContributionIntel.TargetHouse
import data.scripts.campaign.magnetar.crisis.intel.hegemony.MPC_hegemonyMilitaristicHouseEventIntel

class MPC_IAIICHegeCMD: BaseCommandPlugin() {
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
            "canAddMeetDaudInitialOption" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                if (!intel.factionContributions.any { it.factionId == Factions.HEGEMONY }) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialDaudMeet")) return false
                //if (!market.isFractalMarket()) return false
                return true
            }

            "pullOut" -> {
                val intel = MPC_IAIICFobIntel.get() ?: return false
                val toRemove = intel.factionContributions.firstOrNull { it.factionId == Factions.HEGEMONY } ?: return false
                intel.removeContribution(toRemove, false, dialog)
                for (entry in MPC_benefactorDataStore.get().probableBenefactors.toList()) {
                    if (entry.factionId == Factions.HEGEMONY) {
                        MPC_benefactorDataStore.get().probableBenefactors -= entry
                        break
                    }
                }
            }

            "aristoComms" -> {
                val person = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR) ?: return false
                Global.getSettings().loadTexture("graphics/portraits/MPC_aristoComms.png")
                person.portraitSprite = "graphics/portraits/MPC_aristoComms.png"
            }
            "aristoNormal" -> {
                val person = Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR) ?: return false
                person.portraitSprite = "graphics/portraits/portrait_hegemony10.png"
            }

            "canDoEventideMeeting" -> {
                return MPC_hegemonyContributionIntel.get(false)?.state == MPC_hegemonyContributionIntel.State.GO_TO_EVENTIDE_INIT
            }

            "startQuest" -> {
                MPC_hegemonyContributionIntel.get()?.state = MPC_hegemonyContributionIntel.State.CONVINCE_HOUSES
                MPC_hegemonyContributionIntel.get()?.sendUpdateIfPlayerHasIntel(
                    MPC_hegemonyContributionIntel.State.CONVINCE_HOUSES,
                    dialog.textPanel
                )

                val eventide = Global.getSector().economy.getMarket("eventide") ?: throw RuntimeException("EVENTIDE DOESNT EXIST HOW THE FUCK DID THIS HAPPEN")
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_ARISTO_DEFECTOR))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_MORALIST_ARISTO_REP))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_MILITARIST_ARISTO_REP))
                eventide.commDirectory.addPerson(Global.getSector().importantPeople.getPerson(MPC_People.HEGE_OPPORTUNISTIC_ARISTO_REP))
            }

            "dealingWithAHouse" -> {
                return MPC_hegemonyContributionIntel.get(false)?.currentHouse != TargetHouse.NONE
            }
            "cooldownActive" -> {
                return MPC_hegemonyContributionIntel.get(false)?.cooldownActive == true
            }

            "newHouse" -> {
                val house = params[1].getString(memoryMap)
                val newHouse = TargetHouse.valueOf(house) ?: return false

                MPC_hegemonyContributionIntel.get()?.setNewHouse(newHouse, dialog.textPanel)
                MPC_hegemonyContributionIntel.get()
            }

            "createIntel" -> {
                val intel = MPC_hegemonyContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel("Rumors of involvement", dialog.textPanel)
            }

            "houseTurned" -> {
                MPC_hegemonyContributionIntel.get()?.turnedHouse(dialog.textPanel)
                Global.getSector().memoryWithoutUpdate.set("\$MPC_hegeIAIICHouseCooldown", true, 30f)
            }

            "readyToConfrontMil" -> {
                val intel = MPC_hegemonyMilitaristicHouseEventIntel.get(false) ?: return false
                return intel.progress >= intel.maxProgress
            }

            "OPPgoToReadings" -> {
                val intel = MPC_hegemonyContributionIntel.get(false) ?: return false
                intel.opportunisticState = MPC_hegemonyContributionIntel.OpportunisticState.GO_TO_MESON_READINGS
                intel.sendUpdateIfPlayerHasIntel(intel.opportunisticState, dialog.textPanel)
            }
        }

        return false
    }
}
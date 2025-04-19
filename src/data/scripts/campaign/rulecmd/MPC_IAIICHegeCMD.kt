package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Entities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Voices
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_benefactorDataStore
import data.scripts.campaign.magnetar.crisis.intel.MPC_hegemonyContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_patherContributionIntel
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD.Companion.POSTS_TO_CHANGE_ON_CAPTURE
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD.Companion.generateHideout
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD.Companion.marketSuitableForTransfer
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import data.utilities.niko_MPC_settings
import org.magiclib.kotlin.makeUnimportant

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

            "createIntel" -> {
                val intel = MPC_hegemonyContributionIntel.get(true)
                intel?.sendUpdateIfPlayerHasIntel("Rumors of involvement", dialog.textPanel)
            }
        }

        return false
    }
}
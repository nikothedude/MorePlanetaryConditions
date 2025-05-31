package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CommDirectoryEntryAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
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
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_indieContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_patherContributionIntel
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import org.magiclib.kotlin.getStationIndustry
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import java.util.Arrays
import kotlin.math.abs

class MPC_IAIICIndieCMD: BaseCommandPlugin() {

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

        val contribIntel = MPC_indieContributionIntel.get() ?: return false
        val fobIntel = MPC_IAIICFobIntel.get() ?: return false

        when (command) {
            "hammerEnd" -> {
                val contrib = fobIntel.getContributionById("thehammer")
                contrib?.let { fobIntel.removeContribution(it, params[1].getBoolean(memoryMap)) }
            }
            "hammerGoAway" -> {
                val contrib = fobIntel.getContributionById("thehammer")
                contrib?.custom = "TOLD_TO_GO_AWAY"
                contribIntel.sendUpdate("Wipe out the mercenaries", dialog.textPanel)
            }

            "blackknifeGiveTask" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                blackknifeContrib?.custom = "GAVE_DEAL"
                contribIntel.sendUpdate("blackknifeGivenDeal", dialog.textPanel)
            }
            "blackknifeEnd" -> {
                val blackknifeContrib = fobIntel.getContributionById("blackknife")
                blackknifeContrib?.let { fobIntel.removeContribution(it, false) }
            }
        }

        return false
    }
}
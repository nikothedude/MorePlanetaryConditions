package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.church.MPC_churchContributionIntel
import data.scripts.campaign.plugins.MPC_vignetteRenderer
import data.utilities.niko_MPC_ids
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.magiclib.kotlin.makeImportant

class MPC_IAIICChurchCMD: BaseCommandPlugin() {

    companion object {
        fun getExodus(): StarSystemAPI? = Global.getSector().getStarSystem("Eos Exodus")
    }

    @Transient
    var vignette: LunaCampaignRenderingPlugin? = null

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: List<Misc.Token>?,
        memoryMap: Map<String?, MemoryAPI?>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val interactionTarget = dialog.interactionTarget
        val market = interactionTarget.market

        val command = params[0].getString(memoryMap)

        val fobIntel = MPC_IAIICFobIntel.get() ?: return false
        val contribIntel = MPC_churchContributionIntel.get()

        when (command) {
            "churchHostile" -> {
                return true
            }
            "canDoPreIntelStuff" -> {
                return (fobIntel.getContributionById(Factions.LUDDIC_CHURCH)) != null && contribIntel == null
            }
            "beginIntel" -> {
                val intel = MPC_churchContributionIntel.get(true, noUpdate = true, text = dialog.textPanel)
                intel?.sendUpdateIfPlayerHasIntel(intel.state, dialog.textPanel)
            }
            "endScript" -> {
                MPC_IAIICChurchInitializerScript.get()?.delete()
            }
            "eventActive" -> {
                return contribIntel != null
            }

            "vignette" -> {
                val vignette = MPC_vignetteRenderer()
                Global.getSector().memoryWithoutUpdate["\$MPC_vignetteRenderer"] = vignette
                LunaCampaignRenderer.addTransientRenderer(vignette)
            }
            "endVignette" -> {
                val castedVignette = Global.getSector().memoryWithoutUpdate["\$MPC_vignetteRenderer"] as? MPC_vignetteRenderer
                Global.getSector().memoryWithoutUpdate["\$MPC_vignetteRenderer"] = null
                castedVignette?.expiring = true
                castedVignette?.fader?.fadeOut()
            }
            "meetFence" -> {
                contribIntel?.state = MPC_churchContributionIntel.State.FIND_ASHER_CONTACT
                contribIntel?.sendUpdate(contribIntel.state, dialog.textPanel)
            }
            "stateIs" -> {
                if (contribIntel == null) return false
                val state = params[1].getString(memoryMap)
                return (contribIntel.state.name == state)
            }
            "findAsherKnight" -> {
                contribIntel?.state = MPC_churchContributionIntel.State.GO_TO_ASHER_NANOFORGE
                contribIntel?.sendUpdate(contribIntel.state, dialog.textPanel)
            }
            "availableToGoToNanoforge" -> {
                return contribIntel?.state == MPC_churchContributionIntel.State.GO_TO_ASHER_NANOFORGE || contribIntel?.state == MPC_churchContributionIntel.State.DELIVER_HERETICAL_TECH
            }
            "endAmbience" -> {
                val ambiencePlayer = BarCMD.getAmbiencePlayer()
                ambiencePlayer?.stop()
                return true
            }
            "incrFirstStage" -> {
                MPC_churchContributionIntel.get()?.stageOneCompletion++
                MPC_churchContributionIntel.get()?.sendUpdateIfPlayerHasIntel("STAGE_ONE_INCR", dialog.textPanel)
            }
            "incrSecondStage" -> {
                MPC_churchContributionIntel.get()?.stageTwoCompletion++
                MPC_churchContributionIntel.get()?.sendUpdateIfPlayerHasIntel("STAGE_TWO_INCR", dialog.textPanel)
            }
        }

        return false
    }
}
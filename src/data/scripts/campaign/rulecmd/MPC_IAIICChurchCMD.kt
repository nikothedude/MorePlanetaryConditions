package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.ai.CampaignFleetAI
import data.scripts.campaign.magnetar.crisis.MPC_IAIICChurchInitializerScript
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_churchContributionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_indieContributionIntel
import data.scripts.campaign.plugins.MPC_vignetteRenderer
import data.scripts.campaign.plugins.MPC_vignetteScript
import data.utilities.niko_MPC_ids
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.lwjgl.opengl.GL11
import org.magiclib.kotlin.makeImportant

class MPC_IAIICChurchCMD: BaseCommandPlugin() {

    companion object {
        const val HIDEOUT_ID = "MPC_eos_hideout_planet"
        const val HIDEOUT_NAME = "\$MPC_IAIICChurchHideoutPlanetName"

        fun getExodus(): StarSystemAPI = Global.getSector().getStarSystem("Eos Exodus")
        fun getHideout(): PlanetAPI = getExodus().planets.find { it.id == HIDEOUT_ID }!!
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
            "getHideoutName" -> {
                Global.getSector().memoryWithoutUpdate[HIDEOUT_NAME] = getHideout().name
            }
            "beginIntel" -> {
                val intel = MPC_churchContributionIntel.get(true, noUpdate = true, text = dialog.textPanel)
                intel?.sendUpdateIfPlayerHasIntel(intel.state, dialog.textPanel)
                getHideout().makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }
            "goToHideout" -> {
                val fleet = interactionTarget as? CampaignFleetAPI ?: return false
                fleet.clearAssignments()
                fleet.addAssignmentAtStart(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    getHideout(),
                    Float.MAX_VALUE,
                    null
                )
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
        }

        return false
    }
}
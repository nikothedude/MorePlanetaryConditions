package data.scripts.campaign.rulecmd

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.MPC_privateInvestigatorAssignmentAI
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.interactionPlugins.MPC_addDelayedMusicScript
import data.scripts.campaign.magnetar.niko_MPC_magnetarIntel
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.makeUnimportant

class MPC_magnetarGenericCMD: BaseCommandPlugin() {

    companion object {
        const val DAYS_FRACTAL_CORE_INSTALLED_NEEDED_FOR_SIERRA_WORRY = 20f
    }

    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "learnedInterdiction" -> {
                niko_MPC_magnetarIntel.get()?.sendUpdateIfPlayerHasIntel("discoveredInterdict", dialog.textPanel)
            }
            "sierraWorriedAboutFractalCore" -> {
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_talkedToSierraAboutFractalCore")) return false
                val colony = getFractalColony() ?: return false
                Global.getSector().memoryWithoutUpdate["\$MPC_fractalColonyName"] = colony.name
                return true
            }

            "inspectorsGoAway" -> {
                val fleet = dialog.interactionTarget as? CampaignFleetAPI ?: return false

                fleet.makeUnimportant("\$MPC_hegePrivateInspectors")
                val ai = fleet.scripts.firstOrNull { it is MPC_privateInvestigatorAssignmentAI } as? MPC_privateInvestigatorAssignmentAI ?: return false
                ai.abortAndReturnToBase()
                fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS] = true

                val delayedEnc = DelayedFleetEncounter(MathUtils.getRandom(), "MPC_privateInvestigatorFollowup")
                if (Global.getSettings().isDevMode) {
                    delayedEnc.setDelayNone()
                } else {
                    delayedEnc.setDelayVeryShort()
                }
                delayedEnc.setLocationCoreOnly(true, Factions.HEGEMONY)
                //perseanEncounter.setRequireFactionPresence(Factions.PERSEAN)
                delayedEnc.setDoNotAbortWhenPlayerFleetTooStrong()
                delayedEnc.beginCreate()
                delayedEnc.triggerCreateFleet(
                    HubMissionWithTriggers.FleetSize.MEDIUM, HubMissionWithTriggers.FleetQuality.DEFAULT,
                    Factions.HEGEMONY, FleetTypes.PATROL_SMALL, Vector2f()
                )
                delayedEnc.triggerFleetSetName("Task Force")
                delayedEnc.triggerMakeNonHostile()
                delayedEnc.triggerFleetMakeImportantPermanent("\$MPC_privateInvestigatorFollowup")
                //delayedEnc.triggerFleetMakeFaster(true, 0, true)
                delayedEnc.triggerOrderFleetInterceptPlayer()
                //perseanEncounter.triggerOrderFleetEBurn(1f)
                delayedEnc.triggerSetFleetGenericHailPermanent("MPC_privateInvestigatorFollowupHail")
                delayedEnc.endCreate()
            }
        }
        return true
    }

    fun getFractalColony(): MarketAPI? = MPC_hegemonyFractalCoreCause.getFractalColony()
}
package data.scripts.campaign.singularity

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.interactionPlugins.MPC_delayedClearMusicScript

class MPC_ultimaJumpPointInteractionPlugin: InteractionDialogPlugin {

    private var dialog: InteractionDialogAPI? = null
    private var textPanel: TextPanelAPI? = null
    private var options: OptionPanelAPI? = null
    private var visual: VisualPanelAPI? = null

    private var playerFleet: CampaignFleetAPI? = null
    private var jumpPoint: JumpPointAPI? = null

    enum class Option {
        LEAVE
    }

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        textPanel = dialog.textPanel
        options = dialog.optionPanel
        visual = dialog.visualPanel

        playerFleet = Global.getSector().playerFleet
        jumpPoint = dialog.interactionTarget as JumpPointAPI

        visual!!.setVisualFade(0.25f, 0.25f)
        if (jumpPoint!!.customInteractionDialogImageVisual != null) {
            visual!!.showImageVisual(jumpPoint!!.customInteractionDialogImageVisual)
        } else {
            if (jumpPoint!!.isWormhole) {
                visual!!.showImagePortion("illustrations", "jump_point_wormhole", 640f, 400f, 0f, 0f, 480f, 300f)
            } else {
                if (playerFleet!!.containingLocation.isHyperspace) {
                    visual!!.showImagePortion("illustrations", "jump_point_hyper", 640f, 400f, 0f, 0f, 480f, 300f)
                } else {
                    visual!!.showImagePortion("illustrations", "jump_point_normal", 640f, 400f, 0f, 0f, 480f, 300f)
                }
            }
        }

        dialog.setOptionOnEscape("Leave", Option.LEAVE)
        options!!.addOption("Leave", Option.LEAVE)

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
        if (Global.getSector().memoryWithoutUpdate["\$MPC_knowsAboutUltimaJumppoint"] == true) {
            textPanel!!.addPara(
                "Your fleet approaches the jump point.\n" +
                    "\n" +
                    "The energy field still prevents your fleet from translating directly onto the black hole."
            )
        } else {
            textPanel!!.addPara(
                "Your fleet approaches the jump point.\n" +
                    "\n" +
                    "Even outside the system, the signal is strong. It is clearer now - a composition of what appears to be hyper-modulated p-waves, accented by high-energy baryon emissions, a pattern which is only commonly observed amongst catastrophic phase-coil breaches.\n" +
                    "\n" +
                    "As your fleet aligns for translation, you receive a startling report: The jump point seems to be blocked by some kind of... energy field. Whatever it is, you cannot use the black hole's jump point - much to the relief of your crew.",
                Misc.getHighlightColor(),
                "energy field", "cannot use the black hole's jump point"
            )
        }
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {

//		dialog.hideVisualPanel();
//		dialog.setTextWidth(1000);
        if (optionData == Option.LEAVE) {
            Global.getSector().memoryWithoutUpdate["\$MPC_knowsAboutUltimaJumppoint"] = true

            MPC_delayedClearMusicScript().start()
            dialog!!.dismiss()
        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun advance(amount: Float) {
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
        return HashMap()
    }
}
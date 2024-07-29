package data.scripts.campaign.magnetar.interactionPlugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.niko_MPC_magnetarIntel
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils

class MPC_playerFirstVisitToMagnetar: InteractionDialogPlugin {

    //val optionsUsed = HashSet<MPC_playerExposedToMagnetarCore.Options>()
    //var lastOption: MPC_playerExposedToMagnetarCore.Options? = null

    enum class Options {

    }

    protected var dialog: InteractionDialogAPI? = null
    protected var textPanel: TextPanelAPI? = null
    protected var options: OptionPanelAPI? = null
    protected var visual: VisualPanelAPI? = null

    protected var playerFleet: CampaignFleetAPI? = null

    var stage: MPC_playerExposedToMagnetarCore.Stage = MPC_playerExposedToMagnetarCore.Stage.INITIAL

    override fun init(dialog: InteractionDialogAPI?) {
        if (dialog == null) {
            return
        }

        this.dialog = dialog
        textPanel = dialog.textPanel
        options = dialog.optionPanel
        visual = dialog.visualPanel

        playerFleet = Global.getSector().playerFleet

        visual!!.showImagePortion("illustrations", "niko_MPC_magnetar", 800f, 534f, 0f, 0f, 800f, 534f)

        val timesPanicked = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.TIMES_MAGNETAR_PANICKED] as? Int
        if (timesPanicked != null && timesPanicked >= 1) {
            stage = MPC_playerExposedToMagnetarCore.Stage.INITIAL_DONEBEFORE
        }

        createInitialText()
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
        MPC_addDelayedMusicScript("music_encounter_mysterious_non_aggressive").start() // doesnt work if you go through a wormhole otehrwise
        //Global.getSoundPlayer().playCustomMusic(1, 1,"music_encounter_mysterious_non_aggressive", true)
        //MPC_playerExposedToMagnetarCore.Options.addOptions(this)
    }

    private fun createInitialText() {
        val magnetar = dialog!!.interactionTarget as PlanetAPI
        val dist = MathUtils.getDistance(playerFleet, magnetar)
        val overMagnetar = (dist <= (magnetar.radius + 500f))

        val lineText = "Curving streams of ionized particles perfectly trace the lines of the unfathomable magnetic field, " +
                "making a spectacular cosmic light show enthralling to anyone unaware of their dangers."

        if (overMagnetar) {
            textPanel!!.addParagraph(
                "As soon as your translate through the jump point, you are put on alert by the sound of a high-pitched " +
                    "buzzing, seeming to emanate from the outside of the ship - the telltale sound of drive bubble failure. " +
                    "You prepare to raise your voice and issue emergency commands, but when you look up from your station, " +
                    "you stop. Half your bridge crew is staring at the viewport, filled with the most terrifying sight in the galaxy - " +
                    "a magnetar. $lineText\n" +
                    "\n" +
                    "Your gawking is interrupted by your senior tactical officer hurriedly approaching with a spooked look in his " +
                    "eyes. \"S-sir-\" he stammers out, before resolving himself - making the spacer sign for \"all seals confirmed safe\". " +
                    "\"We... that, that is a magnetar, sir.\" he says, as if you were unaware. \"-I will... give you a detailed report in a moment, " +
                    "but-\" he snaps his head over behind his shoulder, towards the star, then sighs shakily, repeating the gesture " +
                    "he did moments before. \"We are... right on top of it, sir. If our drive field breaks...\" he stammers. " +
                    "\"W-we will die, sir. We need to get out of here...NOW.\" He slides you the expected tactical report, and after " +
                    "briefly skimming over it, you grimly agree and order an emergency burn away from the star. Immediately."
            )
            textPanel!!.highlightInLastPara("magnetar", "magnetar", "die", "Immediately")
            textPanel!!.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
        } else {
            textPanel!!.addParagraph(
                "You smoothly translate through the jump point. You routinely request fleet status, put up a jump sickness bulletin, " +
                    "and run a sensor sweep - but you notice your bridge crew is performing slower than usual. Irritated, you look " +
                    "up from your station only to see something that makes your heart sink. In the distance, light seconds away, " +
                    "is a magnetar. $lineText Even now, you can hear the creaking of your ship's hull as it strains against the " +
                    "magnetic force of the star." +
                    "\n" +
                    "Your gawking is interrupted by your senior tactical officer delivering a report to your desk. \"I " +
                    "really want to reiterate, sir...\" he taps the datapad for effect. \"This isn't like a black hole. " +
                    "This is a real threat.\"\n" +
                    "Skimming over the datapad, you're inclined to agree with him. You make a mental note to be careful in this system."
            )
            textPanel!!.highlightInLastPara("magnetar", "real threat", "careful in this system")
            textPanel!!.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
        }

        textPanel!!.addParagraph(
            "\n" +
                "Received tactical report on magnetars",
            Misc.getPositiveHighlightColor()
        )
        textPanel!!.highlightInLastPara(Misc.getHighlightColor(), "magnetars")
        options!!.addOption("Continue", "CONTINUE")
    }

    override fun optionSelected(text: String?, optionData: Any?) {
        if (text != null) {
            textPanel!!.addParagraph(text, Global.getSettings().getColor("buttonText"))
        }
        if (optionData == "CONTINUE") {
            dialog!!.dismiss()
            // sanity
            if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_VISITED_MAGNETAR] == true) return

            Global.getSector().intelManager.addIntel(niko_MPC_magnetarIntel())
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_VISITED_MAGNETAR] = true
            Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
            Global.getSoundPlayer().playCustomMusic(1, 1, null)
        }
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
        return
    }

    override fun advance(amount: Float) {
        return
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
        return
    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> {
        return HashMap()
    }
}

class MPC_addDelayedMusicScript(val musicId: String): niko_MPC_baseNikoScript() {
    var timesRan = 0
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return true
    }

    override fun advance(amount: Float) {
        if (timesRan++ < 2) return
        Global.getSoundPlayer().playCustomMusic(1, 1, musicId, true)
        delete()
    }
}
package data.scripts.campaign.magnetar.interactionPlugins

import com.fs.starfarer.api.GameState
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

        val lineText = "Curving streams of ionized particles trace the unfathomable magnetic field, " +
                "birthing a spectacular cosmic light show enthralling to anyone unaware of their dangers."

        if (overMagnetar) {
            textPanel!!.addParagraph(
                "As soon as you translate through the jump point, you are put on alert by the sound of a high-pitched " +
                    "buzzing, seeming to emanate from the outside of the ship - the telltale sound of imminent drive bubble failure. " +
                    "You prepare to raise your voice and issue emergency commands, but when you look up from your station, " +
                    "you stop. Half your bridge crew is staring at the viewport, filled with what could be the most terrifying sight in the galaxy - " +
                    "a magnetar. $lineText\n" +
                    "\n" +
                    "Your gawking is interrupted by your tactical officer hurriedly approaching. " +
                    "A datapad clatters onto your desk, lacking the usual cadence you expect from your crew. Your advisor stands stiff, " +
                    "putting on a strong face - but you can tell from the micro-twitches and the dilated pupils that this is only a professional courtesy." +
                    "\"S-sir,\" your officer begins, \"We... we have translated... directly on top of a magnetar.\" they state with a sub-tone of terror." +
                    "A nod to the datapad shifts your attention, as your tactical officer continues. \"We are... safe for now, sir, but-\" your officer makes the " +
                    "spacer sign for \"All airlocks sealed\", a sort of ward against evil. \"The drive field, if it breaks-\" their voice catches. " +
                    "\"...we... we won't be able to resist the magnetic field.\" Their grim intonation tells you all you need to know, and " +
                    "after reading the tactical report, you grimly agree and order an immediate emergency burn away from the star."

                    /*" \"S-sir-\" your officer stammers out, before resolving themselves - making the spacer sign for \"all seals confirmed safe\". " +
                    "\"We... that, that is a magnetar, sir.\" you hear, as if you were unaware. \"-I will... give you a detailed report in a moment, " +
                    "but-\" their head snaps over their shoulder to the magnetar, then sighs shakily, repeating the gesture " +
                    "he did moments before. \"We are... right on top of it, sir. If our drive field breaks...\" he stammers. " +
                    "\"W-we will die, sir. We need to get out of here...NOW.\" He slides you the expected tactical report, and after " +
                    "briefly skimming over it, you grimly agree and order an emergency burn away from the star. Immediately."*/
            )
            textPanel!!.highlightInLastPara("magnetar", "directly on top of a magnetar", "won't be able to resist the magnetic field", "immediate emergency burn")
            textPanel!!.setHighlightColorsInLastPara(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
        } else {
            textPanel!!.addParagraph(
                "Your fleet smoothly translates through the jump point. You request fleet status, put up a jump sickness bulletin, " +
                    "and run a sensor sweep. Ten seconds later, you're still waiting for it's initiation. Irritated, you look " +
                    "up from your station only to see something that makes your heart sink. In the distance, mere light seconds away, " +
                    "is a magnetar. $lineText Even now, you can hear the creaking of your ship's hull as it strains against the " +
                    "ever-calling magnetic force, trying to bring your fleet ever-closer." +
                    "\n" +
                    "Your gawking is interrupted by your senior tactical officer delivering a report to your desk. Blinking a few " +
                    "times, you shake off your awe, and return to semi-normal duties."
            )
            textPanel!!.highlightInLastPara("magnetar")
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

class MPC_delayedClearMusicScript(): niko_MPC_baseNikoScript() {
    override fun startImpl() {
        Global.getSector().addScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
        Global.getSoundPlayer().playCustomMusic(1, 1, null, false)
        delete()
    }
}
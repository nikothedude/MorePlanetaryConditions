package data.scripts.campaign.magnetar.interactionPlugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript
import data.scripts.utils.SotfMisc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings

class MPC_playerExposedToMagnetarCore: InteractionDialogPlugin {

    enum class Stage {
        INITIAL_DONEBEFORE,
        INITIAL,
        JUMPPREP,

        NONE,
    }

    enum class Options(
        val stages: MutableSet<Stage>,
        val canBeUsedMultipleTimes: Boolean = false
    ) {
        // INITIAL
        INITIAL_CALL_HOME(mutableSetOf(Stage.INITIAL)) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Transmit a goodbye to the core worlds", INITIAL_CALL_HOME)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)

                plugin.textPanel?.addParagraph(
                    "With a hardened expression, you silence your surroundings with a privacy field. Now free from the " +
                        "panicked sounds of your crew, you take a moment to reflect. It was a foolish idea to enter this system, " +
                        "and even more foolish to remain. And now, you are more than likely face your death. Your more rational side quickly " +
                        "banishes these thoughts - but something within you still echoes the sentiment. You resolve to send a message - just in case you " +
                        "can't escape."
                )
                val hasComms = Global.getSector().intelManager.isPlayerInRangeOfCommRelay
                val beginningString = "Messages sent from the abyss are notoriously slow, so you know you'll be able to cancel this if you survive. " +
                        "Taking a deep breath, you turn to your console, and begin setting up a long-range comms channel."
                val differingString = if (hasComms) {
                    "Within a few seconds, the channel is open. Resolving yourself for what might be your final message ever, " +
                    "you lay out your desires and unfinished promises on the channel. You sniffle, mumble, and pause uncomfortably, " +
                    "as the ever encroaching end of your mortality approaches. You end off with a platitude on resolve, hard choices, " +
                    "and the need for the sector to advance, else it will be lost in the dust. You end the transmission with your current coordinates, " +
                    "hoping you make more people aware of the dangers in the abyss."
                } else {
                    "...however... nothing happens. The channel fails to open. ERR::OOR. Your heart sinks as you realize what this means: " +
                    "You're out of comms range. No-one will ever know your last words, and you will die. Alone."
                }
                plugin.textPanel?.addParagraph(beginningString)
                plugin.textPanel?.addParagraph(differingString)

                plugin.textPanel?.addParagraph("With a sniffle, you close your privacy field, having to cover your ears " +
                        "at the sheer volume of the panic around you.")

            }
        },
        CONSULT_TACOPS(mutableSetOf(Stage.INITIAL)) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options?.addOption("Consult tacops", CONSULT_TACOPS)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)
                plugin.textPanel!!.addParagraph(
                    "You approach the shaken officers at the tactical station. It takes a moment for them to stop staring " +
                        "at the approaching magnetar and salute you - though you can tell it isn't as respectful as it usually is."
                )
                plugin.textPanel!!.addParagraph(
                    "You demand a SitRep - the sixth time today - and they obediently nod, beginning to break down your situation. Again."
                )
                plugin.textPanel!!.addParagraph(
                    "The drive field is shattered. Navigation is offline. Jumping is impossible, and to make matters worse -"
                )
                plugin.textPanel!!.addParagraph(
                    "The junior officer gulps. \"We-we're headed straight towards a magnetar, s-sir.\". The officer finishes their worried " +
                        "report by ending that fleetwide morale is plummeting, and multiple ships are at risk for a full-on mutiny - for all the good that would do them."
                )
                plugin.textPanel!!.addParagraph(
                    "Without even commanding them to be at ease, you march back to your station, head racing."
                )
            }
        },
        /*CALL_SIERRA(mutableSetOf(Stage.INITIAL)) { // SOTF
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                if (!niko_MPC_settings.SOTF_enabled) return
                plugin.options!!.addOption("Call sierra", CALL_SIERRA)
                plugin.dialog!!.setOptionColor(CALL_SIERRA, SotfMisc.SIERRA_COLOR)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)
                plugin.textPanel?.addParagraph(
                    ""
                )
            }
        },*/
        TAC_OFFICER_BARGES_IN(mutableSetOf(Stage.INITIAL)) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Continue", TAC_OFFICER_BARGES_IN)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                plugin.stage = Stage.JUMPPREP
                super.execute(plugin, text)
                plugin.textPanel!!.addParagraph(
                    "You ruminate in your seat, hands folded, head racing in both complex maneuvers to escape and your last words. " +
                        "Suddenly, you're jolted up by a hand on your shoulder - your senior tactical officer, eyes wide and wild. \"S-sir!\" they stammer. " +
                        "\"I m-might, have something, sir.\""
                )
                HUSH_ROOM.addToOptions(plugin)
            }
        },

        HUSH_ROOM(mutableSetOf()) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Hush the room", HUSH_ROOM)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)

                plugin.textPanel!!.addParagraph(
                    "You bring your whistle to your lips and blow. A shrill tone fills the room, causing " +
                        "all your crew to fall silent and stare at you, exhaustion and panic clear on their face. You turn to your tactical officer " +
                        "expectantly."
                )
                plugin.textPanel!!.addParagraph(
                    "Your officer turns to the crowd, clearly nervous. \"I-I think I have something\" they stammer, bringing up their datapad. "  +
                    "\"So, we can't jump. We can't move. But...\" he pauses, less for effect and more out of anxiety. " +
                    "They turn to you. \"Captain... have you heard of blind jumping? It's an ancient technique. It's impossible in the " +
                    "bubble of a drive field...\" his tone rises. \"-but, with ours shattered, coupled with the latent energy of the magnetar " +
                    "field, we might be able to break into hyperspace!\""
                )
                plugin.textPanel!!.addParagraph(
                    "You recall tales of risky blind jumps - a breach into hyperspace made, only for ships to emerge halfway across the local cluster -" +
                    "if they even emerge at all. \"However...\" Your murmuring crew falls silent. " +
                    "\"...it is a very risky maneuver. It will wreak havoc on our ships, leaving them heavily damaged, putting them at risk for " +
                    "critical malfunctions\", and...\" he pauses, debating whether to speak, before resolving to a whisper, hiding the secret from your panicked crew. \"...killing some of our crew.\" Your officer leans in. \"Sir... with our current position, we won't h-have, an opportunity for long.\"",
                )
                plugin.textPanel!!.highlightInLastPara("heavily damaged", "critical malfunctions", "killing some of our crew")
                plugin.textPanel!!.setHighlightColorsInLastPara(Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor())

                GIVE_GO_AHEAD.addToOptions(plugin)
            }
        },
        GIVE_GO_AHEAD(mutableSetOf()) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Give the go-ahead", GIVE_GO_AHEAD)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)
                plugin.textPanel!!.addParagraph(
                    "Feeling a slight hope start swelling in your chest, you seize the moment and begin barking orders " +
                    "to your subordinates, who happily fall into line. What was previously a frenzied chatter is no more than carefully " +
                    "coordinated orders, as everyone works together to get themselves out of this mess."
                )
                plugin.textPanel!!.addParagraph(
                    "System after system fall into place. Shield generators polarize to your tacoff's designations, phase " +
                        "coils prepare to project a high-level phase field 2 kilometers ahead. All non-vital systems are disabled, and " +
                        "sensors are supercharged, ready to rip a hole in spacetime, assisted by what was certainly your doom mere moments ago. " +
                        "Soon, everything is ready, and everyone looks to you for the final confirmation."
                )
                DO_BLIND_JUMP.addToOptions(plugin)
            }
        },
        DO_BLIND_JUMP(mutableSetOf()) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Execute the blind jump", DO_BLIND_JUMP)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)

                plugin.textPanel!!.addParagraph(
                    "\"EXECUTE!\""
                )
                plugin.textPanel!!.addParagraph(
                    "Your sensors panel scrambles and alarms as every single sensor pylon fires off at once, " +
                        "and your tactical panel warns you of a fleet-wide shield failure as generators overload to assist the shearing of reality. " +
                        "Soon, a blinding light of indescribable color fills the bridge, as what can only be described as a fracture opens a kilometer out -" +
                        " with your fleet rapidly approaching. "
                )
                Global.getSoundPlayer().playUISound("world_interdict_off", 1f, 1f)
                ENTER_THE_RIFT.addToOptions(plugin)
            }

        },
        ENTER_THE_RIFT(mutableSetOf()) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Let your fleet drift inside", ENTER_THE_RIFT)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)

                plugin.textPanel!!.addParagraph(
                    "You brace yourself - but it isn't enough. The instant your ship enters the rift, it's shredded " +
                        "by the manmade hole in reality. Sparks fly, electronics blow, bulkheads rupture and crew are tossed like ragdolls. " +
                        "When you come to, you've already exited into hyperspace - far, far worse for wear than you were before."
                )

                END.addToOptions(plugin)
            }
        },
        END(mutableSetOf(Stage.INITIAL_DONEBEFORE)) {
            override fun addToOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.addOption("Take stock of your surroundings", END)
            }

            override fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
                super.execute(plugin, text)

                plugin.dialog!!.dismiss()
                plugin.playerFleet?.let {
                    niko_MPC_magnetarStarScript.doBlindJump(it)
                    if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.TIMES_MAGNETAR_PANICKED] == null) {
                        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.TIMES_MAGNETAR_PANICKED] = 0
                    }
                    Global.getSector().memoryWithoutUpdate[niko_MPC_ids.TIMES_MAGNETAR_PANICKED] = (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.TIMES_MAGNETAR_PANICKED] as Int) + 1
                    Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false)
                    Global.getSoundPlayer().playCustomMusic(1, 1,null, true)
                }
            }
        };

        abstract fun addToOptions(plugin: MPC_playerExposedToMagnetarCore)
        open fun shouldAddOption(plugin: MPC_playerExposedToMagnetarCore): Boolean {
            if (!canBeUsedMultipleTimes && plugin.optionsUsed.contains(this)) {
                return false
            }
            if (!stages.contains(plugin.stage)) {
                return false
            }
            return true
        }
        open fun execute(plugin: MPC_playerExposedToMagnetarCore, text: String?) {
            if (text != null) {
                plugin.textPanel!!.addParagraph(text, Global.getSettings().getColor("buttonText"))
            }
            plugin.optionsUsed += this
            plugin.lastOption = this
            addOptions(plugin)
        }

        companion object {
            fun addOptions(plugin: MPC_playerExposedToMagnetarCore) {
                plugin.options!!.clearOptions()
                for (entry in values()) {
                    if (entry.shouldAddOption(plugin)) {
                        entry.addToOptions(plugin)
                    }
                }
            }
        }
    }

    val optionsUsed = HashSet<Options>()
    var lastOption: Options? = null

    protected var dialog: InteractionDialogAPI? = null
    protected var textPanel: TextPanelAPI? = null
    protected var options: OptionPanelAPI? = null
    protected var visual: VisualPanelAPI? = null

    protected var playerFleet: CampaignFleetAPI? = null

    var stage: Stage = Stage.INITIAL

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
            stage = Stage.INITIAL_DONEBEFORE
        }

        createInitialText()
        Options.addOptions(this)

        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true)
        Global.getSoundPlayer().playCustomMusic(1, 1,"music_encounter_mysterious", true)
    }

    private fun createInitialText() {
        if (stage != Stage.INITIAL_DONEBEFORE) {
            textPanel!!.addParagraph(
                "You stare grimly at the approaching magnetar, your viewport tinged red by the emergency lights, trying " +
                        "your best to ignore the frantic scrambling of your subordinates all around you. You've been trying to think of a way " +
                        "out of this, but with your drive field disabled, you can't jump, you can't move, you can't even protect yourself. As " +
                        "the bridge's velocity alarm begins blaring, and frantic communication turns to indecipherable, you ponder your next steps."
            )
        } else {
            textPanel!!.addParagraph(
                "You stare through your viewport at the approaching magnetar, unfazed by it's promise of instant mortality, " +
                    "though heavily irritated at yourself for the coming damages and casualties. Sighing, you turn around to face your " +
                    "tactical officer. With a single hand gesture, the bridge enters a frenzy of activity. Just like before, " +
                    "sensors are overcharged, shields are polarized, phase coils are overtuned. And with a single command, you order " +
                    "the rift opened once more. "
            )
            textPanel!!.addParagraph("The fleet reluctantly enters, though knowing what awaits them if they chose not to " +
                    "enter, they do so with haste. Buckling yourself to your chair, you close your eyes and pretend the carnage " +
                    "ravaging your fleet and assaulting your ears isn't happening - though the screams of both crew and buckling bulkheads make that hard.")
            //Options.END.addToOptions(this)
        }

    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        if (optionData is Options) {
            optionData.execute(this, optionText)
        }
        /*val sierraCMD = rules.getBestMatching(null, "SotfSierraCMD", dialog, memoryMap)
        if (niko_MPC_settings.SOTF_enabled && sierraCMD is SotfSierraCMD) { // dunno if this works or not
            val hasSierra = sierraCMD.execute(null, dialog, Misc.tokenize("checkSierra"), memoryMap)
        }*/
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
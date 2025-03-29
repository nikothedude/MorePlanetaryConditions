package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionIntel.Companion.BASE_BRIBE_VALUE
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionIntel.Companion.BRIBE_REPEAT_MULT_EXP_BASE
import org.lwjgl.input.Keyboard
import kotlin.math.pow

class MPC_IAIICInspectionDialogPluginImpl(
    val intel: MPC_IAIICInspectionIntel,
    val ui: IntelUIAPI
): InteractionDialogPlugin {

    companion object {
        const val HOSTILE_BRIBE_MULT = 1.5f
    }
    lateinit var dialog: InteractionDialogAPI

    private enum class OptionId {
        INIT,
        COMPLY,
        BRIBE,
        RESIST,
        LEAVE,
        CONFIRM,
        CANCEL,
        HIDE
    }

    override fun init(dialog: InteractionDialogAPI?) {
        if (dialog == null) return

        this.dialog = dialog

        dialog.visualPanel.setVisualFade(0.25f, 0.25f)
        //visual.showImagePortion("illustrations", "quartermaster", 640, 400, 0, 0, 480, 300);
        //visual.showImagePortion("illustrations", "quartermaster", 640, 400, 0, 0, 480, 300);
        dialog.visualPanel.showPlanetInfo(intel.target.primaryEntity)

        dialog.setOptionOnEscape("Leave", OptionId.LEAVE)

        optionSelected(null, OptionId.INIT)
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
        return
    }

    override fun advance(amount: Float) {
        return
    }

    protected fun computeBribeAmount(): Int {
        val inspectionTimes = MPC_IAIICInspectionPrepIntel.get()?.inspectionsUndergone ?: 0
        val isHostile = intel.faction.isHostileTo(Factions.PLAYER)
        var base = (BRIBE_REPEAT_MULT_EXP_BASE.pow(inspectionTimes) * BASE_BRIBE_VALUE).toInt()
        if (isHostile) {
            base = (base * HOSTILE_BRIBE_MULT).toInt()
        }
        // no cap, you can get to INSANE numbers
        return base
    }

    protected fun printOptionDesc(orders: MPC_IAIICInspectionOrders, inConfirm: Boolean) {
        val textPanel = dialog.textPanel
        when (orders) {
            MPC_IAIICInspectionOrders.BRIBE -> {
                val bribe = computeBribeAmount()
                textPanel.addPara(
                    "Sufficient funding allocated to proper official and unofficial actors should " +
                            "ensure that the inspection reaches a satisfactory outcome."
                )
                textPanel.addPara(
                    "The scrupulous nature of the IAIIC investigative branch complicates matters. " +
                            "%s are needed, and there is %s to the amount of credits needed in following investigations. ",
                    Misc.getHighlightColor(),
                    "significantly more credits", "no limit"
                )
                val isHostile = intel.faction.isHostileTo(Factions.PLAYER)
                if (inConfirm) {
                    textPanel.addPara(
                        "Once this order is given and the funds and agents dispatched, it can not be " +
                                "rescinded."
                    )
                    val credits: Int = Global.getSector().playerFleet.cargo.credits.get().toInt()
                    var costColor = Misc.getHighlightColor()
                    if (bribe > credits) costColor = Misc.getNegativeHighlightColor()

//				textPanel.addPara("A total of %s should be enough to get the job done. " +
//					"It's more expensive than the cores involved, " +
//					"but guarantees that your standing with the Hegemony will not suffer.", costColor,
//							  	Misc.getDGSCredits(bribe));
                    textPanel.addPara(
                        "A total of %s should be enough to get the job done.", costColor,
                        Misc.getDGSCredits(bribe.toFloat())
                    )
                    if (isHostile) {
                        textPanel.addPara(
                            "Due to your less-than-stellar standing with the IAIIC, the requisite credits are increased by %s.",
                            Misc.getHighlightColor(),
                            "${HOSTILE_BRIBE_MULT}x"
                        )
                    }
                    textPanel.addPara(
                        "You have %s available.", Misc.getHighlightColor(),
                        Misc.getDGSCredits(credits.toFloat())
                    )
                } else {
                    textPanel.addPara(
                        "You've allocated %s to the task and have otherwise committed to this course of action.",
                        Misc.getHighlightColor(), Misc.getDGSCredits(bribe.toFloat())
                    )
                    //				textPanel.addPara("You've allocated %s to the task. Giving different orders will allow you to recover these funds.", Misc.getHighlightColor(),
//						Misc.getDGSCredits(bribe));
                }
            }

            MPC_IAIICInspectionOrders.COMPLY -> {
                textPanel.addPara(
                    "The local authorities will comply with the inspection. This will result " +
                            "in all AI cores being found and confiscated, and will cause your standing with the IAIIC " +
                            "to fall based on the number of AI cores found."
                )
                textPanel.addPara(
                    "If AI cores currently in use are removed or moved off-planet, this activity will " +
                            "surely leave traces for inspectors to find, inspiring them to much greater zeal."
                )
            }
            MPC_IAIICInspectionOrders.RESIST -> {
                textPanel.addPara("All space and ground forces available will resist the inspection.")
                textPanel.addPara(
                    ("If the inspection reaches the surface, " +
                            "the ground defense strength will determine whether " +
                            "they're able to confiscate any AI cores.")
                )
                textPanel.addPara(
                    "The IAIIC will become immediately aware of this the moment you give the order (due to their advanced IntSec), but will " +
                        "choose not to engage in open conflict until \"pushed to their limit\" (that is, their task force encountering resistance)."
                )
            }
        }
    }

    protected fun addChoiceOptions() {
        val options = dialog.optionPanel
        options.clearOptions()
        val curr: MPC_IAIICInspectionOrders = intel.orders
        val isHostile = intel.faction.isHostileTo(Factions.PLAYER)
        if (curr != MPC_IAIICInspectionOrders.BRIBE) {
            if (!isHostile) {
                options.addOption(
                    "Order the local authorities to comply with the inspection",
                    OptionId.COMPLY,
                    null
                )
            }
            options.addOption(
                "Order your local forces to resist the inspection",
                OptionId.RESIST,
                null
            )
            options.addOption(
                "Allocate sufficient funds to bribe or otherwise handle the inspectors",
                OptionId.BRIBE,
                null
            )
            dialog.setOptionColor(OptionId.BRIBE, Misc.getStoryOptionColor())
        }
        options.addOption("Dismiss", OptionId.LEAVE, null)
        options.setShortcut(
            OptionId.LEAVE,
            Keyboard.KEY_ESCAPE,
            false,
            false,
            false,
            true
        )
        if (curr == MPC_IAIICInspectionOrders.COMPLY) {
            options.setEnabled(OptionId.COMPLY, false)
        }
        if (curr == MPC_IAIICInspectionOrders.BRIBE) {
            options.setEnabled(OptionId.BRIBE, false)
        }
        if (curr == MPC_IAIICInspectionOrders.RESIST) {
            options.setEnabled(OptionId.RESIST, false)
        }
    }

    protected var beingConfirmed: MPC_IAIICInspectionOrders? = null
    protected fun addConfirmOptions() {
        if (beingConfirmed == null) return
        val options = dialog.optionPanel
        options.clearOptions()
        printOptionDesc(beingConfirmed!!, true)
        options.addOption("Confirm your orders", OptionId.CONFIRM, null)
        options.addOption("Never mind", OptionId.CANCEL, null)
        options.setShortcut(
            OptionId.CANCEL,
            Keyboard.KEY_ESCAPE,
            false,
            false,
            false,
            true
        )
        if (beingConfirmed == MPC_IAIICInspectionOrders.BRIBE) {
            val bribe = computeBribeAmount()
            if (bribe > Global.getSector().playerFleet.cargo.credits.get()) {
                options.setEnabled(OptionId.CONFIRM, false)
                options.setTooltip(OptionId.CONFIRM, "Not enough credits.")
            }
            SetStoryOption.set(
                dialog,
                1,
                OptionId.CONFIRM,
                "bribeAICoreInspection",
                Sounds.STORY_POINT_SPEND_TECHNOLOGY,
                "Issued bribe to prevent " + intel.faction.displayName + " AI core inspection"
            )
            //			StoryOptionParams params = new StoryOptionParams(OptionId.BRIBE, 1, "bribeAICoreInspection", Sounds.STORY_POINT_SPEND_TECHNOLOGY);
//			SetStoryOption.set(dialog, params, new BaseOptionStoryPointActionDelegate(dialog, params) {
//				@Override
//				public void createDescription(TooltipMakerAPI info) {
//					float opad = 10f;
//					info.setParaInsigniaLarge();
//					info.addPara("Virtually guarantees that the inspection will not find any AI cores.",
//							-opad);
//					info.addSpacer(opad * 2f);
//					addActionCostSection(info);
//				}
//			});
        }
    }

    override fun optionSelected(text: String?, optionData: Any?) {
        if (optionData == null) return
        val option = optionData as OptionId
        if (text != null) {
            //textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
            dialog.addOptionSelectedText(option)
        }
        when (option) {
            OptionId.INIT -> {
                printOptionDesc(intel.orders, false)
                addChoiceOptions()
            }

            OptionId.COMPLY -> {
                beingConfirmed = MPC_IAIICInspectionOrders.COMPLY
                addConfirmOptions()
            }

            OptionId.BRIBE -> {
                beingConfirmed = MPC_IAIICInspectionOrders.BRIBE
                addConfirmOptions()
            }

            OptionId.RESIST -> {
                beingConfirmed = MPC_IAIICInspectionOrders.RESIST
                addConfirmOptions()
            }

            OptionId.CONFIRM -> {
                val invested: Int = intel.investedCredits
                if (invested > 0) {
                    AddRemoveCommodity.addCreditsGainText(invested, dialog.textPanel)
                    Global.getSector().playerFleet.cargo.credits.add(invested.toFloat())
                    intel.investedCredits = 0
                }
                intel.orders = beingConfirmed!!
                if (beingConfirmed == MPC_IAIICInspectionOrders.BRIBE) {
                    val bribe = computeBribeAmount()
                    intel.investedCredits = bribe
                    AddRemoveCommodity.addCreditsLossText(bribe, dialog.textPanel)
                    Global.getSector().playerFleet.cargo.credits.subtract(bribe.toFloat())
                } else {
                }
                addChoiceOptions()
            }

            OptionId.CANCEL -> addChoiceOptions()
            OptionId.LEAVE -> leave()
            else -> {}
        }
    }

    protected fun leave() {
        dialog.dismiss()
        ui.updateUIForItem(intel)
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {

    }

    override fun getContext(): Any? {
        return null
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? = null
}
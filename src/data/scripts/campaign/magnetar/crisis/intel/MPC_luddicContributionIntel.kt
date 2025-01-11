package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color

open class MPC_luddicContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_luddicContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_luddicContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_luddicContributionIntel
        }

        const val KEY = "\$MPC_patherContributionIntel"
        const val SECT_NAME = "Arrow of Ludd"
    }

    enum class State {
        FIND_PATHER,
        GO_TO_HIDEOUT,
        HAND_OVER_MARKET,
        DONE,
        FAILED,
    }
    var state: State = State.FIND_PATHER

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.LUDDIC_PATH).crest
    }

    override fun getName(): String = "Pather involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.LUDDIC_PATH, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is String) {
            info.addPara(listInfoParam as String, initPad)
        }
        if (listInfoParam is State) {
            val hideout = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideoutName"] as? String
            when (listInfoParam) {
                State.GO_TO_HIDEOUT -> {
                    info.addPara(
                        "Go to %s", initPad, Misc.getHighlightColor(), hideout
                    )
                }
                State.HAND_OVER_MARKET -> {
                    info.addPara(
                        "Give the %s a %s, %s and %s",
                        initPad,
                        Misc.getHighlightColor(),
                        SECT_NAME, "orbital works", "military base", "heavy batteries"
                    ).setHighlightColors(factionForUIColors.baseUIColor, Misc.getHighlightColor(), Misc.getHighlightColor())
                    info.addPara(
                        "Additional conditions available in %s",
                        0f,
                        Misc.getHighlightColor(),
                        "intel"
                    )
                }
                State.FAILED -> {
                    info.addPara("Failed", initPad)
                }
                State.DONE -> {
                    info.addPara(
                        "Success", initPad
                    )
                }
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_PATH)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that pathers may be involved in the IAIIC.",
            5f
        )

        when (state) {
            State.FIND_PATHER -> {
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) {
                    val label = info.addPara(
                        "You've learnt about the %s: A disorganized group of pathers supposedly providing covert support to the %s.",
                        5f,
                        Misc.getHighlightColor(),
                        Global.getSector().memoryWithoutUpdate.getString(niko_MPC_ids.PATHER_SECT_NAME), "IAIIC"
                    )
                    label?.setHighlightColors(
                        Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                        Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                    )

                    info.addPara(
                        "They supposedly hide out in %s bars.",
                        5f,
                        Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                        "Luddic Path"
                    )
                }
            }
            State.GO_TO_HIDEOUT -> {
                val hideout = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideoutName"] as? String
                info.addPara(
                    "A pather has given you a set of coordinates on %s, and instructed you to travel there.", 0f,
                    Misc.getHighlightColor(),
                    hideout
                )
            }
            State.HAND_OVER_MARKET -> {
                val hideout = Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideoutName"] as? String
                info.addPara(
                    "A representative of the %s on %s has offered to help remove %s influence from the %s, but requires " +
                    "a \"favor\" first.",
                    5f,
                    Misc.getHighlightColor(),
                    SECT_NAME, hideout, "pather", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor, Misc.getHighlightColor(), factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
                info.addPara(
                    "You must provide a %s with the %s:",
                    5f,
                    Misc.getHighlightColor(),
                    "colony", "following requirements"
                )
                info.setBulletedListMode(BULLET)
                info.addPara("Orbital works", 0f)
                info.addPara("Military base", 0f)
                info.addPara("Heavy batteries", 0f)
                info.addPara("Megaport", 0f)
                info.addPara("Megaport must be %s", 0f, Misc.getStoryOptionColor(), "improved")
                //info.addPara("Waystation", 0f)
                info.addPara("Battlestation", 0f)
                info.addPara("%s, %s, %s or %s must have a %s", 0f, Misc.getHighlightColor(), "Megaport", "Heavy Batteries", "Orbital works", "Military Base", "special item")
                info.addPara("In-system %s", 0f, Misc.getHighlightColor(), "comms relay")
                info.addPara("No AI cores", 0f)
                info.addPara("No disrupted industries", 0f)
                info.addPara("No more than %s unrest", 0f, Misc.getHighlightColor(), "one")
                info.addPara("No less than %s stability", 0f, Misc.getHighlightColor(), MPC_IAIICPatherCMD.MIN_STABILITY.toInt().toString())
                info.addPara("System must not be %s", 0f, Misc.getHighlightColor(), "dangerous")
                info.setBulletedListMode(null)

                info.addPara(
                    "To help with your endeavours, the pathers on %s will %s and %s if you choose to colonize it.",
                    5f,
                    Misc.getHighlightColor(),
                    hideout, "accelerate construction times", "provide an additional industry slot"
                )
                info.addPara(
                    "Unfortunately, this will come at the cost of %s, as capital is overused by the populace (and funnelled to the \"holy war effort\").",
                    0f,
                    Misc.getHighlightColor(),
                    "income"
                )

                info.addPara(
                    "Once you are finished, you should return to %s and speak to the representative.",
                    5f,
                    Misc.getHighlightColor(),
                    hideout
                )
            }
            State.FAILED -> {
                info.addPara("You have failed to drive a wegde between the IAIIC and the pathers.", 5f)
            }
            State.DONE -> {
                info.addPara("You have successfully armed the %s with heavy industry, and in doing so, drove a wedged between them and the %s.",
                5f,
                Misc.getHighlightColor(),
                "path", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null

        (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI)?.makeUnimportant("\$MPC_IAIICPatherHideout")
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        if (state.ordinal >= State.GO_TO_HIDEOUT.ordinal) {
            return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI
        }
        return null
    }
}
package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD.Companion.MIN_STABILITY
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.getStationIndustry
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color

open class MPC_patherContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_patherContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_patherContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_patherContributionIntel
        }

        const val KEY = "\$MPC_patherContributionIntel"
        const val SECT_NAME = "Arrow of Ludd"
    }

    enum class Requirement {
        SAFE {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"Is this some kind of joke? Your *tribute* is in a hostile environment. We will never accept this offer. Pick another planet.\"")
                text.setFontSmallInsignia()
                text.addPara("Your target colony is in a unsafe system. This can happen due to black holes, pulsars, or an otherwise hostile environment.")
                text.setFontInsignia()
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                if (market.containingLocation.hasTag(Tags.THEME_UNSAFE)) return false
                if (market.starSystem.hasBlackHole() || market.starSystem.hasPulsar()) return false
                return true
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "System must be %s (No black holes, pulsars, etc.)",
                    0f,
                    Misc.getHighlightColor(),
                    "safe"
                )
            }
        },
        NO_DISRUPTION {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"You have non-functional industries. We will wait.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return (market.industries.all { it.isFunctional })
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "No %s",
                    0f,
                    Misc.getHighlightColor(),
                    "disrupted industries"
                )
            }
        },
        STABILITY {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"Your tribute is unstable. Bring its stability to at least ${MIN_STABILITY.toInt()}.\"").setHighlight("${MIN_STABILITY.toInt()}")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return (market.stabilityValue >= MIN_STABILITY)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "At least %s",
                    0f,
                    Misc.getHighlightColor(),
                    "${MIN_STABILITY.toInt()} stability"
                )
            }
        },
        COMM_RELAY {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"You have no in-system comms relay.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                if (market.containingLocation == null) return false
                if (market.containingLocation?.getCustomEntitiesWithTag(Tags.COMM_RELAY)?.isNotEmpty() != true) return false
                return true
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "An in-system %s",
                    0f,
                    Misc.getHighlightColor(),
                    "comms relay"
                )
            }
        },
        NO_AI_CORES {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"Remove your Mammonian slaves - your AI cores.\" ${person.heOrShe} grimaces.")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return (market.industries.all { it.aiCoreId == null } && !market.admin.isAICore)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "No %s",
                    0f,
                    Misc.getHighlightColor(),
                    "AI cores"
                )
            }
        },
        MEGAPORT {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"We require a megaport.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return market.hasIndustry(Industries.MEGAPORT)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara("A %s", 0f, Misc.getHighlightColor(), "megaport")
            }

        },
        IMPROVED_MEGAPORT {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"Your megaport must be improved.\"").setHighlight("megaport", "improved")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return market.getIndustry(Industries.MEGAPORT)?.isImproved == true
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara("%s must be %s", 0f, Misc.getHighlightColor(), "Megaport", "Improved").setHighlightColors(Misc.getHighlightColor(), Misc.getStoryOptionColor())
            }
        },
        ORBITALWORKS {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"We require orbital works.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return market.hasIndustry(Industries.ORBITALWORKS)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara("A %s", 0f, Misc.getHighlightColor(), "orbital works")
            }

        },
        HEAVYBATTERIES {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"We require heavy batteries.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return market.hasIndustry(Industries.HEAVYBATTERIES)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara("%s", 0f, Misc.getHighlightColor(), "Heavy batteries")
            }

        },
        MILITARY {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"We require military infrastructure.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return market.hasIndustry(Industries.MILITARYBASE) || market.hasIndustry(Industries.HIGHCOMMAND)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara("A %s or %s", 0f, Misc.getHighlightColor(), "military base", "high command")
            }

        },
        STATION {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara("\"We require a battlestation, or a star fortress.\"")
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return (market.getStationIndustry()?.spec?.hasTag(Industries.TAG_BATTLESTATION) == true || market.getStationIndustry()?.spec?.hasTag(Industries.TAG_STARFORTRESS) == true)
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "A %s or %s",
                    0f,
                    Misc.getHighlightColor(),
                    "battlestation", "star fortress"
                )
            }
        },
        SPECIAL_ITEM {
            override fun printUnsatisfiedText(
                text: TextPanelAPI,
                person: PersonAPI
            ) {
                text.addPara(
                    "\"We require a %s on either your %s, %s, %s or %s.\""
                ).setHighlight(
                    "special item",
                    "ground defenses",
                    "megaport",
                    "military infrastructure",
                    "orbital works"
                )
            }

            override fun isSatisfied(market: MarketAPI): Boolean {
                return !(market.getIndustry(Industries.ORBITALWORKS)?.specialItem == null &&
                    market.getIndustry(Industries.HEAVYBATTERIES)?.specialItem == null &&
                    market.getIndustry(Industries.MEGAPORT)?.specialItem == null &&
                    (market.getIndustry(Industries.MILITARYBASE) != null && market.getIndustry(Industries.MILITARYBASE).specialItem == null) &&
                    (market.getIndustry(Industries.HIGHCOMMAND) != null && market.getIndustry(Industries.HIGHCOMMAND).specialItem == null)
                )
            }

            override fun printIntelText(info: TooltipMakerAPI) {
                info.addPara(
                    "%s on either %s, %s, %s, or %s",
                    0f,
                    Misc.getHighlightColor(),
                    "special item", "ground defenses", "megaport", "military base", "orbital works"
                )
            }

        };

        abstract fun printUnsatisfiedText(text: TextPanelAPI, person: PersonAPI)
        abstract fun isSatisfied(market: MarketAPI): Boolean
        abstract fun printIntelText(info: TooltipMakerAPI)
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
        return (super.getIntelTags(map) + MPC_indieContributionIntel.getBaseContributionTags() + Factions.LUDDIC_PATH).toMutableSet()
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
                Requirement.entries.forEach { it.printIntelText(info) }
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
                info.addPara("You have failed to drive a wedge between the IAIIC and the pathers.", 5f)
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
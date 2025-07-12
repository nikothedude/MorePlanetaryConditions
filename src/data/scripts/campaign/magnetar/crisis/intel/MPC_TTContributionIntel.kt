package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.quest.MPC_magnetarQuest
import data.utilities.niko_MPC_ids
import java.awt.Color

class MPC_TTContributionIntel: BaseIntelPlugin() {

    var activePerson: PersonAPI? = null
    var evidence: Int = 0
    var state: State = State.FIND_EVIDENCE

    enum class State {
        FIND_EVIDENCE,
        FIND_CACHE,
        RETURN_TO_CONTACT,
        WAIT,
        RESOLVE,
        PAY_UP,
        OVER;

        open fun apply() {}
        open fun unapply() {}
    }
    enum class PirateTransportState {
        HAS_PIRATE,
        FAILED,
        DELIVERED,
        FOUND_CACHE;
    }

    companion object {
        fun get(withUpdate: Boolean = false): MPC_TTContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_TTContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_TTContributionIntel
        }

        const val KEY = "\$MPC_TTContributionIntel"
        const val EVIDENCE_NEEDED = 3
    }

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.TRITACHYON).crest
    }

    override fun getName(): String = "Tri-Tachyon involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + MPC_indieContributionIntel.getBaseContributionTags() + Factions.TRITACHYON).toMutableSet()
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is State) {
            when (listInfoParam) {
                State.FIND_CACHE -> {
                    val system = Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystemName"] as? String ?: return
                    info.addPara(
                        "Find the cache in %s", 0f, Misc.getHighlightColor(), system
                    )
                }
                State.RETURN_TO_CONTACT -> {
                    info.addPara("Cache acquired", 0f)
                    info.addPara(
                        "Return to %s",
                        5f,
                        activePerson?.faction?.baseUIColor,
                        activePerson?.name?.fullName
                    )
                }
                State.WAIT -> {
                    info.addPara(
                        "Wait %s", 0f, Misc.getHighlightColor(), "one month"
                    )
                }
                State.RESOLVE -> {
                    info.addPara(
                        "Return to %s",
                        5f,
                        activePerson?.faction?.baseUIColor,
                        activePerson?.name?.fullName
                    )
                }
                State.PAY_UP -> {
                    val label = info.addPara(
                        "Pay %s alpha cores",
                        5f,
                        Misc.getHighlightColor(),
                        "three"
                    )
                }
            }
        }
        if (listInfoParam is String) {
            info.addPara(listInfoParam as String, initPad)
        }
        if (listInfoParam is PirateTransportState) {
            val person = MPC_People.getImportantPeople()[MPC_People.DONN_PIRATE]
            when (listInfoParam) {
                PirateTransportState.HAS_PIRATE -> {
                    val label = info.addPara(
                        "Transport %s to %s",
                        0f,
                        Misc.getHighlightColor(),
                        "${person?.name?.fullName}", "Gilead"
                    )
                    label.setHighlightColors(
                        person?.faction?.baseUIColor,
                        Global.getSector().economy.getMarket("gilead")?.faction?.baseUIColor
                    )
                }
            }
        }
        if (listInfoParam is EvidenceState) {
            when (listInfoParam) {
                EvidenceState.GOT_ANOTHER -> {
                    info.addPara(
                        "Evidence: %s/%s",
                        0f,
                        Misc.getHighlightColor(),
                        "$evidence", "$EVIDENCE_NEEDED"
                    )
                }
                EvidenceState.FINISHED -> {
                    info.addPara(
                        "Evidence: %s/%s",
                        0f,
                        Misc.getHighlightColor(),
                        "$evidence", "$EVIDENCE_NEEDED"
                    )
                    info.addPara(
                        "Return to %s",
                        5f,
                        activePerson?.faction?.baseUIColor,
                        activePerson?.name?.fullName
                    )
                }
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.TRITACHYON)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        val person = activePerson ?: return
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_startedSearchForTTBM")) {
            val label = info.addPara(
                "%s has agreed to help you learn about %s's involvement with the %s, but you need to do a favor for ${person.himOrHer} first.",
                5f,
                Misc.getHighlightColor(),
                "${person.name.fullName}", "Tri-Tachyon", "IAIIC"
            )
            label.setHighlightColors(person.faction.baseUIColor, Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor, Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor)

            when (state) {
                State.FIND_EVIDENCE -> {
                    val labelThree = info.addPara(
                        "Currently, you need to %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "search for clues as to a data cache's location"
                    )

                    val labelTwo = info.addPara(
                        "%s suggested you search around the %s system, but not on %s, due to security concerns.",
                        5f,
                        Misc.getHighlightColor(),
                        "${person.name.fullName}", "Hybrasil", "Tri-Tachyon planets"
                    )

                    labelTwo.setHighlightColors(
                        person.faction.baseUIColor,
                        Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor,
                        Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor
                    )

                    info.addPara(
                        "You will likely find hints on inhabited planets and (possibly) system objectives such as comm relays.",
                        5f
                    )

                    if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_foundPirateBMContactInBar") && !(Global.getSector().memoryWithoutUpdate.getBoolean(
                            "\$MPC_deliveredPirateToGilead"
                        ) || Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_handedOverPirate"))) {
                        val person = MPC_People.getImportantPeople()[MPC_People.DONN_PIRATE]
                        val pirLabel = info.addPara(
                            "You are currently transporting %s to %s in hopes of %s.",
                            5f,
                            Misc.getHighlightColor(),
                            "${person?.name?.fullName}", "Gilead", "learning what they know"
                        )
                        pirLabel.setHighlightColors(
                            person?.faction?.baseUIColor,
                            Global.getSector().economy.getMarket("gilead")?.faction?.baseUIColor,
                            Misc.getHighlightColor()
                        )

                        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_acceptedDownPayment")) {
                            val pirLabel2 = info.addPara(
                                "You've accepted a down payment of %s from %s, and will be expected to return it once %s tells you what they know.",
                                0f,
                                Misc.getHighlightColor(),
                                "${person?.name?.fullName}",
                                "${Global.getSector().memoryWithoutUpdate["\$MPC_downPaymentDGS"]}",
                                "${person?.name?.fullName}"
                            )
                            pirLabel2.setHighlightColors(
                                person?.faction?.baseUIColor,
                                Misc.getHighlightColor(),
                                person?.faction?.baseUIColor,
                            )
                        }
                    }

                    if (evidence >= EVIDENCE_NEEDED) {
                        info.addPara(
                            "You have accumulated all the evidence you need. You should return to %s.",
                            5f,
                            person.faction.baseUIColor,
                            person.name.fullName
                        )
                    } else if (evidence > 0) {
                        val label4 = info.addPara(
                            "You have %s/%s pieces of evidence.",
                            5f,
                            Misc.getHighlightColor(),
                            "$evidence", "$EVIDENCE_NEEDED"
                        )
                    }
                }
                State.FIND_CACHE -> {
                    val system = Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystemName"] as? String ?: return
                    val label = info.addPara(
                        "%s has identified %s as a possible location for the %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "${person.nameString}", system, "Data Cache"
                    )
                    label.setHighlightColors(
                        person.faction.baseUIColor, Misc.getHighlightColor(), Misc.getHighlightColor()
                    )
                }
                State.RETURN_TO_CONTACT -> {
                    val label = info.addPara(
                        "You've obtained the %s, and now must return it to %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "data cache", person.nameString
                    )
                    label.setHighlightColors(Misc.getHighlightColor(), person.faction.baseUIColor)
                }
                State.WAIT -> {
                    info.addPara(
                        "You are currently waiting for %s to finish 'negotiating'.",
                        5f,
                        person.faction.baseUIColor,
                        person.nameString
                    )
                }
                State.RESOLVE -> {
                    info.addPara(
                        "You've received a summons from %s.",
                        5f,
                        person.faction.baseUIColor,
                        person.nameString
                    )
                }
                State.PAY_UP -> {
                    val label2 = info.addPara(
                        "Despite having retrieved the %s for %s, you're still expected to pay %s for %s to pull out of the %s.",
                        5f,
                        Misc.getHighlightColor(),
                        "data cache", person.nameString, "three alpha cores", "Tri-Tachyon", "IAIIC"
                    )
                    label2.setHighlightColors(
                        Misc.getHighlightColor(),
                        person.faction.baseUIColor,
                        Misc.getHighlightColor(),
                        Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor,
                        Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                    )
                }
                State.OVER -> {
                    val label2 = info.addPara(
                        "You've successfully convinced %s to pull out of the %s, and made %s significantly more wealthy as a result.",
                        5f,
                        Misc.getHighlightColor(),
                        "Tri-Tachyon", "IAIIC", person.nameString
                    )
                    label.setHighlightColors(
                        Global.getSector().getFaction(Factions.TRITACHYON).baseUIColor,
                        Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                        person.faction.baseUIColor
                    )
                }
            }
        }
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_recoveredBMCache")) return null
        if (Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystem"] == null) return null

        return (Global.getSector().memoryWithoutUpdate["\$MPC_BMcacheSystem"] as StarSystemAPI).hyperspaceAnchor
    }

    enum class EvidenceState {
        GOT_ANOTHER,
        FINISHED
    }

    fun incrementEvidence(text: TextPanelAPI? = null) {
        var state = EvidenceState.GOT_ANOTHER
        evidence++
        if (evidence >= EVIDENCE_NEEDED) {
            state = EvidenceState.FINISHED
            Global.getSector().memoryWithoutUpdate["\$MPC_evidenceDone"] = true
        }
        if (text != null) {
            sendUpdateIfPlayerHasIntel(state, text)
        } else {
            sendUpdateIfPlayerHasIntel(state, false, false)
        }
    }
}
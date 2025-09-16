package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.MPC_incomeTallyListener
import data.utilities.niko_MPC_ids
import lunalib.lunaExtensions.getMarketsCopy
import java.awt.Color

class MPC_indieContributionIntel: BaseIntelPlugin() {
    companion object {
        fun get(withUpdate: Boolean = false): MPC_indieContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_indieContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_indieContributionIntel
        }

        const val KEY = "\$MPC_indieContributionIntel"
        const val TACTISTAR_MAX_INCOME_MULT = 1.2f //1.2x your max income over the last 6 months
        const val TACTISTAR_SUPPORT_INCOME_MULT = 2.6f
        const val PIECES_OF_EVIDENCE_NEEDED_FOR_VOIDSUN = 3

        fun getTactistarExitPrice(): Float {
            var amount = Global.getSector().memoryWithoutUpdate.getFloat("\$MPC_IAIICTactistarExitAmnt")
            if (amount == 0f) {
                amount = (MPC_incomeTallyListener.MPC_incomeTally.get(true)!!.getHighestIncome() * TACTISTAR_MAX_INCOME_MULT).coerceAtLeast(1000000f)
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICTactistarExitAmnt"] = amount
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICTactistarExitAmntDGS"] = Misc.getDGSCredits(amount)
            }
            return amount
        }

        fun getTactistarJoinPrice(): Float {
            var amount = Global.getSector().memoryWithoutUpdate.getFloat("\$MPC_IAIICTactistarSupportAmnt")
            if (amount == 0f) {
                amount = (MPC_incomeTallyListener.MPC_incomeTally.get(true)!!.getHighestIncome() * TACTISTAR_SUPPORT_INCOME_MULT).coerceAtLeast(2000000f)
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICTactistarSupportAmnt"] = amount
                Global.getSector().memoryWithoutUpdate["\$MPC_IAIICTactistarSupportAmntDGS"] = Misc.getDGSCredits(amount)
            }
            return amount
        }

        fun pickVoidsunPlanet(): MarketAPI {
            val playerMarkets = Global.getSector().getFaction(Factions.PLAYER).getMarketsCopy().shuffled()

            return playerMarkets.firstOrNull { it.isFreePort } ?: playerMarkets.random()
        }

        fun getVoidsunPlanet(): MarketAPI? {
            return Global.getSector().economy.getMarket(Global.getSector().memoryWithoutUpdate.getString("\$MPC_IAIICvoidsunPlanetId")) as? MarketAPI
        }

        fun getBaseContributionTags(): MutableSet<String> {
            return mutableSetOf<String>(niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)
        }
    }

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.INDEPENDENT).crest
    }

    override fun getName(): String = "Independent involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + MPC_indieContributionIntel.getBaseContributionTags() + Factions.INDEPENDENT).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is String) {
            when (listInfoParam) {
                "test" -> info.addPara("a", 5f)
                "voidsunBegin" -> {
                    info.addPara(
                        "Go to %s",
                        5f,
                        Global.getSector().playerFaction.baseUIColor,
                        getVoidsunPlanet()?.name
                    )
                }
                "VS_GO_TO_RELAY_PLACE" -> {
                    val targetSysId = Global.getSector().memoryWithoutUpdate.getString("\$MPC_voidsunRelaySysId")
                    val targetSys = Global.getSector().getStarSystem(targetSysId) ?: return
                    info.addPara(
                        "Go to %s",
                        5f,
                        Misc.getHighlightColor(),
                        targetSys.name
                    )
                }
                "VS_GOT_EVIDENCE" -> {
                    val voidsunPlanet = getVoidsunPlanet() ?: return
                    info.addPara(
                        "Obtained evidence - return to %s",
                        5f,
                        Global.getSector().playerFaction.baseUIColor,
                        voidsunPlanet.name
                    )
                }
                else -> {info.addPara(listInfoParam as? String, initPad)}
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.INDEPENDENT)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "While the major factors provide incredible power to the IAIIC, smaller independent states are also a significant " +
                "backing force behind it.",
            5f
        )
        //info.addSectionHeading("Benefactors", Alignment.MID, 5f)

        addContributionInfo(info)

    }

    private fun addContributionInfo(info: TooltipMakerAPI) {
        val fobIntel = MPC_IAIICFobIntel.get() ?: return

        val tactistarContrib = fobIntel.getContributionById("tactistar")
        if (tactistarContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("Tactistar", Alignment.MID, 5f)
            val culann = Global.getSector().economy.getMarket("culann")
            info.addPara(
                "%s has been hired to provide military support. You can visit them on %s.",
                0f,
                Misc.getHighlightColor(),
                "Tactistar",
                "Culann"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                culann.faction.baseUIColor
            )
            if (tactistarContrib.custom == "GIVEN_DEAL") {
                val price = Misc.getDGSCredits(getTactistarExitPrice())
                val priceTwo = Misc.getDGSCredits(getTactistarJoinPrice())
                info.addPara(
                    "You must pay %s for Tactistar to pull out - or %s for them to join you.",
                    0f,
                    Misc.getHighlightColor(),
                    price,
                    priceTwo
                )
            }
        }
        val hammerContrib = fobIntel.getContributionById("thehammer")
        val baetis = Global.getSector().economy.getMarket("baetis")
        if (hammerContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("The Hammer", Alignment.MID, 5f)
            info.addPara(
                "\"%s\", a luddite mercenary company, is likely providing military support. They are stationed on %s.",
                0f,
                Misc.getHighlightColor(),
                "The Hammer", "Baetis"
            ).setHighlightColors(
                Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                baetis.faction.baseUIColor
            )

            if (hammerContrib.custom == "TOLD_TO_GO_AWAY") {
                info.addPara(
                    "The mercenaries have seemingly sworn an oath of war against you, and nothing short of brute force will fix this. You must %s.",
                    0f,
                    Misc.getNegativeHighlightColor(),
                    "wipe them out"
                )
            }
        }

        val blackknifeContrib = fobIntel.getContributionById("blackknife")
        val qaras = Global.getSector().economy.getMarket("qaras")
        if (blackknifeContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("Blackknife", Alignment.MID, 5f)
            info.addPara(
                "\"%s\", a pirate merc group, has been recently implicated in numerous covert operations in your space. They are likely with the IAIIC, and are stationed on %s.",
                0f,
                Misc.getHighlightColor(),
                "Blackknife", "Qaras"
            ).setHighlightColors(
                Global.getSector().getFaction(Factions.PIRATES).baseUIColor,
                qaras.faction.baseUIColor
            )

            if (blackknifeContrib.custom == "GO_KILL_GUY") {
                info.addPara(
                    "Apparently, blackknife is willing to cease their operations - or at least make them far less effective - if you kill" +
                        "a Persean League officer scavenging the %s system. They're likely orbiting a planet with ruins.",
                    0f,
                    Misc.getHighlightColor(),
                    "${Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetSysName"]}"
                )
            }
        }

        val MMCContrib = fobIntel.getContributionById("mmmc")
        val maxios = Global.getSector().economy.getMarket("new_maxios")
        if (MMCContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("MMMC", Alignment.MID, 5f)
            info.addPara(
                "Many hulls tagged as %s have been spotted in IAIIC fleets. You should visit %s to have a talk with them.",
                0f,
                Misc.getHighlightColor(),
                "Mikhael Magec Manufacturing", "Nova Maxios"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                maxios.faction.baseUIColor
            )

            if (MMCContrib.custom == "TOLD_OFF") {
                info.addPara(
                    "The MMMC is unwilling to break the contract, so you must \"convince\" them to do so. A disruption of Nova Maxios' %s for at least %s should suffice.",
                    0f,
                    Misc.getHighlightColor(),
                    "heavy industry",
                    "ninety days"
                )
            }
        }

        val voidsunContrib = fobIntel.getContributionById("voidsun")
        val voidplanet = getVoidsunPlanet()
        if (voidsunContrib?.addBenefactorInfo == true && voidplanet != null) {
            info.addSectionHeading("Voidsun", Alignment.MID, 5f)
            info.addPara(
                "%s, a mercenary company stationed on %s, has potentially been compromised by the IAIIC. You should investigate.",
                0f,
                Misc.getHighlightColor(),
                "Voidsun", voidplanet.name
            ).setHighlightColors(
                Misc.getHighlightColor(),
                voidplanet.faction.baseUIColor
            )

            if (voidsunContrib.custom is Pair<*, *>) {
                val infoGot = voidsunContrib.custom as Pair<String, Int>
                val string = infoGot.first
                val num = infoGot.second
                info.addPara(
                    "Involvement is more than likely - but you have little proof. Unless you want a major diplomatic incident, you should " +
                            "%s - search comm relays, inquire into your government, etc.",
                    0f,
                    Misc.getHighlightColor(),
                    "find evidence",
                )
                info.setBulletedListMode(BaseIntelPlugin.BULLET)
                info.addPara(
                    "%s/%s pieces of evidence found",
                    0f,
                    Misc.getHighlightColor(),
                    "$PIECES_OF_EVIDENCE_NEEDED_FOR_VOIDSUN", "$PIECES_OF_EVIDENCE_NEEDED_FOR_VOIDSUN"
                )
                info.setBulletedListMode(null)

                if (num >= PIECES_OF_EVIDENCE_NEEDED_FOR_VOIDSUN) {
                    info.addPara(
                        "You have sufficient evidence to arrest the mercenary company without %s.",
                        0f,
                        Misc.getHighlightColor(),
                        "diplomatic consequences"
                    )
                }
            } else if (voidsunContrib.custom == "GO_TO_COMMS_RELAY") {
                val sysName = voidplanet.starSystem.name
                info.addPara(
                    "The %s %s has potentially been compromised by Voidsun. You should investigate.",
                    0f,
                    Misc.getHighlightColor(),
                    sysName, "comms relay"
                ).setHighlightColors(
                    voidplanet.faction.baseUIColor,
                    Misc.getHighlightColor()
                )

                if (voidplanet.containingLocation.customEntities.none { it.hasTag(Tags.COMM_RELAY) }) {
                    info.addPara(
                        "You will need to construct a comm relay to progress this quest.",
                        0f
                    ).color = Misc.getGrayColor()
                }
            } else if (voidsunContrib.custom == "GO_TO_EXTERNAL_RELAY") {
                val sysName = voidplanet.starSystem.name
                val targetSysId = Global.getSector().memoryWithoutUpdate.getString("\$MPC_voidsunRelaySysId")
                val targetSys = Global.getSector().getStarSystem(targetSysId) ?: return

                info.addPara(
                    "You've found a FTL relay on the $sysName comms relay, that has seemingly been directing specific traffic to somewhere in %s. You'll likely find it near a %s.",
                    0f,
                    Misc.getHighlightColor(),
                    targetSys.name, "stable location"
                )
            } else if (voidsunContrib.custom == "GOT_EVIDENCE") {
                info.addPara(
                    "You found damning evidence of the IAIIC's involvement with Voidsun. You may now arrest them freely.",
                    0f
                )
            }
        }

        val ailmarContrib = fobIntel.getContributionById("ailmar")
        val ailmar = Global.getSector().economy.getMarket("ailmar")
        if (ailmarContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("Ailmar", Alignment.MID, 5f)
            info.addPara(
                "%s, famous for it's almost mercenary nature, has possibly struck a deal with the IAIIC for protection.",
                0f,
                Misc.getHighlightColor(),
                "Ailmar"
            ).setHighlightColors(
                ailmar.faction.baseUIColor
            )

            if (ailmarContrib.custom == "GIVEN_DEAL") {
                info.addPara(
                    "Ailmar is unwilling to divest from the IAIIC as long as they are at risk from the League. You could offer them your protection instead.",
                    0f
                )
            }
        }

        val agreusContrib = fobIntel.getContributionById("agreus")
        val agreus = Global.getSector().economy.getMarket("agreus")
        if (agreusContrib?.addBenefactorInfo == true) {
            info.addSectionHeading("IIT&S", Alignment.MID, 5f)
            info.addPara(
                "%s, stationed on %s, has been implicated in a large amount of metal outflow towards the IAIIC.",
                0f,
                Misc.getHighlightColor(),
                "Ibrahim Interstellar Transport and Salvage",
                "Agreus"
            ).setHighlightColors(
                Misc.getHighlightColor(),
                agreus.faction.baseUIColor
            )

            if (agreusContrib.custom == "GIVEN_DEAL_GET_SHIP") {
                info.addPara(
                    "Ibrahim has refused to help unless you do her a favor - retrieving her long lost ship, the %s.",
                    0f,
                    Misc.getHighlightColor(),
                    "HMS Hamatsu"
                )
            }
            if (agreusContrib.custom == "GIVEN_DEAL_GET_INFO") {
                info.addPara(
                    "Ibrahim requires proof of IAIIC involvement - maybe search a %s.",
                    0f,
                    Misc.getHighlightColor(),
                    "comms relay"
                )
            }
        }
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        val fobIntel = MPC_IAIICFobIntel.get() ?: return null
        val blackknifeContrib = fobIntel.getContributionById("blackknife")
        if (blackknifeContrib?.custom == "GO_KILL_GUY") return (Global.getSector().memoryWithoutUpdate["\$MPC_IAIICBKTargetSys"] as StarSystemAPI).hyperspaceAnchor
        val voidsunContrib = fobIntel.getContributionById("voidsun")
        if (voidsunContrib?.custom == "GO_TO_EXTERNAL_RELAY") {
            val targetSysId = Global.getSector().memoryWithoutUpdate.getString("\$MPC_voidsunRelaySysId")
            val targetSys = Global.getSector().getStarSystem(targetSysId) ?: return null
            return targetSys.hyperspaceAnchor
        }
        return null
    }
}
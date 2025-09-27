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
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.MPC_People
import data.scripts.campaign.rulecmd.MPC_IAIICChurchCMD
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color

open class MPC_churchContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false, noUpdate: Boolean = false, text: TextPanelAPI? = null): MPC_churchContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_churchContributionIntel()
                    Global.getSector().intelManager.addIntel(intel, noUpdate, text)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_churchContributionIntel
        }
        fun getMarket(id: String): MarketAPI? = Global.getSector().economy.getMarket(id)

        const val KEY = "\$MPC_churchContributionIntel"
    }

    enum class State {
        VISIT_HIDEOUT {
            override fun apply() {
                val target = MPC_IAIICChurchCMD.getHideout()
                target.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }
            /*override fun unapply() {
                val target = MPC_IAIICChurchCMD.getHideout()
                target.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }*/
        },
        FIND_ASHER_CONTACT {
            override fun apply() {
                val asher = getMarket("asher") ?: return
                asher.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }
        },
        GO_TO_ASHER_NANOFORGE,
        DELIVER_HERETICAL_TECH {
            override fun unapply() {
                val asher = getMarket("asher") ?: return
                asher.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
        },
        DONE,
        FAILED;

        open fun apply() {}
        open fun unapply() {}
    }
    var state: State = State.VISIT_HIDEOUT
        set(value) {
            field.unapply()
            field = value
            field.apply()
        }

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH).crest
    }

    override fun getName(): String = "Church involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + MPC_indieContributionIntel.getBaseContributionTags() + Factions.LUDDIC_CHURCH).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is String) {
            info.addPara(listInfoParam as String, initPad)
        }
        val hideout = MPC_IAIICChurchCMD.getHideout()
        if (listInfoParam is State) {
            when (listInfoParam) {
                State.VISIT_HIDEOUT -> {
                    info.addPara(
                        "Visit the hideout on %s", initPad, Misc.getBasePlayerColor(), hideout.name
                    )
                }
                State.FIND_ASHER_CONTACT -> {
                    info.addPara(
                        "Find the %s on %s",
                        initPad,
                        Misc.getHighlightColor(),
                        "fence", getMarket("asher")?.name
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        getMarket("asher")!!.faction.baseUIColor
                    )
                }
                State.GO_TO_ASHER_NANOFORGE -> {
                    info.addPara(
                        "Visit the %s on %s",
                        initPad,
                        Misc.getHighlightColor(),
                        "Knights", getMarket("asher")?.name
                    ).setHighlightColors(
                        Misc.getHighlightColor(),
                        getMarket("asher")!!.faction.baseUIColor
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
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the Luddic Church - primarily, the Knights Of Ludd - may be involved in the IAIIC.",
            5f
        )

        val militantOne = getAloofMilitant()
        val hideout = MPC_IAIICChurchCMD.getHideout()
        val exodus = MPC_IAIICChurchCMD.getExodus()
        info.addPara(
            "You've been contacted by a 'militant' named %s who seeks to dismantle the Knights of Ludd. Seeing you as a temporary ally, she suggested " +
            "working together to disrupt the Knights - which you have agreed to. They have a hideout on %s, in the %s system.",
            5f,
            Misc.getHighlightColor(),
            militantOne.nameString, hideout.name, "Eos Exodus"
        ).setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getBasePlayerColor(),
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor
        )

        when (state) {
            State.FAILED -> {
                info.addPara("You have failed to drive a wedge between the IAIIC and the church.", 5f)
            }
            State.DONE -> {
                info.addPara("You have successfully dismantled the %s, and instilled a deep distrust for them within the populist church. " +
                        "Church vessels have ceased appearing in your space, and INTSEC suggests the %s is now lacking the military of the church.",
                5f,
                Misc.getHighlightColor(),
                "Luddic Knights", "IAIIC"
                ).setHighlightColors(
                    factionForUIColors.baseUIColor,
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor
                )
            }

            State.VISIT_HIDEOUT -> {
                info.addPara(
                    "You need to visit the hideout to link up with the rest of the militants. You've been given a set of coordinates, " +
                    "and a pass-phrase: '%s'.",
                    5f,
                    Misc.getHighlightColor(),
                    "In the light of darkness"
                )
            }
            State.FIND_ASHER_CONTACT -> {
                info.addFirstStepText()

                info.addPara(
                    "You are currently trying to meet with a %s on %s to secure contact with a 'technologically malleable' knight.",
                    5f,
                    Misc.getHighlightColor(),
                    "fence", getMarket("asher")?.name
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    getMarket("asher")!!.faction.baseUIColor
                )
            }
            State.GO_TO_ASHER_NANOFORGE -> TODO()
            State.DELIVER_HERETICAL_TECH -> TODO()
        }
    }

    fun TooltipMakerAPI.addFirstStepText() {
        addPara(
            "You and the Militants have hatched a plan. Capitalizing on the latent civil unrest against the Knights' 'heresy', " +
            "you will record a deal with a scrupulous knight involving highly heretical technology and transmit it to luddic population centers.",
            5f
        )
    }

    private fun getAloofMilitant(): PersonAPI {
        return MPC_People.getImportantPeople()[MPC_People.CHURCH_ALOOF_MILITANT]!!
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        when (state) {
            State.VISIT_HIDEOUT -> {
                return MPC_IAIICChurchCMD.getHideout()
            }
            State.FIND_ASHER_CONTACT -> {
                return getMarket("asher")?.primaryEntity
            }
            else -> return null
        }
    }
}
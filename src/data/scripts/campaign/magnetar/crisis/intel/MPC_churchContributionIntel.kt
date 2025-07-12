package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.rulecmd.MPC_IAIICPatherCMD
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeUnimportant
import java.awt.Color

open class MPC_churchContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_churchContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_churchContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
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
                val asher = getMarket("gilead") ?: return
                asher.primaryEntity?.makeImportant(niko_MPC_ids.IAIIC_QUEST)
            }
            override fun unapply() {
                val asher = getMarket("gilead") ?: return
                asher.primaryEntity?.makeUnimportant(niko_MPC_ids.IAIIC_QUEST)
            }
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
        if (listInfoParam is State) {
            /*when (listInfoParam) {
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
            }*/
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_CHURCH)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the Luddic Church may be involved in the IAIIC.",
            5f
        )

        /*when (state) {
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
        }*/
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        /*if (state.ordinal >= State.GO_TO_HIDEOUT.ordinal) {
            return Global.getSector().memoryWithoutUpdate["\$MPC_IAIICLPHideout"] as? PlanetAPI
        }*/
        return null
    }
}
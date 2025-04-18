package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.magiclib.kotlin.makeNonStoryCritical
import java.awt.Color

class MPC_indieContributionIntel: BaseIntelPlugin() {
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

        const val KEY = "\$MPC_indieContributionIntel"
    }

    enum class State {
        PAY_UP,
        DONE,
        FAILED
    }
    var state: State = State.PAY_UP

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.INDEPENDENT).crest
    }

    override fun getName(): String = "Independent involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.INDEPENDENT, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
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
                State.PAY_UP -> {
                    info.addPara(
                        "Pay the %s",
                        5f,
                        factionForUIColors.baseUIColor
                    )
                }
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
            "You are investigating reports that the KKL, a large merc company, may be involved in the IAIIC.",
            5f
        )

        when (state) {
            State.PAY_UP -> {
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
            State.FAILED -> {
                info.addPara("You have failed to drive a wegde between the IAIIC and the KKL.", 5f)
            }
            State.DONE -> {
                info.addPara("You have successfully paid off the %s, forcing them from the %s.",
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

        Global.getSector().economy.getMarket("new_maxios")?.makeNonStoryCritical("\$MPC_IAIICEvent")

    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        return Global.getSector().economy.getMarket("new_maxios")?.primaryEntity
    }
}
package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import java.awt.Color

class MPC_luddicContributionIntel: BaseIntelPlugin() {


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
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.LUDDIC_PATH)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        info?.addImage(factionForUIColors.logo, width, 128f, 10f)

        if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_didInitialIAIICLPBarEvent")) {
            val label = info?.addPara(
                "You've learnt about the %s: A disorganized group of pathers supposadly providing covert support to the %s.",
                0f,
                Misc.getHighlightColor(),
                Global.getSector().memoryWithoutUpdate.getString(niko_MPC_ids.PATHER_SECT_NAME), "IAIIC"
            )
            label?.setHighlightColors(
                Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            )

            info?.addPara(
                "They supposedly hide out in %s bars.",
                5f,
                Global.getSector().getFaction(Factions.LUDDIC_PATH).baseUIColor,
                "Luddic Path"
            )
        }
    }
}
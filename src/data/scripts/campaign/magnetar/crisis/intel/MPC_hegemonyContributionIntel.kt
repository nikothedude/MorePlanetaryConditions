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

class MPC_hegemonyContributionIntel: BaseIntelPlugin() {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_hegemonyContributionIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_hegemonyContributionIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_hegemonyContributionIntel
        }

        const val KEY = "\$MPC_hegeContributionIntel"
    }

    enum class State {
        GO_TO_EVENTIDE_INIT,
        DONE,
        FAILED
    }
    var state: State = State.GO_TO_EVENTIDE_INIT

    override fun getIcon(): String {
        return Global.getSector().getFaction(Factions.HEGEMONY).crest
    }

    override fun getName(): String = "Hegemony involvement"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Factions.HEGEMONY, niko_MPC_ids.IAIIC_FAC_ID, Tags.INTEL_COLONIES)).toMutableSet()
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (info == null || mode == null) return

        if (!isUpdate) return
        if (listInfoParam is String) {
            info.addPara(listInfoParam as String, initPad)
        }
        if (listInfoParam is State) {
            when (listInfoParam) {
                State.GO_TO_EVENTIDE_INIT -> {
                    info.addPara(
                        "Go to %s",
                        5f,
                        Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                        "eventide"
                    )
                }
            }
        }
    }

    override fun getFactionForUIColors(): FactionAPI {
        return Global.getSector().getFaction(Factions.HEGEMONY)
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return
        info.addImage(factionForUIColors.logo, width, 128f, 10f)

        info.addPara(
            "You are investigating reports that the hegemony may be involved in the IAIIC.",
            5f
        )

        when (state) {
            State.GO_TO_EVENTIDE_INIT -> {
                info.addPara(
                    "Following your fruitless meeting with the high hegemon, a strange individual claiming to be a eventide aristocrat " +
                    "claimed to have %s of %s involvement with the %s, and asked you to meet them at %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "evidence", "IAIIC", "hegemony", "eventide"
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
                    factionForUIColors.baseUIColor,
                    Global.getSector().economy.getMarket("eventide")?.faction?.baseUIColor,
                )
            }
            State.FAILED -> {
                info.addPara("You have failed to drive a wegde between the IAIIC and the hegemony.", 5f)
            }
            State.DONE -> {
                info.addPara("The %s has been forced to disengage from the %s, delivering a crippling blow to their operations.",
                    5f,
                    Misc.getHighlightColor(),
                    "hegemony", "IAIIC"
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

        Global.getSector().economy.getMarket("eventide")?.makeNonStoryCritical("\$MPC_IAIICEvent")

    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken? {
        return when (state) {
            State.GO_TO_EVENTIDE_INIT -> Global.getSector().economy.getMarket("eventide").primaryEntity
            State.DONE -> null
            State.FAILED -> null
        }
    }

}
package data.scripts.campaign.magnetar.quest

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class MPC_magnetarQuest: BaseIntelPlugin() {

    enum class Stage {
        GO_TO_KANTA,
        FIND_SYSTEM,
        RETURN_CHAIR,
        DONE
    }
    var stage: Stage = Stage.GO_TO_KANTA

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        val opad = 10f

        if (stage == Stage.GO_TO_KANTA) {
            info!!.addPara(
                "You were accosted by a pair of goons sent from Kanta. Apparently kanta wants to speak with you about " +
                    "some sort of task.",
                opad
            )
        } else if (stage == Stage.FIND_SYSTEM) {
            info!!.addPara(
                "Kanta has tasked you with recovering some kind of artifact, and has given you a wormhole terminus " +
                    "that supposedly leads to it. Surely, this can't go wrong.",
                opad
            )
        } else if (stage == Stage.RETURN_CHAIR){
            info!!.addPara(
                "You've acquired an ancient chair, and now you must deliver it to Kanta for your reward.",
                opad
            )
        } else {
            info!!.addPara(
                "You've delivered the chair to Kanta.",
                opad
            )
        }
        addBulletPoints(info, ListInfoMode.IN_DESC)
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: ListInfoMode) {
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f
        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad
        val tc = getBulletColorForMode(mode)
        bullet(info)
        val isUpdate = getListInfoParam() != null
        if (stage == Stage.GO_TO_KANTA) {
            info.addPara("Go to %s and %s", initPad, h, "Kanta's Den", "talk to kanta")
        } else if (stage == Stage.FIND_SYSTEM) {
            info.addPara("Find the artifact", h, initPad)
        } else if (stage == Stage.RETURN_CHAIR) {
            info.addPara("Return the chair to %s", initPad, h, "Kanta")
        }
        initPad = 0f
        unindent(info)
    }

    override fun endAfterDelay() {
        stage = Stage.DONE
        super.endAfterDelay()
    }

    override fun createIntelInfo(info: TooltipMakerAPI, mode: ListInfoMode?) {
        val c = getTitleColor(mode)
        info.setParaSmallInsignia()
        info.addPara(name, c, 0f)
        info.setParaFontDefault()
        addBulletPoints(info, mode!!)
    }

    override fun getName(): String = "What we lost"

    override fun getIcon(): String? {
        return Global.getSettings().getSpriteName("intel", "niko_MPC_magnetarIcon")
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_ACCEPTED)
        tags.add(Tags.INTEL_MISSIONS)
        if (stage.ordinal > Stage.GO_TO_KANTA.ordinal) {
            tags.add(Tags.INTEL_EXPLORATION)
        }
        return tags
    }

}
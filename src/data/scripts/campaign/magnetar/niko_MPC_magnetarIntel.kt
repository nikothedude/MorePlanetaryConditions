package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import java.awt.Color

class niko_MPC_magnetarIntel: BaseIntelPlugin() {

    init {
        important = true
    }

    companion object {
        fun get(): niko_MPC_magnetarIntel? = Global.getSector().intelManager.getFirstIntel(niko_MPC_magnetarIntel::class.java) as? niko_MPC_magnetarIntel
    }

    override fun addBulletPoints(info: TooltipMakerAPI?, mode: IntelInfoPlugin.ListInfoMode?) {
        super.addBulletPoints(info, mode)

        info!!.addSpacer(3f)
        info.addPara(
            "An important travel advisory",
            0f,
            Misc.getTextColor(),
            Misc.getHighlightColor(),
            "travel advisory"
        )
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI?,
        mode: IntelInfoPlugin.ListInfoMode?,
        isUpdate: Boolean,
        tc: Color?,
        initPad: Float
    ) {
        super.addBulletPoints(info, mode, isUpdate, tc, initPad)

        if (isUpdate) {
            when (listInfoParam) {
                "discoveredInterdict" -> {
                    info?.addPara(
                        "%s to %s",
                        5f,
                        Misc.getHighlightColor(),
                        "Interdict", "parry pulses"
                    )
                    val percentNeeded = "${(niko_MPC_magnetarPulse.MAX_INTERDICT_PROGRESS_NEEDED * 100f).roundNumTo(1).trimHangingZero()}%"
                    info?.addPara(
                        "Effectiveness highest at %s interdict progress or above",
                        5f,
                        Misc.getHighlightColor(),
                        percentNeeded
                    )
                }
            }
        }
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val DESC_ONE = "Magnetars are eccentric neutron star variants, boasting an unfathomably powerful magnetic field that can shred even non-ferric material. \n" +
                "\n" +
                "A drive field is usually sufficient to deny the pulling force of the star, but magnetars are notorious for the ability to break them with ionized pulses, leaving fleets adrift and helpless. \n" +
                "\n" +
                "Nothing has been known to survive contact with a magnetar, making it a top priority to avoid the magnetar and it's pulses if at all possible."
        val DESC_ONE_HIGHLIGHTS = arrayListOf(
            Pair("Magnetars", Misc.getHighlightColor()),
            Pair("shred even non-ferric material", Misc.getNegativeHighlightColor()),
            Pair("usually sufficient", Misc.getPositiveHighlightColor()),
            Pair("break them", Misc.getNegativeHighlightColor()),
            Pair("ionized pulses", Misc.getHighlightColor()),
            Pair("avoid the magnetar", Misc.getNegativeHighlightColor())
        )

        val paraOne = info.addPara(DESC_ONE, 0f)

        val highlightArrayOne = Array(DESC_ONE_HIGHLIGHTS.size) { index: Int -> DESC_ONE_HIGHLIGHTS[index].first }
        val highlightColorArrayTwo = Array(DESC_ONE_HIGHLIGHTS.size) { index: Int -> DESC_ONE_HIGHLIGHTS[index].second }
        paraOne.setHighlight(*highlightArrayOne)
        paraOne.setHighlightColors(*highlightColorArrayTwo)

        info.addSectionHeading(
            "Pulse information",
            Alignment.MID,
            10f
        )

        val discoveredInterdictTech = Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_discoveredInterdictsCounterPulses")
        info.setBulletedListMode(BULLET)
        info.addPara("Pulses impact when the %s hits", 0f, Misc.getHighlightColor(), "center of the cloud")
        info.addPara("Pulses are more dangerous when %s to the magnetar due to the fall-off effect", 0f, Misc.getHighlightColor(), "up-close")
        info.addPara("Pulses are %s", 0f, Misc.getHighlightColor(), "semi-regular")
        info.addPara("Being hit with a pulse will cause %s", 0f, Misc.getHighlightColor(), "significant CR drain")
        info.setBulletedListMode(null)

        info.addSectionHeading(
            "Tactical",
            Alignment.MID,
            10f
        )
        info.addPara("Pulses can be avoided by...", 5f)
        info.setBulletedListMode(BULLET)
        info.addPara("Sheltering %s or %s", 0f, Misc.getHighlightColor(), "utop planets", "behing large stellar bodies")
        info.addPara("Fleeing - getting away from the magnetar %s of the pulse", 0f, Misc.getHighlightColor(), "inhibits the effect")
        if (discoveredInterdictTech) {
            info.addPara("Timing an interdiction well", 0f).setColor(Misc.getHighlightColor())
            info.setBulletedListMode(INDENT + BULLET)
            val percentNeeded = "${(niko_MPC_magnetarPulse.MAX_INTERDICT_PROGRESS_NEEDED * 100f).roundNumTo(1).trimHangingZero()}%"
            info.addPara("If interdict is charged %s or more when the pulse impacts, it will result in %s", 0f, Misc.getHighlightColor(), percentNeeded, "total protection")
            info.addPara("Lower levels will result in %s", 0f, Misc.getHighlightColor(), "partial protection")
            info.addPara("A %s will put interdict on a %s", 0f, Misc.getHighlightColor(), "successful deflection", "long cooldown")
            info.addPara("Firing off the interdiction before impact results in a %s - %s", 0f, Misc.getHighlightColor(), "failed deflection", "timing is key").setHighlightColors(
                Misc.getNegativeHighlightColor(),
                Misc.getHighlightColor()
            )
            info.setBulletedListMode(BULLET)
        }
        info.addPara("%s - but public research on magnetars is %s", 0f, Misc.getHighlightColor(), "Likely more", "heavily limited").setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor()
        )
        info.setBulletedListMode(null)

    }

    override fun getIcon(): String = Global.getSettings().getSpriteName("intel", "niko_MPC_magnetarIcon");

    override fun canTurnImportantOff(): Boolean = true
}
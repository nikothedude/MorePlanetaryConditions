package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class niko_MPC_magnetarIntel: BaseIntelPlugin() {

    init {
        important = true
    }

    companion object {
        const val DEFAULT_DESC = "Magnetars are eccentric neutron star variants, boasting an unfathomably powerful magnetic field " +
                "that can shred even non-ferric material. \n" +
                "\n" +
                "It projects a gargantuan magnetic field powerful enough to pull in a starship and tear off external plating, though " +
                "a fleet's drive field is sufficient at mitigating the worst of this. Unfortunately, this particular magnetar is capable of" +
                "emitting pulses of ionized particles, which are energetic enough to shatter the drive field for days on end, disabling the " +
                "fleet's travel drives and making transverse jumping impossible, allthewhile dragging the fleet closer and closer to the star." +
                "Nothing has been known to survive close proximity to a magnetar without a drive field, making it a TOP PRIORITY to maintain distance. \n" +
                "\n" +
                "\n" +
                "Your tactical officer seems to have left a note at the bottom of the report: \"Sir, I know how you are about these kind of warnings. " +
                "But believe me when I say, this one isn't embellished in the slightest. This isn't just a threat to our daily operations. If we " +
                "get close to the magnetar without shielding... it'll kill us all. Instantly. This isn't like a black hole, where our drive field insulates us," +
                "because we can lose ours here. Please, sir. Tread carefully.\""
        val DEFAULT_DESC_HIGHLIGHTS = hashMapOf(
            Pair("Magnetars", Misc.getHighlightColor()),
            Pair("shred even non-ferric material", Misc.getNegativeHighlightColor()),
            Pair("unfathomably powerful magnetic field", Misc.getHighlightColor()),
            Pair("pull", Misc.getNegativeHighlightColor()),
            Pair("tear", Misc.getNegativeHighlightColor()),
            Pair("sufficient", Misc.getPositiveHighlightColor()),
            Pair("mitigating", Misc.getPositiveHighlightColor()),
            Pair("pulses", Misc.getNegativeHighlightColor()),
            Pair("shatter", Misc.getNegativeHighlightColor()),
            Pair("disabling", Misc.getNegativeHighlightColor()),
            Pair("closer and closer", Misc.getNegativeHighlightColor()),
            Pair("TOP PRIORITY", Misc.getNegativeHighlightColor()),
            Pair("it'll kill us all", Misc.getNegativeHighlightColor()),
            Pair("Tread carefully", Misc.getHighlightColor())
        )
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

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val gray = Misc.getGrayColor()
        info.addSpacer(10f)
        val imageTooltip = info.beginImageWithText(icon, 32f)
        val para = imageTooltip.addPara(DEFAULT_DESC, 0f, Misc.getTextColor())
        para.setHighlight(*DEFAULT_DESC_HIGHLIGHTS.keys.toTypedArray())
        para.setHighlightColors(*DEFAULT_DESC_HIGHLIGHTS.values.toTypedArray())

        info.addImageWithText(0f)
        info.addSpacer(10f)
    }

    override fun getIcon(): String = Global.getSettings().getSpriteName("intel", "niko_MPC_magnetarIcon");

    override fun canTurnImportantOff(): Boolean = true
}
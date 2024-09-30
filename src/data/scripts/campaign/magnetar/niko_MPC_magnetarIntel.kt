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
                "They project a gargantuan magnetic field powerful enough to pull in a starship and turn it to mush, though " +
                "a fleet's drive field is sufficient at mitigating the worst of this. Unfortunately, this particular magnetar is capable of " +
                "emitting pulses of ionized particles which are energetic enough to shatter the drive field for days on end, disabling the " +
                "fleet's travel drives and making transverse jumping impossible, allthewhile dragging the fleet closer and closer to the star. " +
                "Nothing has been known to survive close proximity to a magnetar without a drive field, making it a TOP PRIORITY to maintain distance. \n" +
                "\n" +
                "-------------------------------------------------------------------------------------------------------------------------------------" +
                "\n" +
                "Your tactical officer seems to have left a note at the bottom of the report: \"Sir, after studying the pulses we've " +
                "discovered a potential method of reinforcing our drive field. If we set up our sensors to interdict at the same time, " +
                "we can mitigate if not prevent entirely (depending on the timing of the interdiction) the impact of a pulse - although, it will " +
                "severely overload our ECM packages, meaning it will take a lot longer to set up the next interdiction.\n" +
                "\n" +
                "Additionally, the pulses are blocked by large stellar masses, making them effective shelters - you just have to position your fleet on top of the planet.\n" +
                "\n" +
                "Also, sir, I know how you are about these kind of warnings. " +
                "But believe me when I say, this one isn't embellished in the slightest. This isn't just a threat to our daily operations. If we " +
                "get close to the magnetar without shielding... it'll kill us all. Instantly. This isn't like a black hole, where our drive field insulates us, " +
                "because we can lose ours here. Please, sir. Tread carefully.\""
        val DEFAULT_DESC_HIGHLIGHTS = arrayListOf(
            Pair("Magnetars", Misc.getHighlightColor()),
            Pair("shred even non-ferric material", Misc.getNegativeHighlightColor()),
            Pair("gargantuan magnetic field", Misc.getHighlightColor()),
            Pair("pull", Misc.getNegativeHighlightColor()),
            Pair("tear", Misc.getNegativeHighlightColor()),
            Pair("sufficient", Misc.getPositiveHighlightColor()),
            Pair("mitigating", Misc.getPositiveHighlightColor()),
            Pair("pulses", Misc.getNegativeHighlightColor()),
            Pair("shatter", Misc.getNegativeHighlightColor()),
            Pair("disabling", Misc.getNegativeHighlightColor()),
            Pair("closer and closer", Misc.getNegativeHighlightColor()),
            Pair("TOP PRIORITY", Misc.getNegativeHighlightColor()),
            Pair("interdict at the same time", Misc.getHighlightColor()),
            Pair("mitigate if not prevent entirely", Misc.getPositiveHighlightColor()),
            Pair("lot longer to set up the next interdiction", Misc.getNegativeHighlightColor()),
            Pair("blocked by large stellar masses", Misc.getHighlightColor()),
            Pair("effective shelters", Misc.getPositiveHighlightColor()),
            Pair("on top of the planet", Misc.getHighlightColor()),
            Pair("it'll kill us all", Misc.getNegativeHighlightColor()),
            Pair("Instantly", Misc.getNegativeHighlightColor()),
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

        val highlightArray = Array(DEFAULT_DESC_HIGHLIGHTS.size) { index: Int -> DEFAULT_DESC_HIGHLIGHTS[index].first }
        val highlightColorArray = Array(DEFAULT_DESC_HIGHLIGHTS.size) { index: Int -> DEFAULT_DESC_HIGHLIGHTS[index].second }

        para.setHighlight(*highlightArray)
        para.setHighlightColors(*highlightColorArray)

        info.addImageWithText(0f)
        info.addSpacer(10f)
    }

    override fun getIcon(): String = Global.getSettings().getSpriteName("intel", "niko_MPC_magnetarIcon");

    override fun canTurnImportantOff(): Boolean = true
}
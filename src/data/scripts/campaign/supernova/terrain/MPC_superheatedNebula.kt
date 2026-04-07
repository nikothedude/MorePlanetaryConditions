package data.scripts.campaign.supernova.terrain

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class MPC_superheatedNebula: MPC_supernovaNebula() {
    // TODO - go dark mostly nullifies the effect

    override fun getTerrainName(): String {
        return "Superheated Nebula"
    }

    override fun getNameColor(): Color? {
        val bad = Misc.getNegativeHighlightColor()
        val base = super.getNameColor()

        return Misc.interpolateColor(
            base,
            bad,
            Global.getSector().campaignUI.sharedFader.brightness * 1f
        )
    }
}
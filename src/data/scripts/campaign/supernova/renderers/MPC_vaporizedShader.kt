package data.scripts.campaign.supernova.renderers

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import data.scripts.campaign.supernova.MPC_supernovaActionScript
import data.scripts.campaign.supernova.entities.MPC_supernovaExplosion
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.magiclib.kotlin.interpolateColor
import java.awt.Color
import java.util.EnumSet

class MPC_vaporizedShader(): LunaCampaignRenderingPlugin {
    override fun isExpired(): Boolean {
        return MPC_supernovaActionScript.getCurrStage() == null
    }

    var stage = "WHITEOUT"
    var phase = 0f
    override fun advance(amount: Float) {

        if (stage == "WHITEOUT") {
            phase = (phase + amount).coerceAtMost(1f)
            if (phase >= 1f) stage = "BLACKOUT"
        } else {
            phase = (phase - amount).coerceAtLeast(0f)
        }

        return
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
        return EnumSet.of(CampaignEngineLayers.ABOVE)
    }

    @Transient
    var shader: SpriteAPI? = null

    override fun render(
        layer: CampaignEngineLayers?,
        viewport: ViewportAPI?
    ) {
        if (layer == null || viewport == null) return

        if (shader == null) {
            Global.getSettings().loadTexture("graphics/fx/MPC_colorBlockWhite.png")
            shader = Global.getSettings().getSprite("graphics/fx/MPC_colorBlockWhite.png")
        }

        if (layer == CampaignEngineLayers.ABOVE) {
            val offset = 400f
            shader?.setSize(viewport.visibleWidth + offset, viewport.visibleHeight + offset)

            if (stage == "BLACKOUT") {
                shader?.color = Color.WHITE.interpolateColor(Color.BLACK, (1 - phase).coerceAtMost(1f))
                shader?.alphaMult = 1f
                shader?.render(viewport.llx - (offset / 2), viewport.lly - (offset / 2))
            } else {
                shader?.color = Color.WHITE
                shader?.alphaMult = phase
                shader?.render(viewport.llx - (offset / 2), viewport.lly - (offset / 2))
            }
        }
    }
}
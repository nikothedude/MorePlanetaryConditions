package data.scripts.campaign.supernova.renderers

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import data.scripts.campaign.supernova.MPC_supernovaActionScript
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import java.awt.Color
import java.util.EnumSet

class MPC_supernovaShader(
    val script: MPC_supernovaActionScript
): LunaCampaignRenderingPlugin {
    override fun isExpired(): Boolean {
        return MPC_supernovaActionScript.getCurrStage() == null
    }

    override fun advance(amount: Float) {
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

        if (!script.star.containingLocation.isCurrentLocation) return

        if (shader == null) {
            Global.getSettings().loadTexture("graphics/fx/MPC_colorBlockWhite.png")
            shader = Global.getSettings().getSprite("graphics/fx/MPC_colorBlockWhite.png")
        }

        if (layer == CampaignEngineLayers.ABOVE) {
            val stage = MPC_supernovaActionScript.getCurrStage()
            when (stage) {
                MPC_supernovaActionScript.Stage.BEFORE -> {
                    val offset = 400f

                    val color = Color.BLACK
                    shader?.color = color

                    shader?.alphaMult = (script.getStageProgress() * 0.3f).coerceAtMost(0.9f)
                    shader?.setSize(viewport.visibleWidth + offset, viewport.visibleHeight + offset)
                    shader?.render(viewport.llx - (offset / 2), viewport.lly - (offset / 2))
                }
                MPC_supernovaActionScript.Stage.DURING -> {
                    val offset = 400f

                    val color = Color.WHITE
                    shader?.color = color

                    val inverted = (1 - script.getStageProgress())
                    shader?.alphaMult = (inverted * 0.1f)
                    shader?.setSize(viewport.visibleWidth + offset, viewport.visibleHeight + offset)
                    shader?.render(viewport.llx - (offset / 2), viewport.lly - (offset / 2))
                }
                else -> {}
            }

        }
    }
}
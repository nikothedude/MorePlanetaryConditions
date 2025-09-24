package data.scripts.campaign.plugins

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.FaderUtil
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import java.util.EnumSet
import kotlin.math.exp

class MPC_vignetteRenderer: LunaCampaignRenderingPlugin {

    @Transient
    var vignette: SpriteAPI? = null
    var layers = EnumSet.of(CampaignEngineLayers.ABOVE)
    val fader = FaderUtil(0f, 3f, 3f, false, false)
    @Transient
    var script: EveryFrameScript = object : EveryFrameScript {
        override fun isDone(): Boolean = false

        override fun runWhilePaused(): Boolean = true

        override fun advance(amount: Float) {
            if (!LunaCampaignRenderer.hasRenderer(this@MPC_vignetteRenderer)) {
                Global.getSector().removeScript(this)
                return
            }
            this@MPC_vignetteRenderer.fader.advance(amount)
        }

    }

    init {
        fader.fadeIn()
        Global.getSector().addTransientScript(script)
    }

    var expiring = false
    override fun isExpired(): Boolean = (expiring && fader.brightness == 0f)

    override fun advance(amount: Float) {
        return
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
        return layers
    }

    override fun render(
        layer: CampaignEngineLayers?,
        viewport: ViewportAPI?
    ) {
        if (vignette == null) {
            Global.getSettings().loadTexture("graphics/fx/MPC_vignette.png")
            vignette = Global.getSettings().getSprite("graphics/fx/MPC_vignette.png")
        }

        if (layer == CampaignEngineLayers.ABOVE) {
            //Vignette
            var offset = 400f

            vignette?.alphaMult = fader.brightness
            vignette?.setSize(viewport!!.visibleWidth + offset, viewport!!.visibleHeight + offset)
            vignette?.render(viewport!!.llx - (offset / 2), viewport!!.lly - (offset / 2))

        }
    }
}
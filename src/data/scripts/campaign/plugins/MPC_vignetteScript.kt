package data.scripts.campaign.plugins

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.FaderUtil
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.dark.shaders.util.ShaderLib
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11

class MPC_vignetteScript: niko_MPC_baseNikoScript() {

    val fader = FaderUtil(0f, 3f, 3f, false, false)
    var vignette: SpriteAPI? = null

    init {
        fader.fadeIn()
    }

    override fun startImpl() {
        Global.getSector().addTransientScript(this)
    }

    override fun stopImpl() {
        Global.getSector().removeTransientScript(this)
    }

    override fun runWhilePaused(): Boolean = true

    var isExpiring = false
    override fun advance(amount: Float) {
        if (isExpiring && fader.brightness <= 0f) {
            delete()
            return
        }

        if (vignette == null) {
            Global.getSettings().loadTexture("graphics/fx/MPC_vignette.png")
            vignette = Global.getSettings().getSprite("graphics/fx/MPC_vignette.png")
        }
        fader.advance(amount)

        var offset = 400f

        val viewport = Global.getSector().viewport

        vignette?.alphaMult = fader.brightness
        vignette?.setSize(viewport!!.visibleWidth + offset, viewport!!.visibleHeight + offset)
        vignette?.render(viewport!!.llx - (offset / 2), viewport!!.lly - (offset / 2))
    }
}
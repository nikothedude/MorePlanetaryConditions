package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.dark.shaders.util.ShaderLib
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.elapsedDaysSinceGameStart
import org.magiclib.kotlin.getDistance
import java.util.EnumSet

class MPC_glitchShaderRenderer: LunaCampaignRenderingPlugin {

    companion object {
        const val MAX_INTENSITY_DIST = 0f
        const val MIN_INTENSITY_DIST = 1100f
    }

    var shader = 0

    init {
        shader = ShaderLib.loadShader(
            Global.getSettings().loadText("data/shaders/MPC_baseVertShader.vert"),
            Global.getSettings().loadText("data/shaders/MPC_glitchShader.frag"))
        if (shader != 0) {
            GL20.glUseProgram(shader)

            GL20.glUniform1i(GL20.glGetUniformLocation(shader, "screenTex"), ShaderLib.getScreenTexture())

            GL20.glUseProgram(0)
        }
    }

    override fun isExpired(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        return
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers> {
        return EnumSet.of(CampaignEngineLayers.ABOVE)
    }

    override fun render(
        layer: CampaignEngineLayers,
        viewport: ViewportAPI
    ) {
        var playerfleet = Global.getSector().playerFleet
        if (!playerfleet.containingLocation.hasTag("theme_magnetar")) return

        //Screen texture can be unloaded if graphicslib shaders are disabled, causing a blackscreen
        if (ShaderLib.getScreenTexture() != 0) {
            //Shader
            var clock = Global.getSector().clock
            var t = clock.convertToSeconds(clock.elapsedDaysSinceGameStart())

            val intensity = getIntensity(viewport)
            if (intensity > 0f && Global.getSector().campaignUI.currentInteractionDialog == null) {
                Global.getSoundPlayer().playUILoop(
                    "MPC_glitchLoop", // https://pixabay.com/sound-effects/technology-glitch-sounds-26212/
                    1f,
                    intensity,
                )
            }

            ShaderLib.beginDraw(shader);
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "time"), t)
            GL20.glUniform1f(GL20.glGetUniformLocation(shader, "intensity"), intensity)
            GL20.glUniform1i(GL20.glGetUniformLocation(shader, "colorSensitivityMode"), niko_MPC_settings.getColorSensitivityMode())

            GL13.glActiveTexture(GL13.GL_TEXTURE0 + 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShaderLib.getScreenTexture());

            //Might Fix Incompatibilities with odd drivers
            GL20.glValidateProgram(shader)
            if (GL20.glGetProgrami(shader, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                ShaderLib.exitDraw()
                return
            }

            GL11.glDisable(GL11.GL_BLEND);
            ShaderLib.screenDraw(ShaderLib.getScreenTexture(), GL13.GL_TEXTURE0 + 0)
            ShaderLib.exitDraw()
        }

    }

    fun getIntensity(viewport: ViewportAPI): Float {
        val playerFleet = Global.getSector().playerFleet ?: return 0f
        val star = playerFleet.starSystem.star ?: return 0f

        val dist = MathUtils.getDistance(star, playerFleet)
        val minDist = MAX_INTENSITY_DIST
        val adjustedMin = minDist.coerceAtLeast(star.radius)
        val maxDist = MIN_INTENSITY_DIST

        val adjustedDist = (dist - adjustedMin).coerceAtLeast(0f)
        if (adjustedDist > maxDist) return 0f

        var mult = if (playerFleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.DRIVE_BUBBLE_DESTROYED)) 1f else 0.01f
        if (Global.getSector().isPaused) mult *= 0.1f

        return (1 - (1 / (maxDist / adjustedDist))) * mult
    }
}
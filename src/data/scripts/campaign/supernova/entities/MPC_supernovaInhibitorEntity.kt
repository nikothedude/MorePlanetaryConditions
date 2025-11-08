package data.scripts.campaign.supernova.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.supernova.MPC_supernovaActionScript
import data.scripts.campaign.supernova.MPC_supernovaPrepScript
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import org.lwjgl.util.vector.Vector2f
import java.awt.Color


class MPC_supernovaInhibitorEntity: BaseCustomEntityPlugin() {

    companion object {
        fun shouldDoWarning(progress: Float) = progress >= 0.2f

        const val GLOW_FREQUENCY = 1f
        const val TEX_SCROLL_SPEED = 400f // su
    }

    var textOffset = 0f
    var warningGlowPhase = -1f

    private var phase = 0f
    private var phase2 = 0f

    override fun advance(amount: Float) {
        super.advance(amount)

        textOffset = (textOffset + (amount * TEX_SCROLL_SPEED / sprite.width)) % 1f

        val actionStage = MPC_supernovaActionScript.getCurrStage()
        if (actionStage == MPC_supernovaActionScript.Stage.BEFORE) {
            Global.getSoundPlayer().playLoop("MPC_supernovaAlarmImminent", entity, 1f, 1f, entity.location, Misc.ZERO)

            warningGlowPhase += amount
            if (warningGlowPhase > 1f) warningGlowPhase = -1f
        } else {
            val prepInterval = MPC_supernovaPrepScript.getExplosionInterval(false)
            if (prepInterval != null) {
                val dur = prepInterval.intervalDuration
                val curr = prepInterval.elapsed

                if (shouldDoWarning(curr / dur)) {
                    warningGlowPhase += amount
                    if (warningGlowPhase > 1f) warningGlowPhase = -1f

                    Global.getSoundPlayer().playLoop("MPC_supernovaAlarm", entity, 1f, 1f, entity.location, Misc.ZERO)
                } else {
                    warningGlowPhase = -1f
                }
            }
        }
    }

    @Transient
    var sprite: SpriteAPI = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamFringe")
        get() {
            if (field == null) field = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamFringe")
            return field
        }

    @Transient
    var glow: SpriteAPI = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamGlow")
        get() {
            if (field == null) field = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamGlow")
            return field
        }
    @Transient
    var glowTwo: SpriteAPI = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamGlow")
        get() {
            if (field == null) field = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamGlow")
            return field
        }
    @Transient
    var original: SpriteAPI = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamCore")
        get() {
            if (field == null) field = Global.getSettings().getSprite("fx", "MPC_supernovaInhibitionBeamCore")
            return field
        }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        super.render(layer, viewport)

        if (layer == null || viewport == null) return

        val spec = entity.customEntitySpec
        if (spec == null) return

        val w = spec.spriteWidth
        val h = spec.spriteHeight

        val source = MathUtils.getPointOnCircumference(
            entity.location,
            entity.radius * 0.87f,
            Misc.normalizeAngle(entity.facing - 180f)
        )
        val dest = MathUtils.getPointOnCircumference(
            entity.orbitFocus.location,
            entity.orbitFocus.radius,
            Misc.normalizeAngle(entity.facing)
        )

        val star = entity.orbitFocus
        val starOffset = Misc.getUnitVectorAtDegreeAngle(entity.facing)
        starOffset.scale(star.radius)

        val alphaMult = viewport.alphaMult

        //Fringe
        val fringeColor = Color(0, 247, 255, (206 * alphaMult).toInt())

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        sprite.setSize(w / 1.5f, h)
        sprite.alphaMult = alphaMult
        sprite.angle = entity.facing - 90f

        sprite.bindTexture()
        renderLine(source, dest, 10f, fringeColor)
        original.bindTexture()
        renderLine(source, dest, 10f, fringeColor)

        GL11.glDisable(GL11.GL_BLEND)

        val glowColor = Color(0, 219, 255, (255 * alphaMult).toInt())

        glow.color = glowColor
        glow.setSize(512f, 512f)
        glow.alphaMult = alphaMult
        glow.renderAtCenter(dest.x, dest.y)

        if (warningGlowPhase >= 0f) {
            val warningColor = Color(255, 35, 0, (150 * alphaMult).toInt())

            val sourceWarning = MathUtils.getPointOnCircumference(
                entity.location,
                entity.radius * 0.65f,
                entity.facing
            )

            glowTwo.color = warningColor
            glowTwo.setSize(128f, 128f)
            glowTwo.alphaMult = alphaMult
            glowTwo.renderAtCenter(sourceWarning.x, sourceWarning.y)
        }
    }

    override fun getRenderRange(): Float {
        return 2000f
    }

    fun renderLine(start: Vector2f, end: Vector2f, width: Float, color: Color) {
        // taken from ship mastery with permission <3
        // https://github.com/qcwxezda/Starsector-Ship-Mastery-System/blob/4a51c57d4cada7ddc64a7970634b8df9f71f29d1/src/shipmastery/plugin/EmitterArrayPlugin.java
        val diff = Vector2f.sub(end, start, null)
        val dist = diff.length()

        val perp: Vector2f = Vector2f(-diff.y, diff.x).normalise() as Vector2f
        perp.scale(width)
        val negPerp = Vector2f(-perp.x, -perp.y)
        val p1 = Vector2f.add(start, perp, null)
        val p2 = Vector2f.add(start, negPerp, null)
        val p3 = Vector2f.add(end, negPerp, null)
        val p4 = Vector2f.add(end, perp, null)

        GL11.glBegin(GL11.GL_QUADS)
        val colors = color.getRGBComponents(null)
        GL11.glColor4f(colors[0], colors[1], colors[2], colors[3])
        GL11.glTexCoord2f(textOffset, 0f)
        GL11.glVertex2f(p4.x, p4.y)
        GL11.glTexCoord2f(textOffset, sprite.texHeight)
        GL11.glVertex2f(p3.x, p3.y)
        GL11.glColor4f(colors[0], colors[1], colors[2], colors[3])
        GL11.glTexCoord2f(
            sprite.texWidth * dist / this.sprite.width + textOffset,
            sprite.texHeight
        )
        GL11.glVertex2f(p2.x, p2.y)
        GL11.glTexCoord2f(sprite.texWidth * dist / sprite.width + textOffset, 0f)
        GL11.glVertex2f(p1.x, p1.y)
        GL11.glEnd()
    }
}
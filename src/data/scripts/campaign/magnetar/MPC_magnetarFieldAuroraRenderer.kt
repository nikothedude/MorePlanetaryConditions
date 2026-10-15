package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil
import com.fs.starfarer.api.util.Misc
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class MPC_magnetarFieldAuroraRenderer(private val delegate: AuroraRenderer.AuroraRendererDelegate) {
    private var phaseAngle = 0f

    fun advance(amount: Float) {
        val days = Global.getSector().clock.convertToDays(amount)
        phaseAngle += days * 360f * 0.5f
        phaseAngle = Misc.normalizeAngle(phaseAngle)
    }


    fun render(alphaMult: Float) {
        if (alphaMult <= 0) return

        val bandWidthInTexture = delegate.auroraBandWidthInTexture
        var bandIndex: Float

        val radStart = delegate.auroraInnerRadius
        var radEnd = delegate.auroraOuterRadius

        if (radEnd < radStart + 10f) radEnd = radStart + 10f

        val circ = (Math.PI * 2f * (2500f) / 2f).toFloat()
        val pixelsPerSegment = 50f
        val segments = (circ / pixelsPerSegment).roundToInt().toFloat()

        val startRad = Math.toRadians(0.0).toFloat()
        val endRad = Math.toRadians(360.0).toFloat()
        val spanRad = abs(endRad - startRad)
        val anglePerSegment = spanRad / segments

        val loc = delegate.auroraCenterLoc
        val x = loc!!.x
        val y = loc.y


        GL11.glPushMatrix()
        GL11.glTranslatef(x, y, 0f)

        GL11.glEnable(GL11.GL_TEXTURE_2D)

        //GL11.glDisable(GL11.GL_TEXTURE_2D);
        delegate.auroraTexture!!.bindTexture()

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)

        val thickness = (radEnd - radStart) * 1f
        val radius = radStart

        var texProgress = 0f
        val texHeight = delegate.auroraTexture!!.textureHeight
        val imageHeight = delegate.auroraTexture!!.height
        var texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness

        texPerSegment *= delegate.auroraTexPerSegmentMult

        val totalTex = max(1f, (texPerSegment * segments).roundToInt().toFloat())
        texPerSegment = totalTex / segments

        val texWidth = delegate.auroraTexture!!.textureWidth
        val imageWidth = delegate.auroraTexture!!.width


//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		GL11.glDisable(GL11.GL_BLEND);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

//		CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
//		SectorEntityToken star = ((StarSystemAPI)playerFleet.getContainingLocation()).getStar();
//		float distToStar = Misc.getDistance(playerFleet.getLocation(), star.getLocation());// - star.getRadius();
//		System.out.println("Dist to outer star: " + distToStar);
//		System.out.println("Aurora outer: " + delegate.getAuroraOuterRadius());
        val blocker = delegate.auroraBlocker

        for (iter in 0..1) {
            if (iter == 0) {
                bandIndex = 1f
            } else {
                bandIndex = 0f
            }

            val leftTX = bandIndex * texWidth * bandWidthInTexture / imageWidth
            val rightTX = (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f

            GL11.glBegin(GL11.GL_QUAD_STRIP)
            var i = 0f
            while (i < segments + 1) {
                val segIndex = i % segments.toInt()


                //float phaseAngleRad = (float) Math.toRadians(phaseAngle + segIndex * 10) + (segIndex * anglePerSegment * 10f);
                val phaseAngleRad: Float
                if (iter == 0) {
                    phaseAngleRad = Math.toRadians(phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 10f)
                } else { //if (iter == 1) {
                    phaseAngleRad = Math.toRadians(-phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 5f)
                }


                var angle = Math.toDegrees((segIndex * anglePerSegment).toDouble()).toFloat()
                if (iter == 1) angle += 180f

                var blockerMax = 100000f
                if (blocker != null) {
                    blockerMax = blocker.getCurrMaxAt(angle)
                    //blockerMax *= 1.5f;
                    blockerMax *= 0.75f
                    if (blockerMax > blocker.maxRange) {
                        blockerMax = blocker.maxRange
                    }
                    //blockerMax += 1500f;
                }

                val pulseSin = sin(phaseAngleRad.toDouble()).toFloat().coerceAtLeast(0f)
                //if (delegate instanceof PulsarBeamTerrainPlugin) pulseSin += 1f;
                var pulseMax = thickness * delegate.getAuroraShortenMult(angle)
                //				if (pulseMax < 0) {
//					pulseMax = -pulseMax;
//					//pulseSin += 1f;
//				}
                if (pulseMax > blockerMax * 0.5f) {
                    pulseMax = blockerMax * 0.5f
                }
                val pulseAmount = pulseSin * pulseMax
                var pulseInner = pulseAmount * 0.1f
                pulseInner *= delegate.getAuroraInnerOffsetMult(angle)

                //pulseInner *= Math.max(0, pulseSin - 0.5f);
                //pulseInner *= 0f;
                val r = radius

                val thicknessMult = delegate.getAuroraThicknessMult(angle)
                val thicknessFlat = delegate.getAuroraThicknessFlat(angle)

                val theta = anglePerSegment * segIndex

                val cos = cos(theta.toDouble()).toFloat()
                val sin = sin(theta.toDouble()).toFloat()
                var rInner = r - pulseInner
                if (rInner < r * 0.9f) rInner = r * 0.9f

                var rOuter = (r + thickness * thicknessMult + thicknessFlat)

                if (blocker != null) {
                    if (rOuter > blockerMax - pulseAmount) {
//						float fraction = rOuter / (r + thickness * thicknessMult + thicknessFlat);
//						rOuter = blockerMax * fraction;
                        rOuter = blockerMax - pulseAmount
                        //rOuter = blockerMax;
                        if (rOuter < r) rOuter = r
                    }
                    if (rInner > rOuter) {
                        rInner = rOuter
                    }
                }

                var x1 = cos * rInner
                var y1 = sin * rInner
                var x2 = cos * rOuter
                var y2 = sin * rOuter

                x1 *= 0.91f
                y1 *= 0.91f

                x2 += (cos(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                y2 += (sin(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()

                val color = delegate.getAuroraColorForAngle(angle)
                var alpha = delegate.getAuroraAlphaMultForAngle(angle)
                if (blocker != null) {
                    alpha *= blocker.getAlphaAt(angle)
                }
                //				color = Color.white;
//				alphaMult = alpha = 1f;
                GL11.glTexCoord2f(leftTX, texProgress)
                GL11.glColor4ub(
                    (color!!.red * 3f).toInt().toByte(),
                    (color.green * 0.1f).toInt().toByte(),
                    (color.blue * 2f).toInt().toByte(),
                    (color.alpha.toFloat() * alphaMult * alpha).toInt().toByte()
                )
                GL11.glVertex2f(x1, y1) // INNER
                GL11.glTexCoord2f(rightTX, texProgress)
                GL11.glColor4ub(
                    color!!.red.toByte(),
                    color.green.toByte(),
                    color.blue.toByte(),
                    (color.alpha.toFloat() * alphaMult * alpha).toInt().toByte()
                )
                GL11.glVertex2f(x2, y2) // outer

                texProgress += texPerSegment * 1f
                i++
            }
            GL11.glEnd()

            GL11.glRotatef(180f, 0f, 0f, 1f)
        }
        GL11.glPopMatrix()


//		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }


    fun getRenderDistMax(angle: Float): Float {
        var angle = angle
        val radStart = delegate.auroraInnerRadius
        var radEnd = delegate.auroraOuterRadius
        if (radEnd < radStart + 10f) radEnd = radStart + 10f

        val angleRad = Math.toRadians(angle.toDouble()).toFloat()

        val thickness = (radEnd - radStart) * 1f
        val radius = radStart
        val blocker = delegate.auroraBlocker

        var max = 0f
        for (i in 0..1) {
            val phaseAngleRad: Float
            if (i == 0) {
                phaseAngleRad = Math.toRadians(phaseAngle.toDouble()).toFloat() + (angleRad * 10f)
            } else {
                phaseAngleRad = Math.toRadians(-phaseAngle.toDouble()).toFloat() + (angle * 5f)
                angle += 180f
            }

            var blockerMax = 100000f
            if (blocker != null) {
                blockerMax = blocker.getCurrMaxAt(angle)
                blockerMax *= 1.5f
                if (blockerMax > blocker.maxRange) {
                    blockerMax = blocker.maxRange
                }
            }

            val pulseSin = sin(phaseAngleRad.toDouble()).toFloat()
            var pulseMax = thickness * delegate.getAuroraShortenMult(angle)
            if (pulseMax > blockerMax * 0.5f) {
                pulseMax = blockerMax * 0.5f
            }
            val pulseAmount = pulseSin * pulseMax

            val thicknessMult = delegate.getAuroraThicknessMult(angle)
            val thicknessFlat = delegate.getAuroraThicknessFlat(angle)

            var rOuter = (radius + thickness * thicknessMult - pulseAmount + thicknessFlat)

            if (blocker != null) {
                if (rOuter > blockerMax - pulseAmount) {
                    rOuter = blockerMax - pulseAmount
                    if (rOuter < radius) rOuter = radius
                }
            }
            if (rOuter > max) max = rOuter
        }

        return max
    }
}
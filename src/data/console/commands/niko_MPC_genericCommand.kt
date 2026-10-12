package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.Misc
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU
import org.lwjgl.util.glu.Sphere
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import java.util.*
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.tan

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerFleet = Global.getSector().playerFleet

        //LunaCampaignRenderer.addTransientRenderer(MPC_vignetteRenderer())
        LunaCampaignRenderer.addTransientRenderer(MPC_bipoleMagneticLine(playerFleet, 0f))

        Console.showMessage("A")

        return BaseCommand.CommandResult.SUCCESS
    }

    // if we are under, we must be under the star and the fleet and terrain
    // TODO - add a terrain effect if you fly to the intersection point
    // uh oh! major damage. break drive field(?)
    // squish determines how far off the line is from the star. e.g. its angle from the camera
    // higher squish means more/less (not sure which) parallax
    // no squish? it goes forward, then directly back onto the other pole
    // 50 squish? follows a circle around the star to the pole
    // using an under layer means parallax is reversed.
    class MPC_bipoleMagneticLine(val star: SectorEntityToken, val projectionAngle: Float): LunaCampaignRenderingPlugin {

        companion object {
            const val LINE_DISTANCE = 300f
            const val QUAD_COUNT = 250
            const val HEIGHT_AND_BOTTOM = 100f
        }

        val sphere = Sphere()

        @Transient
        var sprite: SpriteAPI? = null
        @Transient
        var spriteTwo: SpriteAPI? = null

        var texX = 0f
        var rotate = 0f

        val midPoint = MathUtils.getPointOnCircumference(
            star.location,
            star.radius + LINE_DISTANCE,
            projectionAngle
        )

        override fun isExpired(): Boolean = false

        override fun advance(amount: Float) {
            texX += amount
            if (texX >= 1f) texX -= 1f
            rotate += amount * 10f
            if (rotate >= 360f) rotate -= 360f

            sprite?.texX = texX
            spriteTwo?.texX = texX
        }

        override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
            return EnumSet.of(CampaignEngineLayers.TERRAIN_9, CampaignEngineLayers.TERRAIN_1) // above, so below
        }

        override fun render(
            layer: CampaignEngineLayers,
            viewport: ViewportAPI
        ) {
            if (!star.containingLocation.isCurrentLocation) return
            if (layer == CampaignEngineLayers.TERRAIN_1) return

            if (sprite == null) {
                Global.getSettings().loadTexture("graphics/fx/beam_rough2_core.png")
                sprite = Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png")
                //sprite = Global.getSettings().getSprite("graphics/planets/arid.jpg")
            }
            if (spriteTwo == null) {
                Global.getSettings().loadTexture("graphics/fx/beam_rough2_fringe.png")
                spriteTwo = Global.getSettings().getSprite("graphics/fx/beam_rough2_fringe.png")
            }

            val fov = 60f
            val width = 15f
            val segments = 16

            // ---- Step 1: gather info about the game's camera ----------------

            // How many WORLD units tall/wide the visible screen area is right now
            // (this already accounts for the player's zoom level).
            val h = viewport.getVisibleHeight()
            val w = viewport.getVisibleWidth()


            // The world position the camera is centered on.
            val c = viewport.getCenter()


            // ---- Step 2: pick the camera distance so z = 0 matches the 2D game
            //
            // We want things lying flat on the game plane (z = 0) to appear at the
            // SAME size and position as the game's normal 2D drawing. Otherwise
            // our line wouldn't line up with ships and the rest of the world.
            //
            // Trigonometry: a camera with vertical field of view 'fov' sees a
            // height of 2 * dist * tan(fov/2) at distance 'dist'. We want that to
            // equal the visible height 'h', so:  dist = (h/2) / tan(fov/2)
            val dist = (h * 0.5f) / tan(Math.toRadians((fov * 0.5f).toDouble())).toFloat()


            // ---- Step 3: choose the line's endpoints ------------------------
            //
            // IMPORTANT: the camera sits at z = dist. Anything with z >= dist is
            // at or BEHIND the camera and won't be drawn. So the top tip must be
            // comfortably below 'dist'. Tying the endpoints to 'dist' keeps this
            // true at every zoom level.
            val zTop = dist // toward camera (positive z)
            val zBottom = 500f // away from camera (negative z)


            // ---- Step 4: save OpenGL state we're about to change ------------
            //
            // OpenGL is a "state machine": settings stay set until changed. We're
            // about to change several, so we save them now and restore at the
            // end, so we don't break the game's other rendering.
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)


            // ---- Step 5: swap in our perspective PROJECTION matrix ----------

            // "I'm now editing the projection matrix."
            GL11.glMatrixMode(GL11.GL_PROJECTION)


            // Save a copy of the game's current projection (its orthographic one)
            // on the stack, so we can restore it perfectly later.
            GL11.glPushMatrix()


            // Reset the projection matrix to the identity matrix ("do nothing").
            // glu* functions MULTIPLY into the current matrix, so we need to
            // start from a clean slate or we'd stack our perspective on top of
            // the game's orthographic projection and get garbage.
            GL11.glLoadIdentity()


            // Build a perspective projection:
            //   fov      - vertical field of view in degrees
            //   w / h    - aspect ratio (width divided by height), so circles
            //              don't get stretched into ovals
            //   10f      - NEAR clip plane: anything closer than 10 units to the
            //              camera is not drawn
            //   dist * 4 - FAR clip plane: anything farther than this is not
            //              drawn. Must be large enough to include our bottom tip
            //              (which is dist + 1.5*dist = 2.5*dist away).
            GLU.gluPerspective(fov, w / h, 5f, dist * 4f)


            // ---- Step 6: set up the MODELVIEW matrix (camera placement) -----

            // "I'm now editing the modelview matrix."
            GL11.glMatrixMode(GL11.GL_MODELVIEW)


            // Save the game's current modelview matrix, to restore later.
            GL11.glPushMatrix()


            // Reset to identity so the game's existing transforms (translations,
            // rotations from its own camera) don't leak into our view.
            //GL11.glLoadIdentity()


            // Place and aim the camera. gluLookAt takes three groups of 3 numbers:
            //   eye    (c.x, c.y, dist) - where the camera IS: directly above the
            //                             center of the screen, 'dist' units up
            //                             along the Z axis
            //   target (c.x, c.y, 0)    - what it looks AT: the same x/y on the
            //                             game plane, straight down
            //   up     (0, 1, 0)        - which direction is "up" on screen: +Y,
            //                             matching the game's normal orientation
            // Because the eye follows the game camera's x/y (c), panning the game
            // camera pans ours too, and the line's tips shift as perspective says.
            GLU.gluLookAt(
                c.x, c.y, dist,
                c.x, c.y, 0f,
                0f, 1f, 0f
            )


            // ---- Step 7: set drawing state for a plain colored line ---------

            // Turn off depth testing. Depth testing hides things behind other
            // things using a "depth buffer". Starsector's 2D drawing doesn't use
            // it meaningfully, so leaving it on could make our line vanish or
            // fail to show against existing pixels.
            GL11.glDisable(GL11.GL_DEPTH_TEST)


            // Turn off texturing so a leftover bound texture doesn't tint or
            // replace our solid color.
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_CULL_FACE)
            //sprite?.bindTexture()

            // Enable transparency (alpha blending) with the standard formula:
            // result = ourColor * ourAlpha + background * (1 - ourAlpha)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);


            // Smooth (antialiased) lines, and 3-pixel thickness on screen.
            // Note: line width in OpenGL 1.1 is in screen pixels and does NOT
            // shrink with distance, even in perspective mode.
            //GL11.glEnable(GL11.GL_QUADS)
            //GL11.glLineWidth(3f)


            // ---- Step 8: draw the line in 3D --------------------------------

            //GL11.glLineWidth(50f)
            // Start a list of vertices; GL_LINES draws a segment per pair.
            //GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glBegin(GL11.GL_QUAD_STRIP)

            // Near half (z = 0 up toward the camera), bright light blue.
            // glColor4f = red, green, blue, alpha (each 0.0 to 1.0).
            /*GL11.glColor4f(0.4f, 0.8f, 1f, 1f)


            // glVertex3f takes x, y, AND z now. (0, 0, 0) is the point where the
            // line pierces the game plane at world origin.
            GL11.glVertex3f(-10f, -10f, zTop)
            GL11.glVertex3f(10f, -10f, zBottom)
            GL11.glVertex3f(-10f, 10f, zTop)
            GL11.glVertex3f(10f, 10f, zTop)
            GL11.glVertex3f(10f, -10f, zTop)


            // Far half (z = 0 down away from the camera), same color but dimmer
            // (alpha 0.4) as a visual depth cue.
            GL11.glColor4f(0.4f, 0.8f, 1f, 0.4f)
            GL11.glVertex3f(10f, 10f, zBottom)
            GL11.glVertex3f(-10f, 10f, zBottom)
            //GL11.glVertex3f(0f, 0f, 0f)
            //GL11.glVertex3f(0f, 0f, zBottom)
            GL11.glVertex3f(-10f, -10f, zBottom)*/

            for (i in 0..segments) {
                // t goes from 0.0 (first cross-section) to 1.0 (last).

                val t = i.toFloat() / segments
                val next = ((i.toFloat() / segments) + 1f) / 2f

                val parabola = (500f * 0.5f) + (-0.2f) * next.pow(2f)
                // The height (z) of this cross-section: slide from bottom to top.
                val z = zBottom + (zTop - zBottom) * t
                val x = (250f * t)
                val y = (250f * t)

                // Vertex color/alpha. White (1,1,1) leaves the texture unchanged
                // (because of GL_MODULATE). The alpha fades the far end out:
                // t = 0 (far) is 0.2 opaque, t = 1 (near) is fully opaque. Change
                // the color values to tint the texture, or set alpha to 1 for no fade.
                GL11.glColor4f(1f, 1f, 1f, 0.2f + 0.8f * t)
                GL11.glTexCoord2f(0f, t)
                GL11.glVertex3f(x - 50f, y - 50f, parabola)
                GL11.glColor4f(1f, 1f, 1f, 0.2f + 0.8f * t)
                GL11.glTexCoord2f(0f, t)
                GL11.glVertex3f(x + 50f, y + 50f, parabola)
            }

            // End the vertex list and send the lines to the GPU.
            GL11.glEnd()


            // ---- Step 9: restore everything exactly as the game had it ------
            //
            // ORDER MATTERS: each glPopMatrix pops whichever stack is selected by
            // the current glMatrixMode, so we must select the right one first.

            // Restore the game's original PROJECTION matrix.
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPopMatrix()


            // Restore the game's original MODELVIEW matrix.
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPopMatrix()


            // Restore all the other settings we changed (depth test, blending,
            // textures, line width, ...) to what they were at glPushAttrib.
            // Without this, later game rendering would be broken or look wrong.
            GL11.glPopAttrib()
        }

        private fun draw(viewport: ViewportAPI, sprite: SpriteAPI, layer: CampaignEngineLayers) {
            if (layer == CampaignEngineLayers.TERRAIN_9) {
                var left = QUAD_COUNT
                var currX = 0f
                var currY = 0f
                var currWidth = 50f
                while (left-- > 0f) {
                    GL11.glVertex3f(-currWidth, currY, 0f)
                    GL11.glVertex3f(currWidth, currY, 0f)
                }
            }
            // the bezier curve needs to be quartic - five points\
            // 3 may also work but i lose control over the curve
            // maybe 4? not sure.
            // quadradic (four) can work, but five seems the easist to wrap m head around
            // use parallax here to simulate 3d movement
            // we always start from the north pole and project back

            val amplitude = 50f // TODO make const
            // NEVER EXCEED AMPLITUDE. EVER

            // TODO cache

            // ALL LINES ARE STRAIGHT UNLESS PARALAXXED. YES. THIS IS THE WAY.
            // a second line will always be under and be under all terrain/fleets/the star etc
            // get the mid point and use that as the start for the next one

            // mid point will never shift so just cache it

            // how to do parallax:
            // top layer should move more when viewport moves
            // bottom layer should be more static

            val p1 = Vector3f(0f, 0f, 0f)
            val p3 = Vector3f(midPoint.x, midPoint.y, 0f)

            val view = viewport.center

            // get the dist from our midpoint, get the point away from the viewport from same(?) dist
            // might be best to do 2 3d renders anyway. ugh
            val p2 = p3.negate(Vector3f(view.x, view.y, 0f))

            val array = floatArrayOf(
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z,
            )
            val buffer = BufferUtils.createFloatBuffer(array.size)
            for (entry in array) {
                buffer.put(entry)
            }

            GL11.glMap1f(
                GL11.GL_MAP1_VERTEX_3,
                0f,
                1f,
                3,
                4,
                buffer
            )
            GL11.glEnable(GL11.GL_MAP1_VERTEX_3)

            var i = 0f
            while (i++ < 250f) {
                GL11.glEvalCoord1f(i / 250f) // TODO ripped from https://www.geeksforgeeks.org/cpp/bezier-curves-in-opengl/ (wtf is this code doing)
            }
        }

        /*fun getParallaxValue(viewport: ViewportAPI): Float {
            val center = viewport.center
            val point =
        }*/

    }
}
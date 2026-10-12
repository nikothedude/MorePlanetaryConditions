package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.RingBandAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventMouseButton
import com.fs.starfarer.api.input.InputEventType
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import com.fs.state.AppDriver
import data.niko_MPC_modPlugin
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisMissileEntityPlugin
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.niko_MPC_specialProcGenHandler
import data.scripts.campaign.plugins.MPC_vignetteRenderer
import data.scripts.campaign.singularity.MPC_energyFieldInstance
import data.scripts.campaign.singularity.MPC_singularityHyperspaceProximityChecker
import data.scripts.campaign.sinkhole.MPC_sinkholeTerrain
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.MPC_abyssUtils
import data.utilities.niko_MPC_ids
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.*
import org.magiclib.util.MagicCampaign
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import data.utilities.niko_MPC_dialogUtils.getChildrenCopy
import data.utilities.niko_MPC_reflectionUtils
import data.utilities.niko_MPC_settings
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector3f
import java.nio.FloatBuffer
import java.util.EnumSet
import kotlin.collections.random

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerFleet = Global.getSector().playerFleet

        //LunaCampaignRenderer.addTransientRenderer(MPC_vignetteRenderer())
        LunaCampaignRenderer.addTransientRenderer(MPC_bipoleMagneticLine(playerFleet, 0f))

        return BaseCommand.CommandResult.SUCCESS
    }

    // if we are under, we must be under the star and the fleet and terrain
    // TODO - add a terrain effect if you fly to the intersection point
    // uh oh! major damage
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
        }

        override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
            return EnumSet.of(CampaignEngineLayers.TERRAIN_9, CampaignEngineLayers.TERRAIN_1) // above, so below
        }

        override fun render(
            layer: CampaignEngineLayers,
            viewport: ViewportAPI
        ) {
            if (!star.containingLocation.isCurrentLocation) return

            if (sprite == null) {
                Global.getSettings().loadTexture("graphics/fx/beam_rough2_core.png")
                sprite = Global.getSettings().getSprite("graphics/fx/beam_rough2_core.png")
            }
            if (spriteTwo == null) {
                Global.getSettings().loadTexture("graphics/fx/beam_rough2_fringe.png")
                spriteTwo = Global.getSettings().getSprite("graphics/fx/beam_rough2_fringe.png")
            }

            sprite!!.bindTexture()
            GL11.glEnable(GL11.GL_3D)
            GL11.glEnable(GL11.GL_BLEND)
            //GL11.glEnable(GL11.GL_PROJECTION_MATRIX)
            //GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
            GL11.glPushMatrix()

            GL11.glTranslatef(star.location.x, star.location.y, 0f)
            GL11.glRotatef(star.facing, 0f, 0f, 1f) // todo test
            //GL11.glRotatef(rotate, 0f, -1f, 0f)

            GL11.glBegin(GL11.GL_QUAD_STRIP)
            var i = 0f
            var currSprite = sprite!!
            while (i++ < 2f) {
                if (i == 2f) {
                    currSprite = spriteTwo!!
                }
                currSprite.texX = texX
                currSprite.setSize(20f, 20f)
                currSprite.bindTexture()
                //draw(viewport, currSprite, layer)

                GL11.glVertex3f(50f, 0f, -1f)
                GL11.glVertex3f(-50f, 0f, -1f)
                GL11.glVertex3f(0f, 50f, 1f)
                GL11.glVertex3f(0f, -50f, 1f)
            }

            GL11.glEnd()
            GL11.glPopMatrix()
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
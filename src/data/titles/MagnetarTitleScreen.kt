package data.titles

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.CombatEngine
import data.niko_MPC_modPlugin
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript
import lunalib.lunaTitle.BaseLunaTitleScreenPlugin
import lunalib.lunaUtil.LunaCommons
import java.util.EnumSet

class MagnetarTitleScreen: BaseLunaTitleScreenPlugin() {

    val renderer = PlanetRenderer(Global.getSettings().allPlanetSpecs.find { it.planetType == "MPC_star_magnetar" }!!, 0f, 0f, 180f)
    val lineRenderer = LineRenderer()

    class LineRenderer: BaseCombatLayeredRenderingPlugin() {
        val lines = niko_MPC_magnetarStarScript.generateFieldLines(null, null)

        override fun getActiveLayers(): EnumSet<CombatEngineLayers?>? {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER, CombatEngineLayers.BELOW_PLANETS)
        }

        override fun render(layer: CombatEngineLayers?, viewport: ViewportAPI?) {
            super.render(layer, viewport)

            if (viewport == null) return

            if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) {
                lines.forEach { it.render(CampaignEngineLayers.TERRAIN_9, viewport) }
            } else if (layer == CombatEngineLayers.BELOW_PLANETS) {
                lines.forEach { it.render(CampaignEngineLayers.TERRAIN_1, viewport) }
            }
        }

        override fun advance(amount: Float) {
            super.advance(amount)

            lines.forEach { it.advance(amount) }
        }
    }

    override fun pickBasedOnSystemCondition(
        lastSystemID: String,
        lastSystemTags: ArrayList<String>
    ): Boolean {
        return lastSystemTags.contains("theme_magnetar")
    }

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)
        if (engine == null) return

        CombatEngine.replaceBackground("graphics/backgrounds/background_galatia.jpg", true)
        Global.getSoundPlayer().playCustomMusic(1, 1, "MPC_magnetarAmbience", true)

        engine.addLayeredRenderingPlugin(renderer)
        engine.addLayeredRenderingPlugin(lineRenderer)
    }

    override fun advance(
        amount: Float,
        events: List<InputEventAPI?>?
    ) {
        var viewport = Global.getCombatEngine().viewport
        viewport.viewMult = 2f
        viewport.isExternalControl = true

        var width = Global.getSettings().screenWidth * 2
        var height = Global.getSettings().screenHeight * 2

        viewport.set(0f-width/1.8f, 0f-height/1.2f, width, height)
    }

    override fun renderInWorldCoords(viewport: ViewportAPI?) {
        return
    }

    override fun renderInUICoords(viewport: ViewportAPI?) {
        return
    }

    override fun getWeight(): Float {
        if (LunaCommons.getBoolean(niko_MPC_modPlugin.modId, "MPC_hasFoundMagnetar") != true) return 0f
        return super.getWeight()
    }
}
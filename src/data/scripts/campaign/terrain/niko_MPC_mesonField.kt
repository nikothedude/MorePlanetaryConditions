package data.scripts.campaign.terrain

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TerrainAIFlags
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HTScanFactor
import com.fs.starfarer.api.impl.campaign.intel.events.ht.HyperspaceTopographyEventIntel
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer.AuroraRendererDelegate
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager
import com.fs.starfarer.api.impl.campaign.terrain.FlareManager.FlareManagerDelegate
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_HTFactorTracker
import data.utilities.niko_MPC_settings
import lunalib.lunaSettings.LunaSettings
import niko.MCTE.scripts.everyFrames.combat.terrainEffects.mesonField.mesonFieldEffectScript
import niko.MCTE.settings.MCTE_settings
import niko.MCTE.utils.MCTE_ids
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

class niko_MPC_mesonField: BaseRingTerrain(), AuroraRendererDelegate, FlareManagerDelegate, niko_MPC_scannableTerrain {

    class mesonFieldParams(
        bandWidthInEngine: Float,
        middleRadius: Float,
        relatedEntity: SectorEntityToken,
        var innerRadius: Float,
        var outerRadius: Float,
        var baseColor: Color = baseColors.random(),
        var auroraFrequency: Float = 0f,
        @Transient
        var auroraColorRange: List<Color> = auroraColors.random().toList(),

        ): RingParams(bandWidthInEngine, middleRadius, relatedEntity, null) {
        var c: String? = null

        fun readResolve(): Any {
            auroraColorRange = if (c != null) {
                Misc.colorsFromString(c)
            } else {
                ArrayList()
            }
            return this
        }

        fun writeReplace(): Any {
            c = Misc.colorsToString(auroraColorRange)
            return this
        }

    }

    companion object {
        const val HYPERSPACE_TOPOGRAPHY_POINTS = 10

        const val NORMAL_DETECTED_MULT = 7f

        const val STORM_DETECTED_MULT = 0.5f
        const val STORM_SENSOR_MULT = 7f

        val baseColors = arrayOf(
            Color(237, 246, 5, 43),
            //Color(43, 239, 8, 45),
        )
        var auroraColors = arrayOf(
            arrayOf(
                Color(180, 110, 210),
                Color(150, 140, 190),
                Color(140, 190, 210),
                Color(90, 200, 170),
                Color(65, 230, 160),
                Color(20, 220, 70)
            ), arrayOf(
                Color(50, 20, 110, 130),
                Color(150, 30, 120, 150),
                Color(200, 50, 130, 190),
                Color(250, 70, 150, 240),
                Color(200, 80, 130, 255),
                Color(75, 0, 160),
                Color(127, 0, 255)
            ), arrayOf(
                Color(90, 180, 140),
                Color(130, 145, 190),
                Color(165, 110, 225),
                Color(95, 55, 240),
                Color(45, 0, 250),
                Color(20, 0, 240),
                Color(10, 0, 150)
            ), arrayOf(
                Color(90, 180, 40),
                Color(130, 145, 90),
                Color(165, 110, 145),
                Color(95, 55, 160),
                Color(45, 0, 130),
                Color(20, 0, 130),
                Color(10, 0, 150)
            ), arrayOf(
                Color(50, 20, 110, 130),
                Color(150, 30, 120, 150),
                Color(200, 50, 130, 190),
                Color(250, 70, 150, 240),
                Color(200, 80, 130, 255),
                Color(75, 0, 160),
                Color(127, 0, 255)
            ), arrayOf(
                Color(55, 60, 140),
                Color(65, 85, 155),
                Color(175, 105, 165),
                Color(90, 130, 180),
                Color(105, 150, 190),
                Color(120, 175, 205),
                Color(135, 200, 220)
            )
        )
    }

    //	public static float BURN_MULT = 0.5f;
    //	public static float BURN_MULT_AURORA = 0.25f;
    @Transient
    var texture: SpriteAPI? = null

    @Transient
    var color: Color? = null
    var renderer: AuroraRenderer? = null

    var flareManager: FlareManager? = null

    override fun readResolve(): Any {
        super.readResolve()
        texture = Global.getSettings().getSprite("terrain", "aurora")
        layers = EnumSet.of(CampaignEngineLayers.TERRAIN_7)
        if (renderer == null) {
            renderer = AuroraRenderer(this)
        }
        if (flareManager == null) {
            flareManager = FlareManager(this)
        }
        return this
    }

    fun getCastedParams(): mesonFieldParams {
        return params as mesonFieldParams
    }

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)
        this.params = param as mesonFieldParams?
        name = params.name
        if (name == null) {
            name = "Meson Field"
        }
    }

    override fun getNameForTooltip(): String {
        return if (flareManager!!.isInActiveFlareArc(Global.getSector().playerFleet)) {
            "Meson Storm"
        }
        else super.getNameForTooltip()
    }

    fun writeReplace(): Any {
        return this
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? {
        return layers
    }


    override fun advance(amount: Float) {
        super.advance(amount)
        renderer!!.advance(amount)
        flareManager!!.advance(amount)
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI) {
        renderer!!.render(viewport.alphaMult)
    }

    override fun getRenderRange(): Float {
        val curr = flareManager!!.activeFlare
        if (curr != null) {
            val outerRadiusWithFlare = computeRadiusWithFlare(flareManager!!.activeFlare)
            return outerRadiusWithFlare + 200f
        }
        return super.getRenderRange()
    }

    override fun shouldPlayLoopOne(): Boolean {
        return super.shouldPlayLoopOne() && !flareManager!!.isInActiveFlareArc(Global.getSector().playerFleet)
    }

    override fun shouldPlayLoopTwo(): Boolean {
        return super.shouldPlayLoopTwo() && flareManager!!.isInActiveFlareArc(Global.getSector().playerFleet)
    }


    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        if (entity is CampaignFleetAPI) {
            val fleet = entity
            if (flareManager!!.isInActiveFlareArc(fleet)) {
                fleet.stats.addTemporaryModMult(
                    0.1f, modId + "_2",
                    "Inside meson storm", STORM_SENSOR_MULT,
                    fleet.stats.sensorRangeMod
                )
                fleet.stats.removeTemporaryMod(modId + "_6")
                fleet.stats.addTemporaryModMult(
                    0.1f, modId + "_5",
                    "Inside meson storm", STORM_DETECTED_MULT,
                    fleet.stats.detectedRangeMod
                )
            } else {
                fleet.stats.removeTemporaryMod(modId + "_5")
                fleet.stats.addTemporaryModMult(
                    0.1f, modId + "_6",
                    "Inside strong meson field", NORMAL_DETECTED_MULT,
                    fleet.stats.detectedRangeMod
                )
            }
        }
    }

    override fun containsPoint(point: Vector2f?, radius: Float): Boolean {
        if (flareManager!!.isInActiveFlareArc(point)) {
            val outerRadiusWithFlare = computeRadiusWithFlare(flareManager!!.activeFlare)
            val dist = Misc.getDistance(entity.location, point)
            if (dist > outerRadiusWithFlare + radius) return false
            return dist + radius >= params.middleRadius - params.bandWidthInEngine / 2f
        }
        return super.containsPoint(point, radius)
    }

    private fun computeRadiusWithFlare(flare: FlareManager.Flare): Float {
        val inner = auroraInnerRadius
        val outer = params.middleRadius + params.bandWidthInEngine * 0.5f
        var thickness = outer - inner
        thickness *= flare.extraLengthMult
        thickness += flare.extraLengthFlat
        return inner + thickness
    }

    override fun getNameColor(): Color? {
        val bad = Misc.getNegativeHighlightColor()
        val base = super.getNameColor()
        //bad = Color.red;
        //return Misc.interpolateColor(base, bad, Global.getSector().getCampaignUI().getSharedFader().getBrightness() * 1f);
        return super.getNameColor()
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean) {
        val pad = 10f
        val small = 5f
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val fuel = Global.getSettings().getColor("progressBarFuelColor")
        val bad = Misc.getNegativeHighlightColor()
        tooltip.addTitle("Meson Field")
        tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).text1, pad)
        val player = Global.getSector().playerFleet

//		float sensorMult = SENSOR_MULT;
//		float burnMult = getAdjustedMult(player, BURN_MULT);
//		String extraText = "";
//		if (flareManager.isInActiveFlareArc(Global.getSector().getPlayerFleet())) {
//			sensorMult = SENSOR_MULT_AURORA;
//			burnMult = getAdjustedMult(player, BURN_MULT_AURORA);
//			//extraText = " The sensor penalty is currently increased due to being inside a magnetic storm.";
//			extraText = " The sensor and travel speed penalties are currently increased due to being inside a magnetic storm.";
//		}
        val isStorm = flareManager!!.isInActiveFlareArc(Global.getSector().playerFleet)
        var nextPad = pad
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad)
            nextPad = small
        }
        if (isStorm) {
            tooltip.addPara(
                "The meson field is dense enough here to cross the Burivil Threshold, and thus boost sensor range of " +
                        "fleets in it by %s.",
                pad,
                highlight,
                "${(STORM_SENSOR_MULT * 100).toInt()}%"
            )

            tooltip.addPara(
                "Additionally, due to the proximity limitations of the Burivil theory, far-away sensors struggle to penetrate " +
                        "the meson field, decreasing detected-at range of fleets inside by %s.",
                pad,
                highlight,
                "${(STORM_DETECTED_MULT * 100).toInt()}%"
            )
        } else {
            tooltip.addPara(
                "Increases the range at which fleets inside can be detected by %s.",
                pad,
                highlight,
                "${(NORMAL_DETECTED_MULT * 100).toInt()}%"
            )
            tooltip.addPara(
                "%s are known to boost sensor range of fleets inside.",
                pad,
                highlight,
                "Meson storms"
            )
        }

//		String sensorMultStr = Misc.getRoundedValue(1f - sensorMult);
//		String burnMultStr = Misc.getRoundedValue(1f - burnMult);
//		tooltip.addPara("Your fleet's sensor range is reduced by %s. Your fleet's speed is reduced by %s." + extraText, pad,
//				highlight,
//				"" + (int) ((1f - sensorMult) * 100) + "%",
//				"" + (int) ((1f - burnMult) * 100) + "%"
//				);

//		tooltip.addPara("Reduces the range at which fleets inside it can be detected by %s. Also reduces fleet sensor range by %s." + extraText, nextPad,
//				highlight,
//				"" + (int) ((1f - VISIBLITY_MULT) * 100) + "%",
//				"" + (int) ((1f - sensorMult) * 100) + "%"
//		);
        if (expanded) {
            tooltip.addSectionHeading("Combat", Alignment.MID, pad)
            if (niko_MPC_settings.MCTE_loaded && MCTE_settings.MESON_FIELD_ENABLED) {
                mesonFieldEffectScript.modifyTerrainTooltip(tooltip, nextPad, isStorm)
            } else {
                tooltip.addPara("No combat effects.", nextPad)
            }
        }
    }

    override fun isTooltipExpandable(): Boolean {
        return true
    }

    override fun getTooltipWidth(): Float {
        return 350f
    }

    override fun getTerrainName(): String? {
        return if (flareManager!!.isInActiveFlareArc(Global.getSector().playerFleet)) {
            "Meson Storm"
        } else super.getTerrainName()
    }

    override fun getEffectCategory(): String {
        return "meson_field-like"
    }

    override fun getAuroraAlphaMultForAngle(angle: Float): Float {
        return 1f
    }

    override fun getAuroraBandWidthInTexture(): Float {
        return 256f
        //return 512f;
    }

    override fun getAuroraCenterLoc(): Vector2f? {
        return params.relatedEntity.location
    }

    override fun getAuroraColorForAngle(angle: Float): Color? {
        return if (flareManager!!.isInActiveFlareArc(angle)) {
            flareManager!!.getColorForAngle(getCastedParams()!!.baseColor, angle)
        } else getCastedParams()!!.baseColor
    }

    override fun getAuroraInnerRadius(): Float {
        return getCastedParams()!!.innerRadius
    }

    override fun getAuroraOuterRadius(): Float {
        return getCastedParams()!!.outerRadius
    }

    override fun getAuroraShortenMult(angle: Float): Float {
        return 0f + flareManager!!.getShortenMod(angle)
        //return 0.3f + flareManager.getShortenMod(angle);
    }

    override fun getAuroraInnerOffsetMult(angle: Float): Float {
        return flareManager!!.getInnerOffsetMult(angle)
    }

    override fun getAuroraTexPerSegmentMult(): Float {
        return 1f
    }

    override fun getAuroraTexture(): SpriteAPI? {
        return texture
    }

    override fun getAuroraThicknessFlat(angle: Float): Float {
        return if (flareManager!!.isInActiveFlareArc(angle)) {
            flareManager!!.getExtraLengthFlat(angle)
        } else 0f
    }

    override fun getAuroraThicknessMult(angle: Float): Float {
        return if (flareManager!!.isInActiveFlareArc(angle)) {
            flareManager!!.getExtraLengthMult(angle)
        } else 1f
    }


    override fun getFlareArcMax(): Float {
        return 80f
    }

    override fun getFlareArcMin(): Float {
        return 30f
    }

    override fun getFlareColorRange(): List<Color?>? {
        return getCastedParams()!!.auroraColorRange
    }

    override fun getFlareExtraLengthFlatMax(): Float {
        return 0f
    }

    override fun getFlareExtraLengthFlatMin(): Float {
        return 0f
    }

    override fun getFlareExtraLengthMultMax(): Float {
        return 1f
    }

    override fun getFlareExtraLengthMultMin(): Float {
        return 1f
    }

    override fun getFlareFadeInMax(): Float {
        return 2f
    }

    override fun getFlareFadeInMin(): Float {
        return 1f
    }

    override fun getFlareFadeOutMax(): Float {
        return 10f
    }

    override fun getFlareFadeOutMin(): Float {
        return 7f
    }

    override fun getFlareOccurrenceAngle(): Float {
        return 0f
    }

    override fun getFlareOccurrenceArc(): Float {
        return 360f
    }

    override fun getFlareProbability(): Float {
        return getCastedParams()!!.auroraFrequency
    }

    override fun getFlareSmallArcMax(): Float {
        return 20f
    }

    override fun getFlareSmallArcMin(): Float {
        return 10f
    }

    override fun getFlareSmallExtraLengthFlatMax(): Float {
        return 0f
    }

    override fun getFlareSmallExtraLengthFlatMin(): Float {
        return 0f
    }

    override fun getFlareSmallExtraLengthMultMax(): Float {
        return 1f
    }

    override fun getFlareSmallExtraLengthMultMin(): Float {
        return 1f
    }

    override fun getFlareSmallFadeInMax(): Float {
        return 1f
    }

    override fun getFlareSmallFadeInMin(): Float {
        return 0.5f
    }

    override fun getFlareSmallFadeOutMax(): Float {
        return 10f
    }

    override fun getFlareSmallFadeOutMin(): Float {
        return 6f
    }

    override fun getFlareShortenFlatModMax(): Float {
        return 0.8f
    }

    override fun getFlareShortenFlatModMin(): Float {
        return 0.8f
    }

    override fun getFlareSmallShortenFlatModMax(): Float {
        return 0.8f
    }

    override fun getFlareSmallShortenFlatModMin(): Float {
        return 0.8f
    }

    override fun getFlareMaxSmallCount(): Int {
        return 2
    }

    override fun getFlareMinSmallCount(): Int {
        return 7
    }

    override fun getFlareSkipLargeProbability(): Float {
        return 0.0f
    }

    override fun getFlareCenterEntity(): SectorEntityToken? {
        return entity
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return true
    }

    override fun getAuroraBlocker(): RangeBlockerUtil? {
        return null
    }

    override fun onScanned(
        factorTracker: niko_MPC_HTFactorTracker,
        playerFleet: CampaignFleetAPI,
        sensorBurstAbility: AbilityPlugin
    ) {
        if (!containsEntity(playerFleet)) return

        val id = entity.id

        if (factorTracker.scanned.contains(id)) {
            factorTracker.reportNoDataAcquired("Meson Field already scanned")
        } else {
            HyperspaceTopographyEventIntel.addFactorCreateIfNecessary(
                HTScanFactor("Meson Field scanned", HYPERSPACE_TOPOGRAPHY_POINTS), null
            )
            factorTracker.scanned.add(id)
        }
    }
}
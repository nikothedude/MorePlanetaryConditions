package data.scripts.campaign.econ.industries.missileLauncher

import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.RingRenderer
import com.fs.starfarer.api.loading.Description
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.niko_MPC_magnetarField.Companion.DETECTED_MULT
import data.scripts.campaign.magnetar.niko_MPC_magnetarField.Companion.DETECTED_MULT_STORM
import data.scripts.campaign.magnetar.niko_MPC_magnetarField.Companion.SENSOR_RANGE_MULT
import data.scripts.campaign.magnetar.niko_MPC_magnetarField.Companion.SENSOR_RANGE_MULT_STORM
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_fleetUtils.getApproximateECMValue
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_stringUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.UUID
import kotlin.math.PI

class MPC_missileTargettingTerrain: BaseRingTerrain() {

    class MissileTerrainParams(
        bandWidthInEngine: Float,
        middleRadius: Float,
        relatedEntity: SectorEntityToken,
        val parent: MPC_orbitalMissileLauncher
    ): RingParams(bandWidthInEngine, middleRadius, relatedEntity, null)

    lateinit var castedParams: MissileTerrainParams

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        castedParams = params as MissileTerrainParams
    }

    @Transient
    var rr: RingRenderer? = null
    val placeholderColor = Color(255, 200, 50, 255)
    override fun renderOnMap(factor: Float, alphaMult: Float) {
        if (params == null) return
        if (!castedParams.parent.renderTerrain) return
        if (rr == null) {
            rr = RingRenderer("systemMap", "map_asteroid_belt")
        }
        rr!!.render(
            entity.location,
            castedParams.parent.getMaxTargettingRange() - 350f,
            castedParams.parent.getMaxTargettingRange(),
            placeholderColor,
            false, factor, alphaMult
        )
    }

    override fun renderOnRadar(radarCenter: Vector2f?, factor: Float, alphaMult: Float) {
        if (radarCenter == null) return

        GL11.glPushMatrix();
		GL11.glTranslatef(-radarCenter.x * factor, -radarCenter.y * factor, 0f);
        renderOnMap(factor, alphaMult);
		GL11.glPopMatrix();

        /*if (params == null) return
        if (!castedParams.parent.renderTerrain) return
        if (rr == null) {
            rr = RingRenderer("systemMap", "map_asteroid_belt")
        }
        rr!!.render(
            entity.location,
            castedParams.parent.getMaxTargettingRange() - 500f,
            castedParams.parent.getMaxTargettingRange(),
            placeholderColor,
            false, factor, alphaMult
        )*/
    }

    fun getBaseName(): String {
        return castedParams.parent.getTerrainName() ?: "ERROR"
    }

    override fun getNameForTooltip(): String? {
        val playerFleet = Global.getSector().playerFleet
        val baseName = getBaseName()
        val detectedLevel = castedParams.parent.getDetectionLevel(playerFleet)
        var detectedString = ""
        if (castedParams.parent.isHostileTo(playerFleet)) {
            detectedString = if (detectedLevel >= 1f) "DETECTED!" else niko_MPC_stringUtils.toPercent(detectedLevel.roundNumTo(2))
        } else {
            detectedString = "Friendly"
        }

        return "$baseName ($detectedString)"
    }

    override fun getTerrainName(): String? {
        return nameForTooltip
    }

    override fun getNameColor(): Color? {
        val baseColor = castedParams.parent.getFaction().baseUIColor
        val playerFleet = Global.getSector().playerFleet
        if (castedParams.parent.isHostileTo(playerFleet)) {
            val special = Misc.getNegativeHighlightColor()
            val base = baseColor
            //bad = Color.red;
            return Misc.interpolateColor(base, special, Global.getSector().campaignUI.sharedFader.brightness * 1f)
        }

        return baseColor
    }

    override fun containsPoint(point: Vector2f?, radius: Float): Boolean {
        val dist = MathUtils.getDistance(castedParams.parent.getHost(), point)
        return dist <= castedParams.parent.getMaxTargettingRange()
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

    override fun hasAIFlag(flag: Any?): Boolean {
        return false
    }

    override fun getEffectCategory(): String? {
        return null
    }
    val UID = Misc.genUID()
    override fun getTerrainId(): String? {
        return UID
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        if (tooltip == null) return

        val codex = Global.getSettings().isShowingCodex
        val pad = 10f
        val small = 5f
        val gray = Misc.getGrayColor()
        val highlight = Misc.getHighlightColor()
        val fuel = Global.getSettings().getColor("progressBarFuelColor")
        val bad = Misc.getNegativeHighlightColor()

        val baseName = getBaseName()
        tooltip.addTitle(baseName)
        //tooltip.addPara(Global.getSettings().getDescription(getTerrainId(), Description.Type.TERRAIN).text1, pad)

        var nextPad = pad
        if (expanded) {
            tooltip.addSectionHeading("Travel", Alignment.MID, pad)
            nextPad = small
        }

        val playerFleet = Global.getSector().playerFleet
        val parent = castedParams.parent
        val hostile = parent.isHostileTo(playerFleet)
        val inTextNoun = getDescNoun()
        tooltip.addPara(
            "Your fleet is in range of %s, $inTextNoun capable of striking hostile targets across the solar system with high-speed ordnance.",
            pad,
            castedParams.parent.getFaction().baseUIColor,
            baseName
        )
        if (hostile) {
            tooltip.addPara(
                "Your fleet is %s to the forces controlling %s, meaning you are in danger of missile strikes if detected.",
                nextPad,
                Misc.getNegativeHighlightColor(),
                "hostile", baseName
            ).setHighlightColors(
                Misc.getNegativeHighlightColor(),
                castedParams.parent.getFaction().baseUIColor,
            )

            if (castedParams.parent.usingDetection()) {
                tooltip.addPara(
                    "Awareness of your fleet's position will slowly grow while your sensor profile is above %s, capping at %s for a total rate of %s. " +
                            "If detection reaches %s, long-ranged strikes are likely to begin - though they can be evaded with a hard burn.",
                    nextPad,
                    Misc.getHighlightColor(),
                    "${castedParams.parent.minSensorProfile.toInt()}", "${castedParams.parent.maxSensorProfile.toInt()}",
                    "${(castedParams.parent.maxDetectionRate * 100f).toInt()}%/s", "100%"
                )
                tooltip.addPara(
                    "If beneath the threshold of %s sensor profile, detection will decay at a rate of %s.",
                    nextPad,
                    Misc.getHighlightColor(),
                    "${castedParams.parent.minSensorProfile.toInt()}", "${(castedParams.parent.detectionDecayRate * 100f).toInt()}%/s"
                )
            }

            tooltip.addPara(
                "Most incoming missiles' guidance can be scrambled using fleet-wide %s*. Doing so requires a minimum ECM score of %s, " +
                "and the use of one of the following abilities:",
                nextPad,
                Misc.getHighlightColor(),
                "ECM", "${MPC_aegisMissileEntityPlugin.MIN_ECM_NEEDED.toInt()}%"
            )
            tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
            for (id in MPC_aegisMissileEntityPlugin.ecmAbilities) {
                val spec = Global.getSettings().getAbilitySpec(id)
                if (spec == null) {
                    niko_MPC_debugUtils.log.warn("wrong ability spec id, no spec found: $id")
                    continue
                }

                val name = spec.name
                tooltip.addPara(
                    "%s",
                    0f,
                    Misc.getHighlightColor(),
                    name
                )
            }
            tooltip.setBulletedListMode(null)

            val ECMlevel = playerFleet.getApproximateECMValue()
            val enoughECM = (ECMlevel >= MPC_aegisMissileEntityPlugin.MIN_ECM_NEEDED)
            var activeColor: Color
            var activeString: String
            val usingECM = MPC_aegisMissileEntityPlugin.targetUsingECM(playerFleet)
            if (!enoughECM) {
                activeString = "Your fleet does not have enough ECM to counter incoming missiles."
                activeColor = Misc.getNegativeHighlightColor()
            } else if (usingECM) {
                activeString = "Your fleet's long-ranged ECM packages are engaged and defending against guided ordnance."
                activeColor = Misc.getPositiveHighlightColor()
            } else {
                activeString = "Your fleet's long-ranged ECM is inactive, and requires a high-powered sensor ability to be active."
                activeColor = Misc.getNegativeHighlightColor()
            }

            tooltip.addPara(
                activeString,
                nextPad
            ).color = activeColor

            tooltip.addPara(
                "*You have a fleet-wide ECM score of %s.",
                nextPad,
                Misc.getHighlightColor(),
                "${ECMlevel.roundNumTo(1).trimHangingZero()}%"
            ).color = Misc.getGrayColor()
        }
    }

    private fun getDescNoun(): String {
        return castedParams.parent.getDescNoun() ?: "ERROR"
    }

}
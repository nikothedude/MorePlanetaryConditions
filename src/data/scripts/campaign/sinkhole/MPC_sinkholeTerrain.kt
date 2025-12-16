package data.scripts.campaign.sinkhole

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain
import com.fs.starfarer.api.impl.campaign.terrain.RingRenderer
import com.fs.starfarer.api.loading.CampaignPingSpec
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_mathUtils.roundNumTo
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

class MPC_sinkholeTerrain: BaseTerrain() {

    companion object {
        fun addFieldToEntity(entity: SectorEntityToken, id: String, params: SinkholeParams): MPC_sinkholeTerrain? {
            val existing = entity.memoryWithoutUpdate["${INTERDICT_FIELD_MEMID_BASE}_$id"] as? MPC_sinkholeTerrain
            if (existing != null) return existing

            val terrain = addTerrainToEntity(entity, "MPC_interdictionField", params)

            entity.memoryWithoutUpdate["${INTERDICT_FIELD_MEMID_BASE}_$id"] = terrain?.plugin
            return terrain.plugin as? MPC_sinkholeTerrain
        }

        fun addTerrainToEntity(
            entity: SectorEntityToken,
            id: String,
            params: Any
        ): CampaignTerrainAPI {
            val terrain = entity.containingLocation.addTerrain(id, params) as CampaignTerrainAPI

            MPC_fleetTerrainScript(terrain.plugin as BaseTerrain, entity).start()
            terrain.setCircularOrbit(entity, 0f, 0f, 260f)

            return terrain
        }

        fun removeFieldFromEntity(entity: SectorEntityToken, id: String) {
            val existing = entity.memoryWithoutUpdate["${INTERDICT_FIELD_MEMID_BASE}_$id"] as? MPC_sinkholeTerrain
            entity.memoryWithoutUpdate.unset("${INTERDICT_FIELD_MEMID_BASE}_$id")

            entity.containingLocation.removeEntity(existing?.entity)
        }

        const val INTERDICT_FIELD_MEMID_BASE = "\$MPC_interdictField"
    }

    data class SinkholeParams(
        val maxRange: Float,
        val minRange: Float,
        val entity: SectorEntityToken,
        val maxBurnReductionMult: Float = 0.5f
    )

    lateinit var params: SinkholeParams
    val UID = Misc.genUID()

    val pullEffectInterval = IntervalUtil(1f, 1f) // seconds

    override fun init(terrainId: String?, entity: SectorEntityToken?, param: Any?) {
        super.init(terrainId, entity, param)

        params = param as SinkholeParams
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        pullEffectInterval.advance(amount)
        if (pullEffectInterval.intervalElapsed()) {
            tryPullEffect()
        }
    }

    private fun tryPullEffect() {
        val entity = getHostEntity()
        val loc = entity.location
        val containing = entity.containingLocation
        if (!containing.isCurrentLocation) return

        val visibility = getHostEntity().visibilityLevelToPlayerFleet
        if (visibility.ordinal >= SectorEntityToken.VisibilityLevel.SENSOR_CONTACT.ordinal) {

            val custom = CampaignPingSpec()
            custom.isUseFactionColor = true
            custom.width = 15f
            custom.range = params.maxRange
            custom.duration = 2f
            custom.alphaMult = 1f
            custom.inFraction = 0.1f
            custom.num = 1
            custom.isInvert = true
            Global.getSector().addPing(entity, custom)
        }
    }

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        if (entity == null) return
        if (entity is CampaignFleetAPI && !shouldAffectFleet(entity)) return

        for (pair in entity.abilities) {
            val id = pair.key
            if (id == Abilities.EMERGENCY_BURN) continue // you can eburn, tahts it
            val plugin = pair.value
            if (plugin.spec.hasTag("burn+")) {
                if (plugin.isActiveOrInProgress) {
                    plugin.deactivate()
                }
                plugin.forceDisable()
            }
        }

        val targetFleet = entity as? CampaignFleetAPI ?: return
        if (targetFleet.getAbility(Abilities.EMERGENCY_BURN)?.isActiveOrInProgress != true) {
            val percent = getPercentEffectiveness(targetFleet)
            if (percent <= 0f) return
            val finalBurnMult = 1 - (params.maxBurnReductionMult * percent)
            targetFleet.stats.addTemporaryModMult(
                0.1f,
                "${UID}_burn",
                nameForTooltip,
                finalBurnMult,
                targetFleet.stats.fleetwideMaxBurnMod
            )
        }
    }

    fun getPercentEffectiveness(target: SectorEntityToken): Float {
        val fleetLoc = target.location
        val ourEntity = getHostEntity()
        val entityLoc = ourEntity.location

        val dist = MathUtils.getDistance(entity, target)
        val adjustedDist = (dist - params.minRange).coerceAtLeast(0f)
        if (adjustedDist > params.maxRange) return 0f

        var mult = (1 - (1 / (params.maxRange / adjustedDist)))
        return mult
    }

    override fun containsPoint(point: Vector2f?, radius: Float): Boolean {
        val entity = getHostEntity()
        val dist = MathUtils.getDistance(entity.location, point)
        return dist <= params.maxRange
    }

    override fun containsEntity(other: SectorEntityToken?): Boolean {
        return other != getHostEntity() && super.containsEntity(other)
    }

    override fun shouldPlayLoopOne(): Boolean {
        return !params.entity.isPlayerFleet && super.shouldPlayLoopOne()
    }

    override fun getTerrainName(): String? {
        return nameForTooltip
    }

    override fun getNameForTooltip(): String? {
        return "Interdiction Field"
    }

    fun getHostFaction(): FactionAPI = getHostEntity().faction
    fun getHostEntity(): SectorEntityToken = params.entity
    fun getHostFleet(): CampaignFleetAPI? = getHostEntity() as? CampaignFleetAPI

    override fun getNameColor(): Color? {
        val fac = getHostFaction()
        val baseColor = fac.baseUIColor
        val playerFleet = Global.getSector().playerFleet
        if (shouldAffectFleet(playerFleet)) {
            val special = Misc.getNegativeHighlightColor()
            val base = baseColor
            //bad = Color.red;
            return Misc.interpolateColor(base, special, Global.getSector().campaignUI.sharedFader.brightness * 1f)
        }

        return baseColor
    }

    fun shouldAffectFleet(target: CampaignFleetAPI): Boolean {
        val host = getHostFleet()
        val fac = getHostFaction()
        return host?.isHostileTo(target) == true || target.faction.isHostileTo(fac)
    }

    /*override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        val host = getHostEntity()
        val loc = host.location

        GL11.glPushMatrix()
        GL11.glTranslatef(loc.x, loc.y, 0f)

        GL11.glEnable(GL11.GL_TEXTURE_2D)



        GL11.glPopMatrix()

        super.render(layer, viewport)
    }*/

    @Transient
    var rr: RingRenderer? = null
    override fun renderOnMap(factor: Float, alphaMult: Float) {
        val visibility = getHostEntity().visibilityLevelToPlayerFleet
        if (visibility.ordinal < SectorEntityToken.VisibilityLevel.SENSOR_CONTACT.ordinal) return

        if (rr == null) {
            rr = RingRenderer("systemMap", "map_asteroid_belt")
        }
        rr!!.render(
            entity.location,
            params.maxRange - 350f,
            params.maxRange,
            getHostFaction().baseUIColor,
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

    override fun hasAIFlag(flag: Any?): Boolean {
        return false
    }

    override fun getEffectCategory(): String? {
        return null
    }

    override fun getTerrainId(): String? {
        return "MPC_sinkhole"
    }

    override fun hasTooltip(): Boolean {
        return true
    }

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(tooltip, expanded)

        if (tooltip == null) return

        tooltip.addTitle(nameForTooltip)

        val playerFleet = Global.getSector().playerFleet
        val host = getHostEntity()

        tooltip.addPara(
            "Disables movement abilities with the exception of %s.",
            5f,
            Misc.getHighlightColor(),
            "Emergency burn"
        )

        tooltip.addPara(
            "Progressively slows hostile fleets in its vicinity based on proximity, up to a maximum of %s maximum burn level.",
            5f,
            Misc.getNegativeHighlightColor(),
            "${params.maxBurnReductionMult.roundNumTo(1).trimHangingZero()}x"
        )
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addPara(
            "%s renders fleets %s to the slowing effect",
            5f,
            Misc.getHighlightColor(),
            "Emergency burn",
            "temporarily immune"
        )
        tooltip.setBulletedListMode(null)

        val hostile = shouldAffectFleet(playerFleet)
        if (hostile) {
            tooltip.addPara(
                "Your fleet is %s to the entity hosting the field, and your engines already strain against the interdictive properties.",
                5f,
                Misc.getNegativeHighlightColor(),
                "hostile"
            )
        }
    }

    override fun getRenderRange(): Float {
        return 9999f
    }

    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?> {
        return EnumSet.of(CampaignEngineLayers.TERRAIN_2)
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return false
    }

}
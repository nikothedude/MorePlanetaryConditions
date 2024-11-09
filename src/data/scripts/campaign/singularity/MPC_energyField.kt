package data.scripts.campaign.singularity

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.RingBandAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.ShoveFleetScript
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

class MPC_energyField: BaseRingTerrain() {

    companion object {
        const val BASE_SHOVE_INTENSITY = 0.3f
    }

    override fun applyEffect(entity: SectorEntityToken?, days: Float) {
        super.applyEffect(entity, days)

        if (entity !is CampaignFleetAPI) return
        /*val newLoc = getLocOnBorderOfFields(entity)
        entity.setLocation(newLoc.x, newLoc.y)*/

        if (entity.memoryWithoutUpdate[niko_MPC_ids.RECENTLY_HIT_BY_ENERGY_FIELD] == true) return

        val dist = MathUtils.getDistance(entity, this.entity.orbitFocus)
        val base = BASE_SHOVE_INTENSITY
        var shoveMult = (maxRadiusForContains / dist).coerceAtMost(20f)
        if (shoveMult.isNaN()) shoveMult = 1f
        entity.memoryWithoutUpdate.set(niko_MPC_ids.RECENTLY_HIT_BY_ENERGY_FIELD, true, 0.01f)
        entity.addScript(ShoveFleetScript(entity, VectorUtils.getAngle(this.entity.orbitFocus.location, entity.location), base * shoveMult))
        entity.fleetData.membersListCopy.forEach { Misc.applyDamage(
            it,
            MathUtils.getRandom(),
            Misc.FleetMemberDamageLevel.LOW,
            true,
            "MPC_energyField",
            "Energy field impact",
            false,
            null,
            null
        ) }
        Global.getSector().campaignUI.addMessage(
            "Repelled by energy field",
            Misc.getNegativeHighlightColor()
        )
        for (view in entity.views) {
            view.setJitter(0.1f, (BASE_SHOVE_INTENSITY * 3.2f), getFurthestField()!!.over.color, 2, 2f)
            view.setUseCircularJitter(true)
            view.setJitterDirection(Misc.ZERO)
            view.setJitterLength((BASE_SHOVE_INTENSITY * 3.2f))
            view.setJitterBrightness(0.2f)
        }
        Global.getSoundPlayer().playSound("gate_explosion_fleet_impact", 2f, 1f, entity.getLocation(), Misc.ZERO)
    }

    private fun getLocOnBorderOfFields(entity: SectorEntityToken): Vector2f {
        val furthestField = getFurthestField() ?: return Vector2f(0f, 0f)
        val vector = MathUtils.getPointOnCircumference(Misc.ZERO, furthestField.over.middleRadius, VectorUtils.getAngle(Misc.ZERO, entity.location))
        vector.scale(1.003f)
        return vector
    }

    private fun getFields(): MutableSet<MPC_energyFieldInstance> {
        return entity.containingLocation.memoryWithoutUpdate[niko_MPC_ids.SYSTEM_ENERGY_FIELDS_LIST_MEMID] as? MutableSet<MPC_energyFieldInstance> ?: HashSet()
    }

    fun getFurthestField(): MPC_energyFieldInstance? {
        return getFields().sortedBy { it.over.middleRadius }.lastOrNull()
    }

    override fun hasTooltip(): Boolean {
        return false
    }

    override fun getTerrainName(): String {
        return "Energy Field"
    }

    override fun getNameForTooltip(): String {
        return "Energy Field"
    }

    override fun getMaxRadiusForContains(): Float {
        return ((getFurthestField()?.over?.middleRadius) ?: 0f) - 1f
    }

    override fun containsEntity(other: SectorEntityToken?): Boolean {
        return super.containsEntity(other)
    }

    override fun getRenderRange(): Float {
        return Float.MAX_VALUE
    }

    override fun getEffectCategory(): String {
        return "MPC_energyField"
    }
}
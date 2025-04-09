package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f

class MPC_magnetarThreatFleetManager: DisposableThreatFleetManager() {

    override fun getDesiredNumFleetsForSpawnLocation(): Int {
        return super.getDesiredNumFleetsForSpawnLocation() * 2
    }

    override fun pickCurrentSpawnLocation(): StarSystemAPI? {
        if (Global.getSector().isInNewGameAdvance) return null
        val player = Global.getSector().playerFleet ?: return null
        var nearest: StarSystemAPI? = null
        var minDist = Float.MAX_VALUE

        val system = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as? StarSystemAPI ?: return null

        val distToPlayerLY = Misc.getDistanceLY(player.locationInHyperspace, system.location)
        if (distToPlayerLY > MAX_RANGE_FROM_PLAYER_LY) return null

        return system
    }

    override fun spawnFleetImpl(): CampaignFleetAPI? {
        val fleet = super.spawnFleetImpl() ?: return null

        val newLoc = getAreaOutsideOfMagnetar()
        fleet.setLocation(newLoc.x, newLoc.y)

        fleet.removeScriptsOfClass(ThreatFleetBehaviorScript::class.java)
        fleet.addScript(MPC_magnetarThreatFleetBehaviorScript(fleet, currSpawnLoc))

        return fleet
    }

    private fun getAreaOutsideOfMagnetar(): Vector2f {
        val system = Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] as? StarSystemAPI ?: return Vector2f()
        val field = system.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_FIELD_MEMID] as? niko_MPC_magnetarField ?: return Vector2f()

        val angle = Misc.getUnitVectorAtDegreeAngle(MathUtils.getRandomNumberInRange(0f, 360f))
        val distance = field.getMaxEffectRadius(angle) + 4000f
        val point = angle.scale(distance) as? Vector2f ?: return Vector2f()

        return point
    }

}
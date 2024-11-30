package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignmentDeliverResourcesToCache
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.getOuterRadius

object MPC_fractalCrisisHelpers {
    fun getStationPoint(system: StarSystemAPI): Vector2f {
        val furthestMarket = system.getMarketsInLocation(Global.getSector().playerFaction.id).sortedBy { MathUtils.getDistance(it.primaryEntity, system.center) }.lastOrNull() ?: return Vector2f(0f, 0f)
        val targetDist = (MathUtils.getDistance(furthestMarket.primaryEntity, system.center) * MPC_spyAssignmentDeliverResourcesToCache.DISTANCE).coerceAtMost(system.getOuterRadius() * 2.5f)

        val targetLoc = MathUtils.getPointOnCircumference(system.center.location, targetDist, MathUtils.getRandomNumberInRange(0f, 360f))
        return targetLoc
    }

    fun getAssistanceFleets(): MutableSet<CampaignFleetAPI> {
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IAIIC_CRISIS_ASSISTANCE_FLEETS] == null) {
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IAIIC_CRISIS_ASSISTANCE_FLEETS] = HashSet<CampaignFleetAPI>()
        }
        return Global.getSector().memoryWithoutUpdate[niko_MPC_ids.IAIIC_CRISIS_ASSISTANCE_FLEETS] as HashSet<CampaignFleetAPI>
    }
}
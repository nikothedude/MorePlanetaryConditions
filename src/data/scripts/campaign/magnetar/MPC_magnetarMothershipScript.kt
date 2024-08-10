package data.scripts.campaign.magnetar

import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import java.util.*

class MPC_magnetarMothershipScript(source: SectorEntityToken, thresholdLY: Float, minFleets: Int, maxFleets: Int,
                                   respawnDelay: Float, val minPoints: Int, val maxPoints: Int
): SourceBasedFleetManager(source, thresholdLY, minFleets, maxFleets, respawnDelay) {

    private var totalLost = 0

    companion object {
        const val SMALL_FLEET_CHANCE = 0.2f
        const val LARGE_FLEET_CHANCE = 0.2f
    }

    override fun spawnFleet(): CampaignFleetAPI? {
        if (source == null) return null

        val random = Random()

        var combatPoints: Int = minPoints + random.nextInt(maxPoints - minPoints + 1)
        val randFloat = (MathUtils.getRandomNumberInRange(0f, 1f))
        if (randFloat <= SMALL_FLEET_CHANCE) {
            combatPoints = (combatPoints * 0.3).toInt()
        } /*else if (randFloat <= LARGE_FLEET_CHANCE) {
            combatPoints = (combatPoints * 2.5).toInt()
        }*/

        var bonus: Int = totalLost * 4
        if (bonus > maxPoints) bonus = maxPoints

        combatPoints += bonus

        combatPoints *= 8f.toInt()

        val fleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(combatPoints.toFloat(), source))
        MPC_magnetarFleetAssignmentAI(fleet, source.starSystem, source).start()

        val location = source.containingLocation
        location.addEntity(fleet)

        fleet.setLocation(source.location.x, source.location.y)
        fleet.facing = random.nextFloat() * 360f

        fleet.memoryWithoutUpdate["\$sourceId"] = source.id

        return fleet
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        for (fleet in fleets) { // WHY IS THIS NEEDED??? LITERALLY HOW THE FUCK IS THE AI GETTING REMOVED???
            if (!fleet.hasScriptOfClass(MPC_magnetarFleetAssignmentAI::class.java)) {
                MPC_magnetarFleetAssignmentAI(fleet, source.starSystem, source).start()
            }
        }
    }

    override fun reportFleetDespawnedToListener(fleet: CampaignFleetAPI?, reason: FleetDespawnReason, param: Any?) {
        if (reason == FleetDespawnReason.DESTROYED_BY_BATTLE || fleet?.memoryWithoutUpdate?.get(niko_MPC_ids.DRIVE_BUBBLE_DESTROYED) == true) {
            destroyed++
        }
        fleets.remove(fleet)
    }
}
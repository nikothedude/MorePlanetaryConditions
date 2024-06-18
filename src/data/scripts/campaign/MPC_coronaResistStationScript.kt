package data.scripts.campaign

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_miscUtils
import data.utilities.niko_MPC_reflectionUtils

class MPC_coronaResistStationScript(
    entity: SectorEntityToken,
    val terrain: PulsarBeamTerrainPlugin,
    val orbitRadius: Float
) : MPC_coronaResistScript(entity) {
    val posResetInterval = IntervalUtil(0.2f, 0.3f)

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        posResetInterval.advance(days)
        if (posResetInterval.intervalElapsed()) {
            resetPos()
        }

        resetFleetSmods()

        super.advance(amount)
    }

    override fun getTargetFleets(): MutableSet<CampaignFleetAPI> {
        val containingLocation = entity.containingLocation

        val fleetList = containingLocation.fleets.toHashSet()
        val defendingFleet = getCoreDefenderFleet()
        if (defendingFleet != null) {
            fleetList += defendingFleet
        }
        return fleetList
    }

    private fun getCoreDefenderFleet(): CampaignFleetAPI? {
        return entity.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET] as? CampaignFleetAPI
    }

    private fun resetFleetSmods() {
        val fleet = getCoreDefenderFleet() ?: return

        val flagship = fleet.flagship ?: return
        if (flagship.variant.permaMods.size >= 3) return

        niko_MPC_miscUtils.refreshCoronaDefenderFleetVariables(fleet)
    }

    private fun resetPos() {
        val orbit = entity.orbit
        val angle = niko_MPC_reflectionUtils.get("pulsarAngle", terrain) as? Float ?: return

        entity.setCircularOrbitPointingDown(orbit.focus, angle, orbitRadius, orbit.orbitalPeriod)
    }

    override fun shouldAffectFleet(fleet: CampaignFleetAPI): Boolean {
        if (!super.shouldAffectFleet(fleet)) return false

        return fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_DEFENDER] == true
    }

}
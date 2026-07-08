package data.scripts.campaign.magnetar.crisis.industries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICPatrolAssignmentAI
import data.utilities.niko_MPC_ids

class MPC_IAIICHighCommand: MilitaryBase() {
    override fun spawnFleet(route: RouteManager.RouteData?): CampaignFleetAPI? {
        if (market.faction.id != niko_MPC_ids.IAIIC_FAC_ID) return super.spawnFleet(route)

        val custom = route!!.custom as PatrolFleetData
        val type = custom.type

        val random = route!!.random

        val fleet = createPatrol(type, getFleetFactionSource().id, route, market, null, random)
        fleet.setFaction(market.faction.id, true)

        if (fleet == null || fleet.isEmpty) return null

        fleet.addEventListener(this)

        market.containingLocation.addEntity(fleet)
        fleet.facing = Math.random().toFloat() * 360f
        // this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
        fleet.setLocation(market.primaryEntity.location.x, market.primaryEntity.location.y)

        fleet.addScript(MPC_IAIICPatrolAssignmentAI(fleet, route)) // EDIT

        fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS, true] = 0.3f

        //market.getContainingLocation().addEntity(fleet);
        //fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);


        //market.getContainingLocation().addEntity(fleet);
        //fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
        if (custom.spawnFP <= 0) {
            custom.spawnFP = fleet.fleetPoints
        }

        return fleet
    }

    private fun getFleetFactionSource(): FactionAPI {
        val intel = MPC_IAIICFobIntel.get() ?: return market.faction
        val rand = intel.getRandContribForFleet() ?: return market.faction
        return Global.getSector().getFaction(rand.factionId)
    }
}
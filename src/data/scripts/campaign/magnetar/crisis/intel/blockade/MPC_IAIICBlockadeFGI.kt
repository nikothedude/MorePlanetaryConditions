package data.scripts.campaign.magnetar.crisis.intel.blockade

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.impl.campaign.NPCHassler
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.impl.campaign.intel.group.BlockadeFGI
import com.fs.starfarer.api.impl.campaign.intel.group.FGBlockadeAction
import com.fs.starfarer.api.impl.campaign.intel.group.PerseanLeagueBlockade
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.*
import data.utilities.niko_MPC_fleetTypes.BLOCKADE_COMMAND_FLEET

class MPC_IAIICBlockadeFGI(params: GenericRaidParams?, blockadeParams: FGBlockadeAction.FGBlockadeParams?): BlockadeFGI(params, blockadeParams) {

    companion object {
        const val IAIIC_COMMAND_FLEET = "\$MPC_IAIIC_blockadeCommand"
        const val SUPPLY = "\$MPC_IAIIC_blockadeSupplyFleet"
        const val GENERIC = "\$MPC_IAIIC_blockadeGenericFleet"
    }

    var commandFleet: CampaignFleetAPI? = null
    var supplyFleets: MutableSet<CampaignFleetAPI> = HashSet()

    override fun createFleet(size: Int, damage: Float): CampaignFleetAPI {
        val r = getRandom()

        val loc = origin.locationInHyperspace

        val m = FleetCreatorMission(r)
        m.beginFleet()

        val armada = size == 10 && commandFleet == null
        val supplyFleet = size == 3 && supplyFleets.size < 3

        if (armada) {
            m.triggerCreateFleet(
                FleetSize.MAXIMUM,
                FleetQuality.SMOD_3,
                params.factionId,
                BLOCKADE_COMMAND_FLEET,
                loc
            )
            m.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER)
            m.triggerSetFleetFlag(IAIIC_COMMAND_FLEET)
            m.triggerSetFleetType(BLOCKADE_COMMAND_FLEET)
            m.triggerSetFleetDoctrineQuality(5, 5, 5)
            m.triggerSetFleetDoctrineOther(5, 0)
            m.triggerSetFleetComposition(0f, 0f, 0f, 0f, 0f)
            m.triggerFleetMakeFaster(true, 1, false)
            m.triggerFleetAddCommanderSkill(Skills.CREW_TRAINING, 1)
            m.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
            m.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1)
            m.triggerFleetAddCommanderSkill(Skills.CARRIER_GROUP, 1)
        } else if (supplyFleet) {
            var total = 0
            for (i in params.fleetSizes) total += i
            var supplyFleetSize = FleetSize.MEDIUM
            if (total < 50) {
                supplyFleetSize = FleetSize.SMALL
            } else if (total >= 80) {
                supplyFleetSize = FleetSize.LARGE
            }
            m.triggerCreateFleet(supplyFleetSize, FleetQuality.DEFAULT, params.factionId, FleetTypes.SUPPLY_FLEET, loc)
            m.triggerSetFleetOfficers(OfficerNum.DEFAULT, OfficerQuality.DEFAULT)
            m.triggerSetFleetFlag(SUPPLY)
            m.triggerSetFleetType(FleetTypes.SUPPLY_FLEET)
            m.triggerFleetMakeFaster(true, 0, false)
            m.triggerSetFleetComposition(0.5f, 0.5f, 0.1f, 0f, 0.1f)
        } else {
            m.createFleet(params.style, size, params.factionId, loc)
            m.triggerSetFleetFlag(GENERIC)
        }

        m.setFleetSource(params.source)
        m.setFleetDamageTaken(damage)

        m.triggerSetPatrol()
        m.triggerMakeLowRepImpact()
        m.triggerMakeAlwaysSpreadTOffHostility()


        val fleet = m.createFleet()

        if (fleet != null && !armada && !supplyFleet) {
            fleet.addScript(NPCHassler(fleet, targetSystem))
        }

        if (fleet != null && armada) {
            fleet.commander.rankId = Ranks.SPACE_ADMIRAL
            setNeverStraggler(fleet)
            commandFleet = fleet
        }
        if (supplyFleet) {
            supplyFleets += fleet
        }

        return fleet!!
    }

}
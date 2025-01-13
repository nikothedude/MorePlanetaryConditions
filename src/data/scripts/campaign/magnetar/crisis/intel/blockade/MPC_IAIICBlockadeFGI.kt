package data.scripts.campaign.magnetar.crisis.intel.blockade

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.NPCHassler
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.intel.group.BlockadeFGI
import com.fs.starfarer.api.impl.campaign.intel.group.FGBlockadeAction
import com.fs.starfarer.api.impl.campaign.intel.group.PerseanLeagueBlockade
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.*
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_fleetTypes.BLOCKADE_COMMAND_FLEET
import org.magiclib.kotlin.makeImportant

class MPC_IAIICBlockadeFGI(params: GenericRaidParams?, blockadeParams: FGBlockadeAction.FGBlockadeParams?): BlockadeFGI(params, blockadeParams) {

    companion object {
        const val IAIIC_COMMAND_FLEET = "\$MPC_IAIIC_blockadeCommand"
        const val SUPPLY = "\$MPC_IAIIC_blockadeSupplyFleet"
        const val GENERIC = "\$MPC_IAIIC_blockadeGenericFleet"
        const val BLOCKADING = "\$MPC_IAIICblockading"
        const val NUM_OTHER_FLEETS_MULT = 0.25f

        const val KEY = "\$MPC_IAIICBlockade"
        const val HASSLE_REASON = "MPC_IAIICBlockader"

        fun get(): MPC_IAIICBlockadeFGI? = Global.getSector().memoryWithoutUpdate[KEY] as? MPC_IAIICBlockadeFGI
    }

    var commandFleet: CampaignFleetAPI? = null
    var supplyFleets: MutableSet<CampaignFleetAPI> = HashSet()

    init {
        Global.getSector().memoryWithoutUpdate[KEY] = this
    }

    override fun notifyEnding() {
        super.notifyEnding()
        Global.getSector().memoryWithoutUpdate.unset(KEY)
        /*val reynard = People.getPerson(People.REYNARD_HANNAN)
        if (reynard != null) {
            Misc.makeUnimportant(reynard, "PLB")
        }*/
    }


    override fun createFleet(size: Int, damage: Float): CampaignFleetAPI {
        val r = getRandom()

        val loc = origin.locationInHyperspace

        val m = FleetCreatorMission(r)
        m.beginFleet()

        val armada = size == 200 && commandFleet == null
        val supplyFleet = size == 5 && supplyFleets.size < 3

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
        m.triggerSetFleetFlag("\$MPC_IAIICblockaderFleetFlag")

        m.setFleetSource(params.source)
        m.setFleetDamageTaken(damage)

        m.triggerSetPatrol()
        //m.triggerMakeLowRepImpact()
        m.triggerMakeAlwaysSpreadTOffHostility()


        val fleet = m.createFleet()

        if (fleet != null && !armada && !supplyFleet) {
            fleet.addScript(NPCHassler(fleet, targetSystem))
        }

        if (fleet != null && armada) {
            fleet.commander.rankId = Ranks.SPACE_ADMIRAL
            setNeverStraggler(fleet)
            commandFleet = fleet
            fleet.makeImportant("\$MPC_IAIICKeyBlockadeFleet")
        }
        if (supplyFleet) {
            supplyFleets += fleet
            fleet.makeImportant("\$MPC_IAIICKeyBlockadeFleet")
        }

        return fleet!!
    }

    //	@Override
    //	public void abort() {
    //		if (!isAborted()) {
    //			for (CampaignFleetAPI curr : getFleets()) {
    //				curr.getMemoryWithoutUpdate().set(ABORTED_OR_ENDING, true);
    //			}
    //		}
    //		super.abort();
    //	}
    override fun advance(amount: Float) {
        super.advance(amount)
        if (isSpawnedFleets) {
            if (isEnded || isEnding || isAborted || isCurrent(RETURN_ACTION)) {
                for (curr: CampaignFleetAPI in getFleets()) {
                    //curr.getMemoryWithoutUpdate().set(ABORTED_OR_ENDING, true);
                    curr.memoryWithoutUpdate[BLOCKADING] = false
                }
                return
            }
            if (isCurrent(PAYLOAD_ACTION)) {
                for (curr: CampaignFleetAPI in getFleets()) {
                    curr.memoryWithoutUpdate[BLOCKADING] = true
                    //					curr.getMemoryWithoutUpdate().set(ARMADA, true);
// 					curr.getMemoryWithoutUpdate().set(SUPPLY, true);
                }
            }
        }
    }

    override fun periodicUpdate() {
        super.periodicUpdate()
        if (HostileActivityEventIntel.get() == null) { // ???
            abort()
            return
        }
        val action = currentAction
        if (action is FGBlockadeAction) {
            val stat = HostileActivityEventIntel.get().getNumFleetsStat(targetSystem)
            stat.addTemporaryModMult(1f, "MPC_IAIICBlockade", null, NUM_OTHER_FLEETS_MULT)
        }
        if (!isSpawnedFleets || isSpawning) return
        var armada = 0
        var supply = 0
        for (curr: CampaignFleetAPI in getFleets()) {
            if (curr.memoryWithoutUpdate.getBoolean(IAIIC_COMMAND_FLEET)) {
                armada++
            }
            if (curr.memoryWithoutUpdate.getBoolean(SUPPLY)) {
                supply++
            }
        }
        if (armada <= 0) {
            disruptCommand()
            abort()
            return
        }
        if (supply <= 0) {
            abort()
            return
        }
        if (action is FGBlockadeAction) {
            val blockade = action
            if (blockade.primary != null) {
                var supplyIndex = 0
                for (curr: CampaignFleetAPI in getFleets()) {
                    if (blockade.primary.containingLocation !== curr.containingLocation) {
                        continue
                    }
                    if (curr.memoryWithoutUpdate.getBoolean(SUPPLY)) {
                        Misc.setFlagWithReason(curr.memoryWithoutUpdate, MemFlags.FLEET_BUSY, curr.id, true, -1f)
                        curr.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED, true] = 0.4f
                        curr.clearAssignments()
                        var resupplyLoc = blockade.primary
                        if (supplyIndex == 1) {
                            for (jp: SectorEntityToken in blockade.blockadePoints) {
                                if (jp !== resupplyLoc) {
                                    resupplyLoc = jp
                                    break
                                }
                            }
                        }
                        curr.addAssignment(
                            FleetAssignment.ORBIT_PASSIVE, resupplyLoc, 3f,
                            "standing by to provide resupply"
                        )
                        supplyIndex++
                    } else if (curr.memoryWithoutUpdate.getBoolean(IAIIC_COMMAND_FLEET)) {
                    } else {
                        curr.memoryWithoutUpdate[MemFlags.WILL_HASSLE_PLAYER, true] = 2f
                        curr.memoryWithoutUpdate[MemFlags.HASSLE_TYPE, HASSLE_REASON] =
                            2f
                    }
                }
            }
        }
    }

    private fun disruptCommand() {
        MPC_IAIICFobIntel.get()?.disruptCommand()
        MPC_IAIICFobIntel.get()?.sendUpdateIfPlayerHasIntel(
            "Major IAIIC defeat! Command disruption",
            false,
            false
        )
    }


    override fun addPostAssessmentSection(info: TooltipMakerAPI, width: Float, height: Float, opad: Float) {
        info.addPara(
            "The blockading forces are led by a Command Fleet and "
                    + "supported by a trio of supply fleets.", opad
        )
		bullet(info);
		info.addPara("Defeating the Command Fleet will disrupt the IAIIC's chain of command, heavily impairing their military operations (as well as ending the blockade)", opad);
		info.addPara("Forcing the supply fleets to withdraw will defeat the blockade", 0f);
		unindent(info);
    }

}
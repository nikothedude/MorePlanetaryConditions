package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel
import com.fs.starfarer.api.campaign.ai.ModularFleetAIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignment
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_marketUtils.isFractalMarket
import lunalib.lunaExtensions.getMarketsCopy
import org.magiclib.kotlin.getSourceMarket

class MPC_spyFleetScript(val fleet: CampaignFleetAPI, val system: StarSystemAPI, val assignment: MPC_spyAssignment) : niko_MPC_baseNikoScript() {
    val abandonInterval = IntervalUtil(110f, 120f)
    //val timeoutKey = "\$NPCHassleTimeout"

    enum class TargetType(val weight: Float) {
        FLEET(20f),
        MARKET(50f),
        OBJECTIVE(20f)
    }

    override fun startImpl() {
        assignment.init(this)
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        abandonInterval.advance(days)
        if (abandonInterval.intervalElapsed()) {
            abortAndReturnToBase()
            return
        }
        assignment.advance(amount, this)
    }

    private fun abortAndReturnToBase() {
        fleet.clearAssignments()
        fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, fleet.getSourceMarket().primaryEntity, 999999f)
    }

    /*private fun refreshAssignments() {
        //if (restrictTo != null && fleet.containingLocation !== restrictTo) return
        if (Global.getSector().memoryWithoutUpdate.contains(timeoutKey)) return
        if (fleet.memoryWithoutUpdate.getBoolean(MemFlags.FLEET_SPECIAL_ACTION)) return

        if (fleet.battle != null) return
        if (fleet.ai != null && (fleet.ai.isFleeing || fleet.ai.isMaintainingContact)) {
            return
        }
        if (fleet.currentAssignment != null && fleet.currentAssignment.assignment == FleetAssignment.ORBIT_PASSIVE) {
            return
        }

        val fleetTargets = Misc.findNearbyFleets(fleet, 1000f) { other -> isTargetAllowed(other) }
        val marketTargets = findValidMarkets()
        val objectiveTargets = findValidObjectives()
        val picker = WeightedRandomPicker<TargetType>()
        if (fleetTargets.isNotEmpty()) picker.add(TargetType.FLEET, TargetType.FLEET.weight)
        if (marketTargets.isNotEmpty()) picker.add(TargetType.MARKET, TargetType.MARKET.weight)
        if (objectiveTargets.isNotEmpty()) picker.add(TargetType.OBJECTIVE, TargetType.OBJECTIVE.weight)

        val type = picker.pick()
        when (type) {

        }
    }

    private fun findValidObjectives(): List<SectorEntityToken> {
        return system.getEntitiesWithTag(Tags.OBJECTIVE).filter { it.hasTag(Tags.COMM_RELAY) || it.hasTag(Tags.SENSOR_ARRAY) }
    }

    private fun findValidMarkets(): MutableMap<MarketAPI, Float> {
        val marketsToWeight = HashMap<MarketAPI, Float>()

        val playerFaction = Global.getSector().playerFaction
        for (market in playerFaction.getMarketsCopy()) {
            if (market.containingLocation != system) continue
            if (market.isFractalMarket())  {
                marketsToWeight[market] = 10f
            } else {
                marketsToWeight[market] = 3f
            }
        }

        return marketsToWeight
    }

    fun isTargetAllowed(target: CampaignFleetAPI): Boolean {
        if (target.isPlayerFleet || target.ai == null) return false
        if (target.isHostileTo(fleet)) return false
        if (target.isStationMode) return false
        if (target.battle != null) return false

        if (target.faction == fleet.faction) return false
        if (target.memoryWithoutUpdate.contains(timeoutKey)) return false

        if (target.ai is ModularFleetAIAPI) {
            val ai = target.ai
            if (ai.isFleeing || ai.isMaintainingContact) return false
            if (fleet.interactionTarget is CampaignFleetAPI) return false
        }

        if (!isTargetRightTypeOfFleet(target)) return false

        val vis: VisibilityLevel = target.getVisibilityLevelTo(fleet)
        return vis != VisibilityLevel.NONE
    }

    private fun isTargetRightTypeOfFleet(target: CampaignFleetAPI): Boolean {
        if (Misc.isTrader(target)) return true
        if (Misc.isPirate(target)) return false
        if (Misc.isPatrol(target) && !target.faction.isPlayerFaction) return false
        if (Misc.isWarFleet(target)) return false
        return target.faction.getCustomBoolean(Factions.CUSTOM_DECENTRALIZED)
    }*/

}

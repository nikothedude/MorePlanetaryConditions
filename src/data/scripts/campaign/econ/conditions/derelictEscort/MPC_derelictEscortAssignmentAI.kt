package data.scripts.campaign.econ.conditions.derelictEscort

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Abilities
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getEscortFleetList
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_settings
import org.lazywizard.lazylib.MathUtils

// lots of this was inspired by tiandong patrol ai
class MPC_derelictEscortAssignmentAI(
    val fleet: CampaignFleetAPI,
    var target: CampaignFleetAPI,
    var homeMarket: MarketAPI,
    var despawnSetting: Boolean? = target.isNoAutoDespawn,
): niko_MPC_baseNikoScript() {

    companion object {
        const val MIN_DAYS_ESCORTING_TIL_END = 90f
        const val MAX_DAYS_ESCORTING_TIL_END = 95f

        const val DERELICT_ESCORT_CATCH_UP_DIST = 400f
        const val DERELICT_ESCORT_JOIN_COMBAT_DIST = 400f

        const val MAX_FOLLOW_PLAYER_DIST_LY = 5.0

        fun get(fleet: CampaignFleetAPI): MPC_derelictEscortAssignmentAI? = fleet.scripts.firstOrNull { it is MPC_derelictEscortAssignmentAI } as? MPC_derelictEscortAssignmentAI
    }

    val interval = IntervalUtil(MIN_DAYS_ESCORTING_TIL_END, MAX_DAYS_ESCORTING_TIL_END)
    var shouldDoScaryMessages = !homeMarket.isInhabited()

    init {
        giveInitialAssignments()
    }

    protected fun giveInitialAssignments() {
        refreshAssignments()
    }

    override fun startImpl() {
        fleet.addScript(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            abortAndReturnToBase()
            return
        }

        refreshAssignments()

        val newMarket = homeMarket.primaryEntity?.market
        if (newMarket != null) homeMarket = newMarket
        if (shouldDoScaryMessages && homeMarket.isInhabited()) shouldDoScaryMessages = false
    }

    fun refreshAssignments() {

        if (homeMarket.primaryEntity == null || !homeMarket.primaryEntity.isAlive) {
            selfDestruct()
            return
        }

        if (!target.isAlive || fleet.isHostileTo(target)) {
            abortAndReturnToBase()
            return
        }
        if ((fleet.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_STATE_MEMFLAG]) == derelictEscortStates.RETURNING_TO_BASE) {
            val curr = fleet.ai?.currentAssignment ?: return
            if (curr.assignment != FleetAssignment.GO_TO_LOCATION_AND_DESPAWN) {
                fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, homeMarket.primaryEntity, 1000f, null)
            }
            return
        }

        val targetBattle = target.battle
        if (targetBattle != null && !targetBattle.isDone) {
            moveToJoinBattle()
            return
        }

        if (target.isPlayerFleet && target.containingLocation != null && !homeMarket.isInhabited()) {
            val inSameSystem = target.containingLocation == homeMarket.containingLocation
            if (!inSameSystem) {
                val distFromHome = Misc.getDistanceToPlayerLY(homeMarket.locationInHyperspace)
                if (distFromHome > MAX_FOLLOW_PLAYER_DIST_LY) {
                    abortAndReturnToBase()
                    return
                }
            }
        }
        val dist = MathUtils.getDistance(fleet, target)

        if (target.currentAssignment?.target == fleet || target.interactionTarget == fleet) {
            derelictEscortStates.MEETING_ESCORT.overrideAssignment(
                fleet,
                this,
                target
            )
            return
        }

        if (dist > DERELICT_ESCORT_CATCH_UP_DIST) {
            derelictEscortStates.CATCHING_UP_TO.overrideAssignment(fleet, this, target)
        } else {
            derelictEscortStates.ESCORTING.overrideAssignment(fleet, this, target)
        }
    }

    private fun moveToJoinBattle(targetFleet: CampaignFleetAPI = target) {
        val canJoinBattle = target.battle.canJoin(fleet)

        if (!canJoinBattle) {
            derelictEscortStates.WAITING_FOR_BATTLE_TO_END.overrideAssignment(fleet, this, targetFleet)
            return
        }

        val dist = MathUtils.getDistance(fleet, targetFleet)
        if (dist <= DERELICT_ESCORT_JOIN_COMBAT_DIST) {
            derelictEscortStates.JOINING_BATTLE.overrideAssignment(fleet, this, targetFleet)
        }
    }

    private fun selfDestruct() {
        for (member in fleet.fleetData.membersListCopy) {
            fleet.removeFleetMemberWithDestructionFlash(member)
        }
        delete()
    }

    fun abortAndReturnToBase() {
        derelictEscortStates.RETURNING_TO_BASE.overrideAssignment(fleet, this, homeMarket.primaryEntity)
        fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS] = true
        homeMarket.getEscortFleetList() -= target

        if (!target.isPlayerFleet) target.isNoAutoDespawn = despawnSetting
        fleet.isNoAutoDespawn = false
    }

    override fun delete(): Boolean {
        if (!super.delete())
            return false

        fleet.clearAssignments()
        fleet.memoryWithoutUpdate[MemFlags.FLEET_IGNORES_OTHER_FLEETS] = true

        homeMarket.getEscortFleetList() -= target
        target.memoryWithoutUpdate[niko_MPC_ids.DERELICT_ESCORT_FLEET_TARGET_MEMID] = null

        if (!target.isPlayerFleet) target.isNoAutoDespawn = despawnSetting
        fleet.isNoAutoDespawn = false
        return true
    }
}
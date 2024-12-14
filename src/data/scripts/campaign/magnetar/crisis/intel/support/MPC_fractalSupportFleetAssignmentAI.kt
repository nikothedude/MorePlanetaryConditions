package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.spawnFleet.overgrownNanoforgeFleetAssignmentAI
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.getSourceMarket
import java.util.*
import kotlin.math.max

class MPC_fractalSupportFleetAssignmentAI(
    val fleet: CampaignFleetAPI,
    val intel: MPC_fractalCrisisSupport
): niko_MPC_baseNikoScript(), CampaignEventListener {

    companion object {
        fun get(fleet: CampaignFleetAPI): MPC_fractalSupportFleetAssignmentAI? {
            return fleet.memoryWithoutUpdate["\$MPC_fractalSupportScript"] as? MPC_fractalSupportFleetAssignmentAI
        }

        const val objectiveWeight = 1f
        const val fractalColonyWeight = 1.25f
        const val planetWeight = 0.5f
        const val jumpPointWeight = 1f

        const val MAX_ASSIGNMENTS = 6
        const val DAMAGE_THRESH_FOR_STANDDOWN = 0.34f
    }

    lateinit var state: MPC_fractalSupportState
    var returnReason: ReturnReason = ReturnReason.OFF_DUTY
    var assignmentsDone = 0f
    var assignmentsGiven = false
    val initialFP = fleet.fleetPoints
    val interval = IntervalUtil(1f, 1.1f)

    enum class ReturnReason {
        OFF_DUTY,
        HOSTILE,
        EVENT_OVER,
        DAMAGED;
    }

    override fun startImpl() {
        fleet.addScript(this)
        fleet.memoryWithoutUpdate["\$MPC_fractalSupportScript"] = this
        if (!assignmentsGiven) {
            addInitialAssignments()
        }
    }

    override fun stopImpl() {
        fleet.removeScript(this)
        fleet.memoryWithoutUpdate["\$MPC_fractalSupportScript"] = null
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (state == MPC_fractalSupportState.RETURNING) return
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            val FP = fleet.fleetPoints
            val remainder = FP / initialFP
            if (remainder <= DAMAGE_THRESH_FOR_STANDDOWN) {
                returnFromPatrol(ReturnReason.DAMAGED)
            }
        }
    }

    private fun addInitialAssignments() {
        val system = getSystem() ?: return
        val target = system.jumpPoints.randomOrNull() ?: getColony()?.primaryEntity
        if (fleet.containingLocation != system) { // we have to travel
            fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, target, Float.MAX_VALUE, "Travelling to ${system.name}") { }
        } else {
            startPatrol()
        }
    }

    private fun startPatrol() {
        state = MPC_fractalSupportState.PATROLLING

        refreshAssignments()
    }

    private fun refreshAssignments() {
        if (assignmentsDone >= MAX_ASSIGNMENTS) {
            returnFromPatrol(ReturnReason.OFF_DUTY)
            return
        }
        assignmentsDone++
        val target: SectorEntityToken? = pickEntityToGuard()
        fleet.clearAssignments()
        if (target == null) {
            val dur = MathUtils.getRandomNumberInRange(20f, 30f)
            fleet.addAssignmentAtStart(FleetAssignment.PATROL_SYSTEM, null, dur, "patrolling") { refreshAssignments() }
        }
        val dur = MathUtils.getRandomNumberInRange(20f, 30f)
        fleet.addAssignmentAtStart(FleetAssignment.DEFEND_LOCATION, target, dur) { refreshAssignments() }
    }

    fun returnFromPatrol(reason: ReturnReason) {
        state = MPC_fractalSupportState.RETURNING
        returnReason = reason

        fleet.clearAssignments()
        val source = fleet.getSourceMarket() ?: getColony() ?: Global.getSector().economy.marketsCopy.random()
        fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, source.primaryEntity, Float.MAX_VALUE, "returning to ${source.name}") {
            fleet.clearAssignments()
            fleet.addAssignmentAtStart(
                FleetAssignment.ORBIT_PASSIVE, source.primaryEntity, MathUtils.getRandomNumberInRange(3f, 4f), "standing down from war duty") {
                    fleet.clearAssignments()
                    fleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, source.primaryEntity, Float.MAX_VALUE, "standing down from war duty", null)
            }
        }
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
    }

    private fun pickEntityToGuard(): SectorEntityToken? {
        val picker = WeightedRandomPicker<SectorEntityToken?>()
        val system = getSystem() ?: return null
        val fractalColony = getColony() ?: return null

        picker.add(null, 1.25f) // just patrol

        val markets = Misc.getMarketsInLocation(fleet.containingLocation)

        for (market in markets) {
            if (market.faction.isHostileTo(fleet.faction)) continue
            if (market.faction.isHostileTo(Global.getSector().playerFaction)) continue
            if (market.faction.id == niko_MPC_ids.IAIIC_FAC_ID) continue

            val value = if (market == fractalColony) fractalColonyWeight else planetWeight
            picker.add(market.primaryEntity, value)
        }

        var hostileMax = 0
        var friendlyMax = 0
        for (market in markets) {
            if (market.faction.isHostileTo(fleet.faction)) {
                hostileMax = hostileMax.coerceAtLeast(market.size)
            } else {
                friendlyMax = max(friendlyMax, market.size)
            }
        }
        val inControl = friendlyMax > hostileMax

        if (inControl) {
            for (entity in system.jumpPoints) {
                var weight = jumpPointWeight
                if (fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] == FleetFactory.PatrolType.HEAVY) {
                    weight *= 0.1f
                }
                picker.add(entity, weight)
            }
        }

        for (entity in system.getEntitiesWithTag(Tags.OBJECTIVE)) {
            if (entity.faction != Global.getSector().playerFaction) continue
            var weight = objectiveWeight
            if (fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] == FleetFactory.PatrolType.HEAVY) {
                weight *= 0.1f
            }

            picker.add(entity, weight)
        }
        return picker.pick()
    }

    fun getColony(): MarketAPI? = MPC_hegemonyFractalCoreCause.getFractalColony()
    fun getSystem(): StarSystemAPI? = getColony()?.starSystem

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerClosedMarket(market: MarketAPI?) {
        return
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        return
    }

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        return
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        return
    }

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        return
    }

    override fun reportFleetDespawned(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        return
    }

    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        return
    }

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        if (fleet != this.fleet) return

        if (to == getSystem() && state == MPC_fractalSupportState.TRAVELLING_FROM_BASE) {
            reachedSystem()
        }
    }

    private fun reachedSystem() {
        startPatrol()
    }

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI?) {
        return
    }

    override fun reportPlayerReputationChange(faction: String?, delta: Float) {
        return
    }

    override fun reportPlayerReputationChange(person: PersonAPI?, delta: Float) {
        return
    }

    override fun reportPlayerActivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDeactivatedAbility(ability: AbilityPlugin?, param: Any?) {
        return
    }

    override fun reportPlayerDumpedCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportPlayerDidNotTakeCargo(cargo: CargoAPI?) {
        return
    }

    override fun reportEconomyTick(iterIndex: Int) {
        return
    }

    override fun reportEconomyMonthEnd() {
        return
    }

}
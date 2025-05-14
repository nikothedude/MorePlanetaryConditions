package data.scripts.campaign.magnetar.crisis.intel.support

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause.Companion.getFractalColony
import data.utilities.niko_MPC_ids
import java.awt.Color

abstract class MPC_fractalCrisisSupport: BaseIntelPlugin(), FleetEventListener, CampaignEventListener {

    init {
        Global.getSector().addScript(this)
        Global.getSector().addListener(this)
        Global.getSector().listenerManager.addListener(this)
    }

    enum class State {
        ACTIVE {
            override fun apply(intel: MPC_fractalCrisisSupport) {
                return
            }

            override fun unapply(intel: MPC_fractalCrisisSupport) {
                return
            }
        },
        SUSPENDED_DUE_TO_HOSTILITIES {
            override fun apply(intel: MPC_fractalCrisisSupport) {
                intel.recallFleets(MPC_fractalSupportFleetAssignmentAI.ReturnReason.HOSTILE)
            }

            override fun unapply(intel: MPC_fractalCrisisSupport) {
                return
            }
        };

        abstract fun apply(intel: MPC_fractalCrisisSupport)
        abstract fun unapply(intel: MPC_fractalCrisisSupport)
    }

    var state = State.ACTIVE
    val fleets: MutableSet<CampaignFleetAPI> = HashSet()
    var returningPatrolValue: Float = 0f
    abstract val tracker: IntervalUtil

    fun setStateExternal(newState: State) {
        state.unapply(this)
        newState.apply(this)
    }

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        val days = Misc.getDays(amount)
        if (state == State.ACTIVE) {
            var extraTime = 0f
            if (returningPatrolValue > 0) {
                // apply "returned patrols" to spawn rate, at a maximum rate of 1 interval per day
                val interval: Float = tracker.intervalDuration
                extraTime = interval * days
                returningPatrolValue -= days
                if (returningPatrolValue < 0) returningPatrolValue = 0f
            }
            tracker.advance(days + extraTime)
            if (tracker.intervalElapsed()) {
                tryCreatingFleet()
            }
        }
    }

    fun tryCreatingFleet(): CampaignFleetAPI? {
        val fleet = createFleet() ?: return null

        fleet.memoryWithoutUpdate[niko_MPC_ids.FRACTAL_CRISIS_ASSISTANCE_FLEET] = true
        fleet.memoryWithoutUpdate["\$MPC_supportFleetStartingFP"] = fleet.fleetPoints
        fleet.addEventListener(this)
        addAssignmentAI(fleet)
        //fleet.facing = Math.random().toFloat() * 360f
        fleets += fleet
        return fleet
    }
    abstract fun createFleet(): CampaignFleetAPI?

    open fun addAssignmentAI(fleet: CampaignFleetAPI) {
        MPC_fractalSupportFleetAssignmentAI(fleet, this).start()
    }

    fun getCount(vararg types: FleetFactory.PatrolType): Int {
        var count = 0
        for (fleet in fleets) {
            val foundType = fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] ?: continue
            for (type in types) {
                if (type == foundType) {
                    count++
                    break
                }
            }
        }
        return count
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return

        //val c = getTitleColor(mode)
        info.setParaFontDefault()

        //info.addPara(name, factionForUIColors.baseUIColor, 0f)
        info.setParaFontDefault()
        addDesc(info)

        addFleetSection(info)

        //addBulletPoints(info, mode)
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI?,
        mode: IntelInfoPlugin.ListInfoMode?,
        isUpdate: Boolean,
        tc: Color?,
        initPad: Float
    ) {
        if (info == null) return

        super.addBulletPoints(info, mode, isUpdate, tc, initPad)
        if (isUpdate) {
            addUpdateBullets(info)
        }
    }

    open fun addUpdateBullets(info: TooltipMakerAPI) {
        if (listInfoParam is State) {
            when (listInfoParam) {
                State.ACTIVE -> {
                    info.addPara(
                        "Assistance resumed due to newly cooperative stance",
                        5f
                    )
                }
                State.SUSPENDED_DUE_TO_HOSTILITIES -> {
                    info.addPara(
                        "Assistance suspended due to hostilities",
                        5f
                    )
                }
            }
        }
    }

    override fun getName(): String? {
        return "Transient Alliance - "
    }

    abstract fun addDesc(info: TooltipMakerAPI)
    fun addFleetSection(info: TooltipMakerAPI) {
        info.addSectionHeading("Fleets", Alignment.MID, 10f)
        info.addSpacer(5f)

        if (state == State.SUSPENDED_DUE_TO_HOSTILITIES) {
            info.addPara(
                "Unfortunately, your stance with the assisting polity has degraded to the point that %s until %s.",
                5f,
                Misc.getHighlightColor(),
                "no fleets will be provided", "relations are improved"
            )
        }

        val colony = getFractalColony() ?: return
        val travellingFleets = getTravellingFleets()
        val returningFleets = getReturningFleets()

        info.addPara(
            "%s total fleets",
            5f,
            Misc.getHighlightColor(),
            "${fleets.size}"
        )
        info.addPara(
            "%s actively patrolling %s",
            5f,
            Misc.getHighlightColor(),
            "${getPatrollingFleets().size}", colony.starSystem.name
        )
        info.addPara(
            "$INDENT...%s of which are currently engaged in battle",
            0f,
            Misc.getHighlightColor(),
            "${getPatrollingFleets().filter { it.battle != null }.size}"
        )
        if (travellingFleets.isNotEmpty()) {
            info.addPara(
                "%s travelling to %s",
                5f,
                Misc.getHighlightColor(),
                "${travellingFleets.size}", colony.starSystem.name
            )
        }
        if (returningFleets.isNotEmpty()) {
            info.addPara(
                "%s returning to base",
                5f,
                Misc.getHighlightColor(),
                "${returningFleets.size}"
            )
        }
    }

    private fun getAllFleets(): Set<CampaignFleetAPI> {
        return fleets
    }

    private fun getReturningFleets(): List<CampaignFleetAPI> {
        return fleets.filter { MPC_fractalSupportFleetAssignmentAI.get(it)?.state == MPC_fractalSupportState.RETURNING }
    }

    private fun getTravellingFleets(): List<CampaignFleetAPI> {
        return fleets.filter { MPC_fractalSupportFleetAssignmentAI.get(it)?.state == MPC_fractalSupportState.TRAVELLING_FROM_BASE }
    }

    private fun getPatrollingFleets(): List<CampaignFleetAPI> {
        return fleets.filter { MPC_fractalSupportFleetAssignmentAI.get(it)?.state == MPC_fractalSupportState.PATROLLING }
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: FleetDespawnReason?,
        param: Any?
    ) {
        if (fleet == null || reason == null) return
        // this is a manual listener, so we can be assured that whatever is in here was intentionally listened to
        fleets -= fleet

        if (reason == FleetDespawnReason.REACHED_DESTINATION) {
            fleetOffDuty(fleet)
        }
    }

    fun recallFleets(reason: MPC_fractalSupportFleetAssignmentAI.ReturnReason) {
        for (fleet in fleets) {
            val script = MPC_fractalSupportFleetAssignmentAI.get(fleet) ?: continue
            script.returnFromPatrol(reason)
        }
    }

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        if (fleet == null || from == null || to == null) return
        val system = getSystem() ?: return
        if (!fleets.contains(fleet)) return
        if (from == system && to == Global.getSector().hyperspace) {
            fleetOffDuty(fleet)
        }
    }

    /** For when a fleet leaves the space on good terms, AKA it just goes off duty. */
    private fun fleetOffDuty(fleet: CampaignFleetAPI) {
        if (fleet.memoryWithoutUpdate.getBoolean(niko_MPC_ids.OFF_DUTY)) return
        val baseFP = fleet.memoryWithoutUpdate.getFloat("\$MPC_supportFleetStartingFP")
        if (baseFP > 0) {
            val fraction = (fleet.fleetPoints / baseFP)
            returningPatrolValue += fraction
        }
        fleet.memoryWithoutUpdate[niko_MPC_ids.OFF_DUTY] = true
    }

    override fun notifyEnded() {
        super.notifyEnded()

        Global.getSector().removeScript(this)
        Global.getSector().removeListener(this)
        Global.getSector().listenerManager.removeListener(this)
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String>? {
        val tags = super.getIntelTags(map)

        tags += Tags.INTEL_COLONIES
        tags += factionForUIColors.id
        tags += niko_MPC_ids.IAIIC_FAC_ID

        return tags
    }

    fun getColony(): MarketAPI? = getFractalColony()
    fun getSystem(): StarSystemAPI? = getFractalColony()?.starSystem

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportBattleOccurred(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

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

    override fun reportBattleFinished(primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        return
    }

    override fun reportFleetDespawned(fleet: CampaignFleetAPI?, reason: FleetDespawnReason?, param: Any?) {
        return
    }

    override fun reportFleetSpawned(fleet: CampaignFleetAPI?) {
        return
    }

    override fun reportFleetReachedEntity(fleet: CampaignFleetAPI?, entity: SectorEntityToken?) {
        return
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
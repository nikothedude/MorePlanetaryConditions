package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.Script
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_aloofTargetAssignmentAI(val fleet: CampaignFleetAPI): niko_MPC_baseNikoScript(), CampaignEventListener, FleetEventListener {

    var beganJourneyPrep = false

    init {
        giveInitialAssignments()
    }

    private fun giveInitialAssignments() {

        class beginDeparture(): Script {
            override fun run() {
                startJourney()
            }

        }

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.ORBIT_PASSIVE,
            Global.getSector().economy.getMarket("eventide").primaryEntity,
            90f, // you actually have a lot of time
            "preparing for departure",
            beginDeparture()
        )
    }

    override fun startImpl() {
        fleet.addScript(this)
        fleet.addEventListener(this)
        Global.getSector().addListener(this)
    }

    override fun stopImpl() {
        fleet.removeScript(this)
        fleet.removeEventListener(this)
        Global.getSector().removeListener(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
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

    override fun reportEncounterLootGenerated(
        plugin: FleetEncounterContextPlugin?,
        loot: CargoAPI?
    ) {
        return
    }

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        return
    }

    override fun reportBattleOccurred(
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        return
    }

    override fun reportBattleFinished(
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
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

    override fun reportFleetReachedEntity(
        fleet: CampaignFleetAPI?,
        entity: SectorEntityToken?
    ) {
        return
    }

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        if (fleet != Global.getSector().playerFleet) return
        if (to != this.fleet.containingLocation) return

        if (!beganJourneyPrep) return
        startJourney()
    }

    private fun startJourney() {

        class goToChico(): Script {
            override fun run() {

                class failure(): Script {
                    override fun run() {
                        fleetReachedChico()
                    }
                }

                fleet.clearAssignments()
                fleet.addAssignmentAtStart(
                    FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
                    Global.getSector().economy.getMarket("chicomoztoc").primaryEntity,
                    Float.MAX_VALUE,
                    "travelling to Chicomoztoc",
                    failure()
                )
            }

        }

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.ORBIT_PASSIVE,
            Global.getSector().economy.getMarket("eventide").primaryEntity,
            3f,
            "preparing for departure",
            goToChico()
        )

        beganJourneyPrep = true
    }

    fun fleetReachedChico() {
        MPC_hegemonyContributionIntel.get(false)?.state = MPC_hegemonyContributionIntel.State.FAILED
        MPC_hegemonyContributionIntel.get(false)?.sendUpdateIfPlayerHasIntel(MPC_hegemonyContributionIntel.State.FAILED, false)
        MPC_hegemonyContributionIntel.get(false)?.endAfterDelay()

        val repChange = CoreReputationPlugin.CustomRepImpact()
        repChange.limit = RepLevel.VENGEFUL
        repChange.delta = -0.2f

        Global.getSector().adjustPlayerReputation(
            RepActionEnvelope(
                RepActions.CUSTOM,
                repChange, null, null, true, true
            ),
            Factions.HEGEMONY
        )
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

    override fun reportPlayerActivatedAbility(
        ability: AbilityPlugin?,
        param: Any?
    ) {
        return
    }

    override fun reportPlayerDeactivatedAbility(
        ability: AbilityPlugin?,
        param: Any?
    ) {
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

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        if (reason != null && reason != CampaignEventListener.FleetDespawnReason.PLAYER_FAR_AWAY) {
            success()
        }
    }

    fun success() {
        Global.getSector().playerMemoryWithoutUpdate["\$MPC_IAIICkilledAloofSister"] = true

        MPC_hegemonyContributionIntel.get(false)?.aloofState = MPC_hegemonyContributionIntel.AloofState.ELIMINATE_TARGET_FINISHED
        MPC_hegemonyContributionIntel.get(false)?.sendUpdateIfPlayerHasIntel(MPC_hegemonyContributionIntel.AloofState.ELIMINATE_TARGET_FINISHED, false)

        delete()
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        return
    }
}
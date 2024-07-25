package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_miscUtils.getApproximateHyperspaceLoc
import org.lazywizard.lazylib.MathUtils

class niko_MPC_magnetarStarScript(
    val magnetar: PlanetAPI
): niko_MPC_baseNikoScript(), CampaignEventListener {

    companion object {
        fun doBlindJump(fleet: CampaignFleetAPI) {
            val system = fleet.starSystem ?: return
            val approximateLoc = fleet.getApproximateHyperspaceLoc()

            val xOffset = MathUtils.getRandomNumberInRange(BLIND_JUMP_X_VARIATION_LOWER_BOUND, BLIND_JUMP_X_VARIATION_UPPER_BOUND)
            val yOffset = MathUtils.getRandomNumberInRange(BLIND_JUMP_Y_VARIATION_LOWER_BOUND, BLIND_JUMP_Y_VARIATION_UPPER_BOUND)

            approximateLoc.translate(xOffset, yOffset)
            val token = Global.getSector().hyperspace.createToken(approximateLoc.x, approximateLoc.y)

            val dest = JumpDestination(token, null)
            Global.getSector().doHyperspaceTransition(fleet, fleet, dest)
            fleet.memoryWithoutUpdate.set(niko_MPC_ids.BLIND_JUMPING, true, 2f)

            for (member in fleet.fleetData.membersListCopy) {
                member.status.applyDamage(9999999f) // very high
                member.repairTracker.cr = 0f
            }
        }

        const val MIN_DAYS_PER_PULSE = 4f
        const val MAX_DAYS_PER_PULSE = 4.2f

        const val BASE_X_COORD_FOR_SYSTEM = -71000f
        const val BASE_Y_COORD_FOR_SYSTEM = -45000f

        const val X_COORD_VARIATION_LOWER_BOUND = -1200f
        const val X_COORD_VARIATION_UPPER_BOUND = 1200f
        const val Y_COORD_VARIATION_LOWER_BOUND = -600f
        const val Y_COORD_VARIATION_UPPER_BOUND = 200f

        const val BLIND_JUMP_X_VARIATION_LOWER_BOUND = -3000f
        const val BLIND_JUMP_X_VARIATION_UPPER_BOUND = 3000f
        const val BLIND_JUMP_Y_VARIATION_LOWER_BOUND = -2000f
        const val BLIND_JUMP_Y_VARIATION_UPPER_BOUND = 3000f
    }

    val daysPerPulse = IntervalUtil(MIN_DAYS_PER_PULSE, MAX_DAYS_PER_PULSE)

    override fun startImpl() {
        magnetar.addScript(this)
        Global.getSector().addListener(this)
    }

    override fun stopImpl() {
        magnetar.removeScript(this)
        Global.getSector().removeListener(this)
    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val containingLocation = magnetar.containingLocation ?: return
        if (containingLocation != Global.getSector().playerFleet?.containingLocation) return

        val days = Misc.getDays(amount)
        daysPerPulse.advance(days)
        if (daysPerPulse.intervalElapsed()) {
            doPulse()
        }
    }

    fun doPulse() {
        val color = niko_MPC_magnetarPulse.BASE_COLOR
        val params = ExplosionEntityPlugin.ExplosionParams(color, magnetar.containingLocation, magnetar.location, 500f, 2f)
        params.damage = ExplosionEntityPlugin.ExplosionFleetDamage.LOW
        val explosion = magnetar.containingLocation.addCustomEntity(
            Misc.genUID(), "Ionized Pulse",
            "MPC_magnetarPulse", Factions.NEUTRAL, params
        )
        explosion.setLocation(magnetar.location.x, magnetar.location.y)
    }

    override fun reportFleetJumped(fleet: CampaignFleetAPI?, from: SectorEntityToken?, to: JumpDestination?) {
        if (fleet == null || !fleet.isPlayerFleet || to?.destination?.containingLocation != magnetar.containingLocation) return
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_VISITED_MAGNETAR] == true) return

        Global.getSector().intelManager.addIntel(niko_MPC_magnetarIntel())
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_VISITED_MAGNETAR] = true
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
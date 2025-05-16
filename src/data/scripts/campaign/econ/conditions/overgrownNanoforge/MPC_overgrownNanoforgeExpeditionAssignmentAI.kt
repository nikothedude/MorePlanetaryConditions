package data.scripts.campaign.econ.conditions.overgrownNanoforge

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.derelictEscort.MPC_derelictEscortAssignmentAI
import data.scripts.campaign.econ.conditions.derelictEscort.derelictEscortStates
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getEscortFleetList
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.getSourceMarket

class MPC_overgrownNanoforgeExpeditionAssignmentAI(
    val fleet: CampaignFleetAPI,
    var target: MarketAPI,
    var homeMarket: MarketAPI,
    val prepTime: Float
): niko_MPC_baseNikoScript() {

    companion object {
        /** If we exceed this, we will give up and go home. */
        const val MAX_DAYS = 120f
        const val FUEL_PERCENT_TO_GIVE_UP = 0.2f
    }

    val interval = IntervalUtil(MAX_DAYS, MAX_DAYS)
    var initialFuel = fleet.cargo.maxFuel

    init {
        giveInitialAssignments()
    }

    protected fun giveInitialAssignments() {
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE, homeMarket.primaryEntity, prepTime, "preparing for departure"
        ) {
            fleet.clearAssignments()
            fleet.addAssignmentAtStart(
                FleetAssignment.DELIVER_CREW, target.primaryEntity, MAX_DAYS, "performing covert operations"
            ) {
                bombard()
                abortAndReturnToBase()
            }
        }
    }

    private fun bombard() {
        if (fleet.visibilityLevelToPlayerFleet > SectorEntityToken.VisibilityLevel.NONE) {
            fleet.memoryWithoutUpdate["\$MPC_didOvergrownBombard"] = true
            MarketCMD.addBombardVisual(target.primaryEntity)
        }
        fleet.cargo.removeFuel(fleet.cargo.fuel * 0.8f)
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
    }

    private fun refreshAssignments() {

        if (!target.hasCondition("niko_MPC_overgrownNanoforgeCondition")) {
            return abortAndReturnToBase()
        }

        if ((fleet.cargo.maxFuel / initialFuel) <= FUEL_PERCENT_TO_GIVE_UP) {
            return abortAndReturnToBase()
        }

        if (target.primaryEntity == null || target.isInhabited()) {
            return abortAndReturnToBase()
        }
    }

    fun abortAndReturnToBase() {
        delete()
    }

    override fun delete(): Boolean {
        if (!super.delete())
            return false

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            fleet.getSourceMarket()?.primaryEntity ?: Global.getSector().economy.marketsCopy.random().primaryEntity,
            Float.MAX_VALUE,
            null
        )
        return true
    }
}
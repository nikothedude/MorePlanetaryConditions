package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import org.lazywizard.lazylib.campaign.CampaignUtils

/** Every successful [advance] run, iterates through [getPrimaryEntity]'s fleets to find fleets near [getPrimaryEntity], using
 * [niko_MPC_satelliteHandlerCore.satelliteInterferenceDistance] as the radius param.
 * We then run [niko_MPC_satelliteHandlerCore.tryToJoinBattle].(battle) if the fleet has a ongoing battle. */
class niko_MPC_satelliteFleetProximityChecker(var handler: niko_MPC_satelliteHandlerCore): niko_MPC_deltaTimeScript() {

    override val thresholdForAdvancement: Float
        get() = 0.2f
    override val onlyUseDeltaIfPlayerNotNear: Boolean
        get() = true

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun getPrimaryLocation(): LocationAPI? {
        return getPrimaryEntity()?.containingLocation
    }

    fun getPrimaryEntity(): SectorEntityToken? {
        val memoryHaver: HasMemory = handler.getPrimaryHolder() ?: return null
        if (memoryHaver is SectorEntityToken) return memoryHaver
        else if (memoryHaver is MarketAPI) return memoryHaver.primaryEntity
        return null
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {
        if (!canAdvance(amount)) return
        val primaryEntity = getPrimaryEntity() ?: return

        val fleetsWithinInterferenceDistance = CampaignUtils.getNearbyFleets(primaryEntity, handler.satelliteInterferenceDistance)
        for (fleet in fleetsWithinInterferenceDistance) {
            val battle = fleet.battle ?: continue //only fight fleets with a ongoing battle
            handler.tryToJoinBattle(battle)
        }
    }
}
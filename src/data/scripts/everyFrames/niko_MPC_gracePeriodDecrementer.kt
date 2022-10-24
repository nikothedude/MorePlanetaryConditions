package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import data.scripts.campaign.econ.conditions.defenseSatellite.handlers.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_dataLoggable
import data.utilities.niko_MPC_debugUtils

/** Every time [advance] successfully runs, iterates through every item in [niko_MPC_satelliteHandlerCore.gracePeriods]
 * and subtracts the value of each key with [cachedDeltaTime] using [niko_MPC_satelliteHandlerCore.adjustGracePeriod].*/
class niko_MPC_gracePeriodDecrementer(var handler: niko_MPC_satelliteHandlerCore) : niko_MPC_deltaTimeScript(), niko_MPC_dataLoggable {

    override val thresholdForAdvancement: Float
        get() = 0.0f
    override val doOneSecondDelayIfPlayerNotNear: Boolean
        get() = true

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }

    override fun runWhilePaused(): Boolean {
        return false
    }
    var cachedDeltaTime: Float = 0f

    override fun getPrimaryLocation(): LocationAPI? {
        return handler.getLocation()
    }

    override fun advance(amount: Float) {
        cachedDeltaTime += amount
        if (!canAdvance(amount)) return
        val iterator: MutableIterator<Map.Entry<CampaignFleetAPI, Float>> = handler.gracePeriods.entries.iterator()
        while (iterator.hasNext()) {
            val entry: Map.Entry<CampaignFleetAPI, Float> = iterator.next()
            val fleet = entry.key
            if (fleet == null) {
                niko_MPC_debugUtils.displayError("something has gone terrible wrong and a fleet was null during $this advance")
                niko_MPC_debugUtils.logDataOf(this)
                iterator.remove()
                continue
            }
            if (fleet.isExpired || fleet.isDespawning) { // to prevent memory leaks, we have to account for if the fleet is null or expired
                iterator.remove() // we dont remove on 0, because honestly theres no real reason to, we already remove them if they are deleted
                continue
            }
            handler.adjustGracePeriod(fleet, -cachedDeltaTime) // for each fleet tracked, decrement the grace period by amount
        }
        cachedDeltaTime = 0f
    }

    override fun provideLoggableData(): List<String> {
        return arrayListOf("Handler: $handler", "Handler data: ${handler.provideLoggableData()}")
    }

}
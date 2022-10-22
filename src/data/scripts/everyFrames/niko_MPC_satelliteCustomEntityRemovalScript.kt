package data.scripts.everyFrames

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler

open class niko_MPC_satelliteCustomEntityRemovalScript(var memoryHaver: HasMemory, val handler: niko_MPC_satelliteHandlerCore)
    : niko_MPC_baseNikoScript() {

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun getPrimaryLocation(): LocationAPI? {
        return handler.getLocation()
    }

    override fun advance(amount: Float) {
        if (!memoryHaver.hasSatelliteHandler(handler)) handler.delete()
        delete()
    }

    override fun start() {
        Global.getSector().addScript(this)
    }

    override fun stop() {
        Global.getSector().removeScript(this)
    }
}
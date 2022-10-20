package data.scripts.everyFrames

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.impl.campaign.ids.Tags
import data.scripts.campaign.econ.conditions.defenseSatellite.niko_MPC_satelliteHandlerCore
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_memoryUtils.deleteMemoryKey
import data.utilities.niko_MPC_satelliteUtils.hasSatelliteHandler
import data.utilities.niko_MPC_satelliteUtils.purgeSatellitesFromEntity

open class niko_MPC_satelliteCustomEntityRemovalScript(var memoryHaver: HasMemory, val handler: niko_MPC_satelliteHandlerCore)
    : niko_MPC_baseNikoScript() {

    override fun runWhilePaused(): Boolean {
        return true //todo: return to this
    }

    override fun advance(amount: Float) {
        if (!memoryHaver.hasSatelliteHandler(handler)) handler.delete()
        prepareForGarbageCollection()
    }

    override fun prepareForGarbageCollection() {
        super.prepareForGarbageCollection()

        Global.getSector().removeScript(this)
    }
}
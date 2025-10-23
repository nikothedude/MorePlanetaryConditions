package data.scripts.campaign.sinkhole

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain
import data.scripts.everyFrames.niko_MPC_baseNikoScript

class MPC_fleetTerrainScript(val terrain: BaseTerrain, val host: SectorEntityToken): niko_MPC_baseNikoScript(), FleetEventListener {

    override fun startImpl() {
        host.addScript(this)
        if (host is CampaignFleetAPI) host.addEventListener(this)
    }

    override fun stopImpl() {
        host.removeScript(this)
        if (host is CampaignFleetAPI) host.removeEventListener(this)

    }

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (!host.isAlive || host.isExpired) {
            deleteTerrain()
            delete()
            return
        }

        if (host.containingLocation != terrain.entity.containingLocation) {
            terrain.entity.containingLocation.removeEntity(terrain.entity)
            host.containingLocation.addEntity(terrain.entity)
        }
    }

    private fun deleteTerrain() {
        terrain.entity.containingLocation.removeEntity(terrain.entity)
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        deleteTerrain()
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
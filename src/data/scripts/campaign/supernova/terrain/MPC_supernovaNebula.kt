package data.scripts.campaign.supernova.terrain

import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.TerrainAIFlags
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin

class MPC_supernovaNebula: NebulaTerrainPlugin() {

    override fun getTerrainName(): String {
        return "Planetary Nebula"
    }

    override fun getNameForTooltip(): String? {
        return terrainName
    }

    override fun canPlayerHoldStationIn(): Boolean {
        return true
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        super.render(layer, viewport)
    }

    override fun hasAIFlag(flag: Any?, fleet: CampaignFleetAPI?): Boolean {
        return flag == TerrainAIFlags.REDUCES_DETECTABILITY ||
                flag == TerrainAIFlags.REDUCES_SPEED_SMALL ||
                flag == TerrainAIFlags.TILE_BASED
    }
}
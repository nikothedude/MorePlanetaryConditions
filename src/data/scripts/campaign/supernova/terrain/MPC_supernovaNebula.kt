package data.scripts.campaign.supernova.terrain

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.TerrainAIFlags
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.terrain.NebulaTerrainPlugin
import data.scripts.campaign.listeners.niko_MPC_saveListener
import java.util.zip.DataFormatException

open class MPC_supernovaNebula: NebulaTerrainPlugin(), niko_MPC_saveListener {

    init {
        Global.getSector().listenerManager.addListener(this, false)
    }

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

    override fun advance(amount: Float) {
        super.advance(amount)
    }

    override fun beforeGameSave() {
        params.tiles = null
        savedTiles = encodeTiles(tiles)
    }

    override fun onGameLoad() {
        texture = Global.getSettings().getSprite(params.cat, params.key)
        mapTexture = Global.getSettings().getSprite(params.cat, params.key + "_map")

        if (savedTiles != null) {
            try {
                tiles = decodeTiles(savedTiles, params.w, params.h)
            } catch (e: DataFormatException) {
                throw RuntimeException("Error decoding tiled terrain tiles", e)
            }
        } else {
            // shouldn't be here, if we are then savedTiles == null and something went badly wrong
            tiles = Array<IntArray?>(params.w) { IntArray(params.h) }
        }

        regenTiles()
    }

    override fun afterGameSave() {
        return
    }

    override fun onGameSaveFailed() {
        return
    }
}
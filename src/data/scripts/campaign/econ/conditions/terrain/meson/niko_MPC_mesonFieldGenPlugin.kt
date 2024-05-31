package data.scripts.campaign.econ.conditions.terrain.meson

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenDataSpec
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenPlugin

class niko_MPC_mesonFieldGenPlugin: TerrainGenPlugin {
    override fun wantsToHandle(terrainData: TerrainGenDataSpec?, context: StarSystemGenerator.GenContext?): Boolean {
        return (terrainData != null && terrainData.id == "MPC_mesonField")
    }

    override fun generate(
        terrainData: TerrainGenDataSpec?,
        context: StarSystemGenerator.GenContext?
    ): StarSystemGenerator.GenResult {
        TODO("Not yet implemented")
    }
}
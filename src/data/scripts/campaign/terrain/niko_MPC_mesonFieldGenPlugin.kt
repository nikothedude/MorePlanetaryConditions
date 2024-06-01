package data.scripts.campaign.terrain

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.GenResult
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenDataSpec
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenPlugin

class niko_MPC_mesonFieldGenPlugin: TerrainGenPlugin {

    companion object {
        const val WIDTH_PLANET = 400f // different from normal
        const val WIDTH_STAR = 600f

        fun systemCanHaveMesonField(system: StarSystemAPI): Boolean {
            if (system.tags.contains(Tags.THEME_CORE)) return false

            return true
        }

        fun planetCanHaveMesonField(planet: PlanetAPI): Boolean {
            return true
        }
    }
    // this DOES generate btw! the csv is good
    override fun wantsToHandle(terrainData: TerrainGenDataSpec?, context: StarSystemGenerator.GenContext?): Boolean {
        return (terrainData != null &&
                context != null &&
                terrainData.id == "MPC_mesonField" &&
                systemCanHaveMesonField(context.system) && // currently can only gen on planets
                context.parent is PlanetAPI &&
                planetCanHaveMesonField(context.parent))
    }

    override fun generate(
        terrainData: TerrainGenDataSpec?,
        context: StarSystemGenerator.GenContext?
    ): GenResult {
        val result = GenResult()
        result.context = context
        result.orbitalWidth = 0f

        if (terrainData == null || context == null)
            return result

        //if (!(context.star instanceof PlanetAPI)) return null;
        val system = context.system
        var parent = context.center
        if (context.parent != null) parent = context.parent

        var isStar = false
        if (parent is PlanetAPI) {
            val planet = parent
            isStar = planet.isStar
        } else if (parent === context.system.center) {
            isStar = true
        }

        if (context.parent != null) parent = context.parent

        //System.out.println("GENERATING MAG FIELD AROUND " + parent.getId());


        //System.out.println("GENERATING MAG FIELD AROUND " + parent.getId());
        val baseIndex = (niko_MPC_mesonField.baseColors.size * StarSystemGenerator.random.nextDouble()).toInt()
        val auroraIndex = (niko_MPC_mesonField.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()


        /*var visStartRadius = (planet.radius * 1.5f)
        var visEndRadius = planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET
        var bandWidth = 180f // approx the size of the band
        var midRadius = (planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET) / 1.5f*/

        val widthToUse = if (isStar) WIDTH_STAR else WIDTH_PLANET
        val orbitalWidth = widthToUse

        var visStartRadius = (parent.radius * 1.5f)
        var visEndRadius = visStartRadius + widthToUse
        var bandWidth = (visEndRadius - visStartRadius) * 0.6f
        var midRadius = (visStartRadius + visEndRadius) / 2
//		float visStartRadius = parent.getRadius() + 50f;
//		float visEndRadius = parent.getRadius() + 50f + WIDTH_PLANET + 50f;
        //		float visStartRadius = parent.getRadius() + 50f;
//		float visEndRadius = parent.getRadius() + 50f + WIDTH_PLANET + 50f;
        var auroraProbability = 1f

        val params = niko_MPC_mesonField.mesonFieldParams(
            bandWidth,
            midRadius,
            parent,
            visStartRadius,
            visEndRadius,
            niko_MPC_mesonField.baseColors[baseIndex],
            auroraProbability,
            niko_MPC_mesonField.auroraColors[auroraIndex].toList(),
        )
        val mesonField = system.addTerrain("MPC_mesonField", params)
        mesonField.setCircularOrbit(parent, 0f, 0f, 100f)

        result.onlyIncrementByWidth = !isStar
        result.entities.add(mesonField)
        result.orbitalWidth = orbitalWidth
        return result
    }
}
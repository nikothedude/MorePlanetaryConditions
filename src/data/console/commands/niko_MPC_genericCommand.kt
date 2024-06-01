package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import data.scripts.campaign.terrain.niko_MPC_mesonField
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin.Companion.WIDTH_PLANET
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils

class niko_MPC_genericCommand: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerLoc = Global.getSector().playerFleet.starSystem
        if (playerLoc == null) {
            Console.showMessage("failure - null loc")
            return BaseCommand.CommandResult.ERROR
        }

        for (planet in playerLoc.planets) {
            if (planet.isStar) continue

            val baseIndex = (niko_MPC_mesonField.baseColors.size * StarSystemGenerator.random.nextDouble()).toInt()
            val auroraIndex = (niko_MPC_mesonField.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()


            /*var visStartRadius = (planet.radius * 1.5f)
            var visEndRadius = planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET
            var bandWidth = 180f // approx the size of the band
            var midRadius = (planet.radius + niko_MPC_mesonFieldGenPlugin.WIDTH_PLANET) / 1.5f*/

            var visStartRadius = (planet.radius * 1.5f)
            var visEndRadius = visStartRadius + WIDTH_PLANET
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
                planet,
                visStartRadius,
                visEndRadius,
                niko_MPC_mesonField.baseColors[baseIndex],
                auroraProbability,
                niko_MPC_mesonField.auroraColors[auroraIndex].toList(),
            )
            val mesonField = playerLoc.addTerrain("MPC_mesonField", params)
            mesonField.setCircularOrbit(planet, 0f, 0f, 100f)
            break
        }

        Console.showMessage("success")
        return BaseCommand.CommandResult.SUCCESS
    }
}
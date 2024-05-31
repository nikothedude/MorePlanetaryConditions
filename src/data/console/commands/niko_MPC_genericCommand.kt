package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import data.scripts.campaign.econ.conditions.terrain.meson.niko_MPC_mesonField
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

            val bandwidth = planet.radius + MagFieldGenPlugin.WIDTH_PLANET
            val middleRadius = (planet.radius + MagFieldGenPlugin.WIDTH_PLANET) / 2
            val innerRadius = 80f
            val outerRadius = 1600f * MathUtils.getRandomNumberInRange(0.8f, 1.2f)

            val baseColor = niko_MPC_mesonField.MESON_COLOR
            val auroraIndex = (MathUtils.getRandomNumberInRange(0, niko_MPC_mesonField.auroraColors.size))

            val params = niko_MPC_mesonField.mesonFieldParams(
                bandwidth,
                middleRadius,
                planet,
                innerRadius,
                outerRadius,
                baseColor,
                niko_MPC_mesonField.auroraColors[auroraIndex].toList()
            )
            playerLoc.addTerrain("MPC_mesonField", params)
        }

        Console.showMessage("success")
        return BaseCommand.CommandResult.SUCCESS
    }
}
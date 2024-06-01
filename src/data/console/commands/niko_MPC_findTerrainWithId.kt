package data.console.commands

import com.fs.starfarer.api.Global
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class niko_MPC_findTerrainWithId: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (!context.isInCampaign) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }
        if (args.isEmpty()) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }
        val tmp = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (tmp.size != 1) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }
        val terrainId = tmp[0]
        val systems = Global.getSector().starSystems
        for (system in systems) {
            for (terrain in system.terrainCopy) {
                if (terrain.plugin.terrainId != terrainId) continue
                var constellationString = "This system has no constellation."
                val hasConstellation = system.constellation != null
                if (hasConstellation) {
                    constellationString = "Constellation: ${system.constellation.name}"
                }
                var orbitString = "orbiting nothing"
                val orbitFocus = terrain.orbitFocus
                if (orbitFocus != null) {
                    orbitString = "orbiting ${orbitFocus.name}"
                }

                val systemName = system.name

                Console.showMessage("$systemName has ${terrain.plugin.terrainName}, $orbitString. $constellationString")
            }
        }
        return BaseCommand.CommandResult.SUCCESS
    }
}
package data.console.commands

import com.fs.starfarer.api.Global
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class niko_MPC_findPlanetsWithCondition : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        if (!context.isInCampaign) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY)
            return CommandResult.WRONG_CONTEXT
        }
        if (args.isEmpty()) {
            return CommandResult.BAD_SYNTAX
        }
        val tmp = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (tmp.size != 1) {
            return CommandResult.BAD_SYNTAX
        }
        val conditionId = tmp[0]
        val systems = Global.getSector().starSystems
        for (system in systems) {
            for (planet in system.planets) {
                if (planet.hasCondition(conditionId)) {
                    val displayConditionName = planet.market != null && planet.market.hasCondition(conditionId)
                    var toDisplay = conditionId
                    if (displayConditionName) {
                        toDisplay = planet.market.getCondition(conditionId).name
                    }
                    val hasConstellation = system.constellation != null
                    var constellationString = "This system has no constellation."
                    if (hasConstellation) {
                        constellationString = "Constellation: " + system.constellation.name
                    }
                    Console.showMessage(
                        planet.name + " has " + toDisplay + ", location " +
                                system.name + ", " + constellationString
                    )
                }
            }
        }
        return CommandResult.SUCCESS
    }
}
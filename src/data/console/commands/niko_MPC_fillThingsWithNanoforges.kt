package data.console.commands

import com.fs.starfarer.api.Global
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class niko_MPC_fillThingsWithNanoforges: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        var i = 3
        var failsafeIndex = 250
        while (i > 0) {
            failsafeIndex--
            if (failsafeIndex <= 0) {
                Console.showMessage("failsafe hit")
                return BaseCommand.CommandResult.ERROR
            }
            val randSystem = Global.getSector().starSystems.random()
            if (randSystem.isEnteredByPlayer) continue
            val randPlanet = randSystem.planets.randomOrNull() ?: continue
            if (randPlanet.market == null) continue
            if (randPlanet.market.isInhabited()) continue

            randPlanet.market.addCondition(niko_MPC_ids.overgrownNanoforgeConditionId)
            i--
        }
        Console.showMessage("success")
        return BaseCommand.CommandResult.SUCCESS
    }
}
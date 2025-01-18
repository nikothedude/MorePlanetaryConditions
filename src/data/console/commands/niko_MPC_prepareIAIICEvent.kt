package data.console.commands

import com.fs.starfarer.api.impl.campaign.econ.AICoreAdmin
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyHostileActivityFactor
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.rulecmd.MPC_hegemonySpyCMD
import data.utilities.niko_MPC_settings
import org.apache.log4j.Level
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class niko_MPC_prepareIAIICEvent: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()
        if (fractalColony == null) {
            Console.showMessage("Error: Must have a colony with a fractal core admin")
            return BaseCommand.CommandResult.ERROR
        }

        AICoreAdmin.get(fractalColony)?.daysActive = MPC_hegemonySpyCMD.DAYS_NEEDED_FOR_SPY_TO_APPEAR.toFloat()
        HegemonyHostileActivityFactor.setPlayerDefeatedHegemony()

        Console.showMessage("Success! IAIIC event is now prepared - just visit the fractal core colony bar.")

        return BaseCommand.CommandResult.SUCCESS
    }
}
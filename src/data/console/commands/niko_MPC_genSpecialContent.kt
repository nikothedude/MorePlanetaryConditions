package data.console.commands

import data.niko_MPC_modPlugin
import data.scripts.campaign.niko_MPC_specialProcGenHandler.doSpecialProcgen
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class niko_MPC_genSpecialContent: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        doSpecialProcgen(true)

        Console.showMessage("special content updated to current version! note this does not guarantee perfect parity. " +
                "hints: look around neutron stars")
        return BaseCommand.CommandResult.SUCCESS
    }
}
package data.console.commands

import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings.loadSettings
import org.json.JSONException
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console
import java.io.IOException

class niko_MPC_loadSettings : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        try {
            loadSettings()
        } catch (ex: Exception) {
            throw RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during command execution! Exception: " + ex)
        }
        Console.showMessage("Success! Settings have been reloaded.")
        return CommandResult.SUCCESS
    }
}
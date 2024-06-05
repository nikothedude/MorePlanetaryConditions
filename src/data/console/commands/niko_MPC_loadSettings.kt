package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import data.utilities.niko_MPC_settings.loadAllSettings
import data.utilities.niko_MPC_settings.loadSettings
import org.apache.log4j.Level
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console

class niko_MPC_loadSettings : BaseCommand {
    override fun runCommand(args: String, context: CommandContext): CommandResult {
        try {
            loadAllSettings()
        } catch (ex: Exception) {
            val errorCode = "runCommand failed due to thrown exception: $ex, ${ex.cause}"
            Console.showMessage(errorCode, Level.ERROR)
            return CommandResult.ERROR
        }
        Console.showMessage("Success! Settings have been reloaded.")

        return CommandResult.SUCCESS
    }
}
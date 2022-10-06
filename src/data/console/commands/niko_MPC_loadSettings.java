package data.console.commands;

import data.niko_MPC_modPlugin;
import data.utilities.niko_MPC_ids;
import data.utilities.niko_MPC_settings;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.io.IOException;

public class niko_MPC_loadSettings implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        try {
            niko_MPC_settings.loadSettings();
        } catch (IOException | JSONException | NullPointerException ex) {
            throw new RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during command execution! Exception: " + ex);
        }
        Console.showMessage("Success! Settings have been reloaded.");
        return CommandResult.SUCCESS;
    }
}

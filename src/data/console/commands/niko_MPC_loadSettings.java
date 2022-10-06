package data.console.commands;

import data.niko_MPC_modPlugin;
import data.utilities.niko_MPC_ids;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.lazywizard.console.BaseCommand;

import java.io.IOException;

public class niko_MPC_loadSettings implements BaseCommand {
    @Override
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        try {
            niko_MPC_modPlugin.loadSettings();
        } catch (IOException | JSONException | NullPointerException ex) {
            throw new RuntimeException(niko_MPC_ids.niko_MPC_masterConfig + " loading failed during command execution!");
        }
        return CommandResult.SUCCESS;
    }
}

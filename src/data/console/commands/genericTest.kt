package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.StarSystem
import data.scripts.campaign.econ.conditions.terrain.hyperspace.niko_MPC_realspaceHyperspace
import data.utilities.niko_MPC_reflectionUtils.get
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lwjgl.util.vector.Vector2f

class genericTest: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val index = 8
        val playerFleet = Global.getSector().playerFleet
        val plugin =
            (playerFleet.containingLocation as StarSystem).terrain[index].plugin as niko_MPC_realspaceHyperspace

        for (tracker in plugin.activeCells) {
            if (tracker.isNotEmpty()) {
                for (entry in tracker) {
                    if (entry != null) {
                        val placeholderVar = 5
                    }
                }
            }
        }
        Console.showMessage("success")
        return BaseCommand.CommandResult.SUCCESS
    }
}
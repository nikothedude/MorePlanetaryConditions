package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.supernova.MPC_supernovaActionScript
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import org.lazywizard.console.BaseCommand

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val star = Global.getSector().currentLocation.planets[0]
        val script = MPC_supernovaActionScript(star)
        script.start()

        return BaseCommand.CommandResult.SUCCESS
    }

    /*class soundScript(val fleet: CampaignFleetAPI): niko_MPC_baseNikoScript() {

        companion object {
            fun get(fleet: CampaignFleetAPI): soundScript {
                var timer = Global.getSector().memoryWithoutUpdate["\$wdhuadjawd"] as? soundScript
                if (timer == null) {
                    timer = soundScript(fleet)
                    timer.start()
                    Global.getSector().memoryWithoutUpdate["\$wdhuadjawd"] = timer
                }
                return timer
            }
        }

        var soundId: String = ""

        override fun startImpl() {
            fleet.addScript(this)
        }

        override fun stopImpl() {
            fleet.removeScript(this)
        }

        override fun runWhilePaused(): Boolean {
            return true
        }

        override fun advance(amount: Float) {
            Global.getSoundPlayer().playLoop(soundId, fleet, 1f, 1f, fleet.location, Misc.ZERO)
        }
    }*/
}
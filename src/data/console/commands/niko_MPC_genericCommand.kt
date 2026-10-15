package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.abilities.MPC_magnetarPulseAbility
import data.scripts.campaign.magnetar.niko_MPC_magnetarPulse
import lunalib.lunaUtil.campaign.LunaCampaignRenderer
import lunalib.lunaUtil.campaign.LunaCampaignRenderingPlugin
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU
import org.lwjgl.util.glu.Sphere
import org.lwjgl.util.vector.Vector2f
import org.lwjgl.util.vector.Vector3f
import java.util.*
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tan

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerFleet = Global.getSector().playerFleet

        //LunaCampaignRenderer.addTransientRenderer(MPC_vignetteRenderer())

        Console.showMessage("A")

        return BaseCommand.CommandResult.SUCCESS
    }
}
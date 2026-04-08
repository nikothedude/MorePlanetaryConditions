package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import data.scripts.campaign.sinkhole.MPC_sinkholeTerrain
import org.lazywizard.console.BaseCommand
import data.utilities.niko_MPC_settings
import kotlin.collections.random

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerFleet = Global.getSector().playerFleet

        /*val fleet = Global.getSector().playerFleet.containingLocation.fleets[4] ?: return BaseCommand.CommandResult.ERROR
        val params = MPC_sinkholeTerrain.SinkholeParams(
            1400f,
            200f,
            fleet
        )
        val paramsTwo = MPC_sinkholeTerrain.SinkholeParams(
            1400f,
            200f,
            playerFleet
        )*/

        val widthToUse = 325f
        var visStartRadius = (playerFleet.radius * 2f)
        var visEndRadius = visStartRadius + widthToUse
        var bandWidth = (visEndRadius - visStartRadius) * 0.6f
        var midRadius = (visStartRadius + visEndRadius) / 2
        var auroraProbability = 1f
        val auroraIndex =
            (MagFieldGenPlugin.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()
        val params = MagneticFieldTerrainPlugin.MagneticFieldParams(
            bandWidth, midRadius,
            playerFleet,
            visStartRadius, visEndRadius,
            niko_MPC_settings.hyperMagFieldColors.random(),
            auroraProbability,
            *MagFieldGenPlugin.auroraColors[auroraIndex],
        )

        /*MPC_sinkholeTerrain.addFieldToEntity(fleet, "test", params)
        MPC_sinkholeTerrain.addFieldToEntity(playerFleet, "ttest", paramsTwo)*/

        MPC_sinkholeTerrain.addTerrainToEntity(
            playerFleet,
            Terrain.MAGNETIC_FIELD,
            params
        )

        return BaseCommand.CommandResult.SUCCESS
    }
}
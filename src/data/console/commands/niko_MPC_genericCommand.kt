package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.RingBandAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.ExplosionEntityPlugin
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.input.InputEventMouseButton
import com.fs.starfarer.api.input.InputEventType
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignState
import com.fs.starfarer.ui.impl.StandardTooltipV2Expandable
import com.fs.state.AppDriver
import data.niko_MPC_modPlugin
import data.scripts.campaign.econ.industries.missileLauncher.MPC_aegisMissileEntityPlugin
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.niko_MPC_specialProcGenHandler
import data.scripts.campaign.singularity.MPC_energyFieldInstance
import data.scripts.campaign.singularity.MPC_singularityHyperspaceProximityChecker
import data.scripts.campaign.sinkhole.MPC_sinkholeTerrain
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.MPC_abyssUtils
import data.utilities.niko_MPC_ids
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.*
import org.magiclib.util.MagicCampaign
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin
import data.utilities.niko_MPC_dialogUtils.getChildrenCopy
import data.utilities.niko_MPC_reflectionUtils
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
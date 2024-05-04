package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.StarSystem
import data.scripts.campaign.econ.conditions.terrain.hyperspace.niko_MPC_realspaceHyperspace
import data.scripts.campaign.econ.conditions.terrain.magfield.niko_MPC_hyperMagField
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.niko_MPC_miscUtils.setArc
import data.utilities.niko_MPC_reflectionUtils
import data.utilities.niko_MPC_settings
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.MathUtils.getDistance
import org.lwjgl.util.vector.Vector2f
import kotlin.math.pow

class niko_MPC_setHypercloud: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {

        val playerFleet = Global.getSector().playerFleet ?: return BaseCommand.CommandResult.ERROR
        val playerSystem = playerFleet.containingLocation as? StarSystemAPI ?: return BaseCommand.CommandResult.ERROR

        val randPlanet = playerSystem.planets.randomOrNull() ?: return BaseCommand.CommandResult.ERROR
        if (randPlanet.isStar) return BaseCommand.CommandResult.ERROR

        /*val bandwidth = 1600f
        val middleRadius = randPlanet.radius + 400f
        val innerRadius = 50f
        val outerRadius = 1600f

        val auroraProbability = 0.25f + 0.75f * StarSystemGenerator.random.nextFloat()

        val baseIndex = (MagFieldGenPlugin.baseColors.size * StarSystemGenerator.random.nextDouble()).toInt()
        val auroraIndex = (MagFieldGenPlugin.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()

        val ringParams = MagneticFieldParams(
            bandwidth, middleRadius,
            randPlanet,
            innerRadius, outerRadius,
            hyperMagFieldColors.random(),
            auroraProbability,
            *MagFieldGenPlugin.auroraColors[auroraIndex],
        )
        val magField: CampaignTerrainAPI = playerSystem.addTerrain("MPC_magnetic_field_hyper", ringParams) as? CampaignTerrainAPI ?: return BaseCommand.CommandResult.ERROR
        val plugin: MagneticFieldTerrainPlugin = magField.plugin as? MagneticFieldTerrainPlugin ?: return BaseCommand.CommandResult.ERROR
        magField.location.set(randPlanet.location)
        magField.setCircularOrbit(randPlanet, 0f, 0f, 100f)*/

        /*val bonus = 0
        val orbitRadius: Float? = randPlanet.circularOrbitRadius
        val radiusToUse = orbitRadius ?: (randPlanet.radius / 100).pow(2) // 100 is arbitrary
        val perimeter = (((radiusToUse) + bonus).toInt())

        var tiles = ""
        var index = perimeter.toDouble()//.pow(2)
        while(index-- > 0) { // increase tiles to cover the entire orbit, basically, dont worry about removing xes
            tiles += "x"
        }
        var tiles =
            "xxxxxx" +
            "xxxxxx" +
            "xxxxxx" +
            "xxxxxx" +
            "xxxxxx" +
            "xxxxxx"
        var height = 6
        var width = 6

        val terrainType = "MPC_realspaceHyperspace"

        val tilesWide = 12
        val tilesHigh = 12

        val tileParams = BaseTiledTerrain.TileParams(
            tiles,
            height, width,
            "terrain",
            "deep_hyperspace",
            tilesWide,
            tilesHigh,
            null
        )
        tileParams.w

        val nebula = playerSystem.addTerrain(terrainType, tileParams) as? CampaignTerrainAPI ?: return BaseCommand.CommandResult.ERROR
        nebula.location?.set(randPlanet.location)

        if (randPlanet.orbit != null) {
            var timesToIterate = randPlanet.orbit.orbitalPeriod
            nebula.orbit = (randPlanet.orbit.makeCopy())
            while (timesToIterate-- > 0) { // try days if this doesnt work
                nebula.orbit.advance(Global.getSector().clock.convertToDays(1f))
                //val newNebula = playerSystem.addTerrain(terrainType, tileParams) as? CampaignTerrainAPI ?: return BaseCommand.CommandResult.ERROR
                nebula.location?.set(nebula.location)
            }
            nebula.orbit = null
            val newNebula = playerSystem.addTerrain(terrainType, tileParams) as? CampaignTerrainAPI ?: return BaseCommand.CommandResult.ERROR
            newNebula.location.set(-nebula.location.x, -nebula.location.y)
            //niko_MPC_nebulaStickerScript(randPlanet, nebula.plugin as niko_MPC_realspaceHyperspace).start()
        } */

        val nebula = Misc.addNebulaFromPNG("data/campaign/terrain/generic_system_nebula.png",
            0f, 0f, playerSystem, "terrain", "deep_hyperspace", 4, 4, "MPC_realspaceHyperspace", playerSystem.age) as? CampaignTerrainAPI ?: return BaseCommand.CommandResult.ERROR

        val orbit = randPlanet.orbit
        val orbitFocus = orbit.focus
        val radius = randPlanet.radius
        val buffer = radius * 4f
        var sizeBonus = 0f

        val arcSource = Vector2f()

        val dist: Float

        if (orbitFocus == null) {
            arcSource.set(randPlanet.location)
            dist = 0f
        } else {
            arcSource.set(orbitFocus.location)
            dist = getDistance(randPlanet, orbitFocus) + orbitFocus.radius + randPlanet.radius
            var lastFocus = orbitFocus
            var recursiveFocus = orbitFocus.orbitFocus
            var maxDistanceFound = 0f

            while (recursiveFocus != null) {
                arcSource.set(recursiveFocus.location)
                val recursiveDist = getDistance(lastFocus, randPlanet) + lastFocus.radius + randPlanet.radius
                if (recursiveDist > maxDistanceFound) {
                    maxDistanceFound = recursiveDist
                    sizeBonus += recursiveDist
                }
                lastFocus = recursiveFocus
                recursiveFocus = recursiveFocus.orbitFocus
            }
        }

        val innerRadius = (dist - buffer + sizeBonus).coerceAtLeast(0f)
        val outerRadius = (dist + buffer + sizeBonus)
        val level = 100

        val plugin = nebula.plugin as niko_MPC_realspaceHyperspace
        val nebulaEditor = NebulaEditor(plugin)

        nebulaEditor.setArc(level, arcSource.x, arcSource.y,  innerRadius, outerRadius, 0f, 360f)

        val middleRadius = randPlanet.radius + 400f
        val innerMagRadius = 80f
        val outerMagRadius = 1600f
        val bandwidth = outerRadius

        val auroraProbability = 0.25f + 0.75f * StarSystemGenerator.random.nextFloat()

        val auroraIndex = (MagFieldGenPlugin.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()

        val ringParams = MagneticFieldTerrainPlugin.MagneticFieldParams(
            bandwidth, middleRadius,
            randPlanet,
            innerMagRadius, outerMagRadius,
            niko_MPC_settings.hyperMagFieldColors.random(),
            auroraProbability,
            *MagFieldGenPlugin.auroraColors[auroraIndex],
        )
        val magField = randPlanet.containingLocation.addTerrain("MPC_magnetic_field_hyper", ringParams)
        magField.setCircularOrbit(randPlanet, 0f, 0f, 10f)


        Console.showMessage("done")
        return BaseCommand.CommandResult.SUCCESS
    }
}

class niko_MPC_nebulaStickerScript(
    val target: SectorEntityToken,
    val plugin: niko_MPC_realspaceHyperspace
): niko_MPC_baseNikoScript() {
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (target.location == null) return
        if (target.orbit == null) return

        //plugin.entity?.location?.set(target.location)
        plugin.entity.facing = MathUtils.getRandomNumberInRange(5f, 50f)
    }

    override fun startImpl() {
        target.addScript(this)
    }

    override fun stopImpl() {
        target.removeScript(this)
    }
}
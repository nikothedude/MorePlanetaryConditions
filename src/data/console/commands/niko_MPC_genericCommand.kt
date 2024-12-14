package data.console.commands

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.campaign.RingBandAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin.DebrisFieldParams
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.singularity.MPC_energyFieldInstance
import data.scripts.campaign.singularity.MPC_singularityHyperspaceProximityChecker
import data.utilities.MPC_abyssUtils
import data.utilities.niko_MPC_ids
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.*
import org.magiclib.util.MagicCampaign
import java.awt.Color

class niko_MPC_genericCommand: BaseCommand {

    companion object {
        const val CHANCE_FOR_MOORED_DMODS = 90f
    }
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        /*if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.SINGULARITY_SYSTEM_MEMID] != null) return BaseCommand.CommandResult.ERROR

        val tmp = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (tmp.size == 1) {
            /*val playerFleet = Global.getSector().playerFleet
            Console.showMessage("X: ${playerFleet.location.x}, Y: ${playerFleet.location.y}")
            return BaseCommand.CommandResult.SUCCESS*/

            
        }

        val sysName = "Perseus BH 19042+2CG"
        val system = Global.getSector().createStarSystem(sysName)
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.SINGULARITY_SYSTEM_MEMID] = system
        system.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "music_campaign_alpha_site"
        system.backgroundTextureFilename = "graphics/backgrounds/background_galatia.jpg"
        val hole = system.initStar("MPC_ultimaSingularity", StarTypes.BLACK_HOLE, 400f, 700f, 10f, 0.2f, 6f)
        //hole.customDescriptionId = "MPC_ultimaSingularity"
        hole.descriptionIdOverride = "MPC_ultimaSingularity"
        system.removeEntity(hole.getCoronaFor()?.entity)
        val starData = Global.getSettings()
            .getSpec(StarGenDataSpec::class.java, hole.spec.planetType, false) as StarGenDataSpec
        var corona: Float =
            hole.radius * (starData.coronaMult + starData.coronaVar * (MathUtils.getRandom().nextFloat() - 0.5f))
        if (corona < starData.coronaMin) corona = starData.coronaMin

        val eventHorizon = system.addTerrain(
            Terrain.EVENT_HORIZON,
            CoronaParams(
                hole.radius + corona, (hole.radius + corona) / 2f,
                hole, starData.solarWind,
                (starData.minFlare + (starData.maxFlare - starData.minFlare) * MathUtils.getRandom().nextFloat()),
                starData.crLossMult
            )
        ) as CampaignTerrainAPI
        //(eventHorizon.plugin as EventHorizonPlugin).params.name = "REALITY TORN ASUNDER."
        eventHorizon.setCircularOrbit(hole, 0f, 0f, 100f)
        system.lightColor = Color(255, 255, 255)

        val xCoord = -81720f
        val yCoord = -46840f
        system.location.set(xCoord, yCoord)

        val planetOneInitialAngle = MathUtils.getRandomNumberInRange(0f, 360f)
        val planetOne = system.addPlanet(
            "MPC_singularityPlanetOne",
            hole,
            "$sysName 1",
            Planets.PLANET_LAVA,
            planetOneInitialAngle,
            60f,
            2300f,
            -130f
        )
        planetOne.market.addCondition(Conditions.ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.RARE_ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.NO_ATMOSPHERE)
        planetOne.market.addCondition(Conditions.VERY_HOT)
        planetOne.market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)
        planetOne.market.addCondition(Conditions.METEOR_IMPACTS)
        planetOne.market.addCondition("MPC_fracturedWorld")
        planetOne.descriptionIdOverride = "MPC_planetPhaseAlloy"

        val asteroidWidth = 220f
        val asteroidRadius = (planetOne.radius) + (asteroidWidth * 0.5f)
        val belt = system.addAsteroidBelt(
            planetOne,
            40,
            asteroidRadius,
            asteroidWidth,
            6f,
            9f,
            Terrain.ASTEROID_BELT,
            "Planetary Remnant"
        ) as CampaignTerrainAPI
        val plugin = (belt.plugin as AsteroidBeltTerrainPlugin)
        plugin.params.minSize = 3f
        plugin.params.maxSize = 8f

        val randomSeed = MathUtils.getRandom()
        val orbitRadius = 3000f

        val texBandWidth = 256.0f
        var numBands = 34

        var ensureCenterIsIce = true
        var timesLeft = 2
        while (timesLeft-- > 0) {
            for (i in 0 until numBands) {
                val radius: Float = orbitRadius - i.toFloat() * texBandWidth * 0.25f - i.toFloat() * texBandWidth * 0.1f
                val ring =
                    if (ensureCenterIsIce && ((i + 1) >= (numBands - 1))) "rings_ice0" else if (randomSeed.nextBoolean()) "rings_ice0" else "rings_dust0"
                val ringIndex: Int = randomSeed.nextInt(2)
                val visual: RingBandAPI = system.addRingBand(
                    hole,
                    "misc",
                    ring,
                    256.0f,
                    ringIndex,
                    Color(46, 35, 173),
                    texBandWidth,
                    radius + texBandWidth / 2.0f,
                    -(radius / (30.0f + 10.0f * randomSeed.nextFloat()))
                )
                visual.isSpiral = true
                visual.minSpiralRadius = 0f
                visual.spiralFactor = 1f + randomSeed.nextFloat() * 5.0f
            }
            ensureCenterIsIce = false
            numBands = 34
        }
        //terrain.setLocation(hole.location.x, hole.location.y)
        /*val xVariation = MathUtils.getRandomNumberInRange(
            niko_MPC_magnetarStarScript.X_COORD_VARIATION_LOWER_BOUND,
            niko_MPC_magnetarStarScript.X_COORD_VARIATION_UPPER_BOUND
        )
        val yVariation = MathUtils.getRandomNumberInRange(
            niko_MPC_magnetarStarScript.Y_COORD_VARIATION_LOWER_BOUND,
            niko_MPC_magnetarStarScript.Y_COORD_VARIATION_UPPER_BOUND
        )*/

        val violenceRadius = 1500f
        val shieldOne = system.addRingBand(
            hole,
            "planets",
            "MPCatmosphere3_red",
            10f,
            1,
            Color.RED,
            50f,
            violenceRadius,
            5f
        ) as RingBandAPI
        val shieldOneOuter = system.addRingBand(
            hole,
            "planets",
            "MPCshieldInnerRed",
            256f,
            1,
            Color.RED,
            60f,
            violenceRadius,
            90f
        ) as RingBandAPI
        val guardStationOne = system.addSalvageEntity(null, "MPC_singularityGuardStationViolence", Factions.NEUTRAL, null)
        guardStationOne.id = "MPC_singularityGuardStationViolence"
        guardStationOne.setCircularOrbitPointingDown(hole, 0f, violenceRadius + 40f, 200f)

        val intelligenceRadius = 1000f
        val shieldTwo = system.addRingBand(
            hole,
            "planets",
            "MPCatmosphere3_blue",
            10f,
            1,
            Color.BLUE,
            50f,
            intelligenceRadius,
            5f
        ) as RingBandAPI
        val shieldTwoOuter = system.addRingBand(
            hole,
            "planets",
            "MPCshieldInnerBlue",
            256f,
            1,
            Color.BLUE,
            60f,
            intelligenceRadius,
            90f
        ) as RingBandAPI
        val guardStationTwo = system.addSalvageEntity(null, "MPC_singularityGuardStationIntelligence", Factions.NEUTRAL, null)
        guardStationTwo.id = "MPC_singularityGuardStationIntelligence"
        guardStationTwo.setCircularOrbitPointingDown(hole, 30f, intelligenceRadius + 40f, 120f)

        val moralityRadius = 500f
        val shieldThree = system.addRingBand(
            hole,
            "planets",
            "MPCatmosphere3_green",
            10f,
            1,
            Color.GREEN,
            50f,
            moralityRadius,
            5f
        ) as RingBandAPI
        val shieldThreeOuter = system.addRingBand(
            hole,
            "planets",
            "MPCshieldInnerGreen",
            256f,
            1,
            Color.GREEN,
            60f,
            moralityRadius,
            90f
        ) as RingBandAPI
        val guardStationThree = system.addSalvageEntity(null, "MPC_singularityGuardStationMorality", Factions.NEUTRAL, null)
        guardStationThree.id = "MPC_singularityGuardStationMorality"
        guardStationThree.setCircularOrbitPointingDown(hole, 230f, moralityRadius + 40f, -90f)

        val entranceBandwidth = 890f
        val entranceMiddle = 0f
        val theEntranceParams = RingParams(
            entranceBandwidth,
            entranceMiddle,
            hole,
            "A n d  t h e  c h o i r  s a n g  h i s  n a m e ."
        )
        val theEntrance = system.addTerrain(Terrain.RING, theEntranceParams)
        theEntrance.addTag(Tags.ACCRETION_DISK)
        theEntrance.setCircularOrbit(hole, 0f, 0f, -100f)
        val mantleBandwidth = 450f
        val mantleMiddle = (entranceBandwidth * 0.6f) + (mantleBandwidth / 2f)
        val theMantleParams = RingParams(
            mantleBandwidth,
            mantleMiddle,
            hole,
            "He descended from his gilded throne..."
        )
        val theMantle = system.addTerrain(Terrain.RING, theMantleParams) as CampaignTerrainAPI
        theMantle.addTag(Tags.ACCRETION_DISK)
        theMantle.setCircularOrbit(hole, 0f, 0f, -100f)
        val exteriorBandwidth = 435f
        val exteriorMiddle = ((mantleBandwidth + entranceBandwidth + mantleMiddle) * 0.508f) + (exteriorBandwidth / 2f)
        val theExteriorParams = RingParams(
            exteriorBandwidth,
            exteriorMiddle,
            hole,
            "The chariot arrived to paradise."
        )
        val theExterior = system.addTerrain(Terrain.RING, theExteriorParams)
        theExterior.addTag(Tags.ACCRETION_DISK)
        theExterior.setCircularOrbit(hole, 0f, 0f, -100f)

        val theWildsBandwidth = 1250f
        val theWildsMiddle = ((mantleBandwidth + entranceBandwidth + mantleMiddle + exteriorBandwidth + exteriorMiddle) * 0.416f) + (theWildsBandwidth / 2f)
        val theWildsParams = RingParams(
            theWildsBandwidth,
            theWildsMiddle,
            hole,
            "In the dark, a light."
        )
        val theWilds = system.addTerrain(Terrain.RING, theWildsParams)
        theWilds.addTag(Tags.ACCRETION_DISK)
        theWilds.setCircularOrbit(hole, 0f, 0f, -100f)

        val stagingStation = system.addSalvageEntity(null, "MPC_singularityStagingStation", Factions.NEUTRAL, null)
        stagingStation.setCircularOrbitPointingDown(hole, 340f, 4950f, 365f)
        stagingStation.setAbandonedStationMarket("MPC_singularityStagingStation_market")
        setupMooredShips(stagingStation)
        val stagingDebrisRadius = (stagingStation.radius + 150f)
        val stagingDebrisParams = DebrisFieldParams(
            stagingDebrisRadius,
            3f,
            Float.MAX_VALUE,
            0f
        )
        stagingDebrisParams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.GEN
        stagingDebrisParams.baseSalvageXP = 500

        val stagingDebrisField = system.addDebrisField(stagingDebrisParams, MathUtils.getRandom())
        stagingDebrisField.setLocation(stagingStation.location.x, stagingStation.location.y)
        stagingDebrisField.setCircularOrbit(stagingStation, 5f, 0f, 350f)
        stagingDebrisField.isDiscoverable = true
        stagingDebrisField.sensorProfile = 700f

        val derelictOne = MagicCampaign.createDerelict("atlas_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, true, 50, true, stagingDebrisField, 20f, stagingDebrisRadius, 30f, true)
        val derelictTwo = MagicCampaign.createDerelict("colossus_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, true, 50, true, stagingDebrisField, 50f, stagingDebrisRadius * 0.91f, 26f, true)
        val derelictThree = MagicCampaign.createDerelict("colossus_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, true, 50, true, stagingDebrisField, 110f, stagingDebrisRadius * 0.6f, 26f, true)
        val derelictFour = MagicCampaign.createDerelict("lasher_Standard", ShipRecoverySpecial.ShipCondition.WRECKED, true, 50, true, stagingDebrisField, 250f, stagingDebrisRadius * 0.4f, 19f, true)
        derelictOne.addTag("MPC_habitatWreck")
        derelictTwo.addTag("MPC_habitatWreck")
        derelictThree.addTag("MPC_habitatWreck")
        derelictFour.addTag("MPC_habitatWreck")

        system.memoryWithoutUpdate[niko_MPC_ids.SYSTEM_ENERGY_FIELDS_LIST_MEMID] = hashSetOf(MPC_energyFieldInstance(shieldOne, shieldOneOuter), MPC_energyFieldInstance(shieldTwo, shieldTwoOuter), MPC_energyFieldInstance(shieldThree, shieldThreeOuter))
        system.addTerrain("MPC_energyField", RingParams(1f, 1f, hole)).setCircularOrbit(hole, 0f, 0f, 100f)
        MPC_singularityHyperspaceProximityChecker(system).start()

        system.autogenerateHyperspaceJumpPoints(true, true)
        system.star.getJumpPointTo().addTag("MPC_ultimaSingularityJumpPoint") // interactionplugin shenanigans handled in campaignplugin
        system.star.getJumpPointTo().memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] = true
        MPC_abyssUtils.setupSystemForAbyss(system)

        system.addTag(Tags.THEME_SPECIAL)
        system.addTag(Tags.THEME_HIDDEN)
        system.addTag(Tags.THEME_INTERESTING)*/

        MPC_IAIICFobIntel.get()?.progress = 125

        return BaseCommand.CommandResult.SUCCESS
    }

    private fun setupMooredShips(stagingStation: SectorEntityToken) {
        val storage = stagingStation.market.getSubmarket(Submarkets.SUBMARKET_STORAGE).cargo
        storage.addCommodity(Commodities.FUEL, 102f)
        storage.addCommodity(Commodities.SUPPLIES, 20f)
        storage.addCommodity(Commodities.METALS, 981f)
        storage.addCommodity(Commodities.RARE_METALS, 23f)
        storage.addCommodity(Commodities.HEAVY_MACHINERY, 230f)
        storage.addCommodity(Commodities.GAMMA_CORE, 1f)
        storage.addCommodity(Commodities.VOLATILES, 12f)
        storage.addCommodity(Commodities.FOOD, 312f)

        storage.addMothballedShip(FleetMemberType.SHIP, "atlas_Standard", "MBAC Pillar of Dawn")
        storage.addMothballedShip(FleetMemberType.SHIP, "atlas_Standard", "MBAC Ferryfly")

        storage.addMothballedShip(FleetMemberType.SHIP, "eradicator_Outdated", "MBAC Resolute")
        storage.addMothballedShip(FleetMemberType.SHIP, "eradicator_Outdated", "MBAC Resolute")
        storage.addMothballedShip(FleetMemberType.SHIP, "eradicator_Outdated", "MBAC Resolute")

        storage.addMothballedShip(FleetMemberType.SHIP, "buffalo_Standard", "MBAC Jefferson")
        storage.addMothballedShip(FleetMemberType.SHIP, "nebula_Standard", "MBAC Cold As Ice")
        storage.addMothballedShip(FleetMemberType.SHIP, "nebula_Standard", "MBAC Domain Steel")

        storage.initMothballedShips(Factions.NEUTRAL)
        for (member in storage.mothballedShips.membersListCopy) {
            val chance = MathUtils.getRandomNumberInRange(0, 100)
            if (chance > CHANCE_FOR_MOORED_DMODS) continue
            DModManager.addDMods(member, false, MathUtils.getRandomNumberInRange(1, 5), MathUtils.getRandom())
        }

        storage.sort()
    }
}
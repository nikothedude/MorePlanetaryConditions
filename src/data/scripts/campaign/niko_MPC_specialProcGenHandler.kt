package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator
import com.fs.starfarer.api.impl.campaign.terrain.*
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceAbyssPluginImpl.NASCENT_WELL_DETECTED_RANGE
import data.coronaResistStationCoreFleetListener
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript
import data.utilities.*
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils.getApproximateOrbitDays
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.addSalvageEntity
import org.magiclib.kotlin.getPulsarInSystem
import java.awt.Color

object niko_MPC_specialProcGenHandler {

    const val DANGEROUS_TERRAIN_EMITTER_SPAWN_CHANCE = 0.15f // percentage

    fun doSpecialProcgen(checkExisting: Boolean = false) {
        generateExplorationContent()

        //generateMesonFields(checkExisting)
    }

    private fun generateExplorationContent() {
        generateCoronaImmunityStuff()
        generateRandomBaryonEmitters()
        generateMagnetar()
    }

    private fun generateMagnetar() {
        val sysName = "PRS-NM 2231+9"
        val system = Global.getSector().createStarSystem(sysName)
        system.backgroundTextureFilename = "graphics/backgrounds/background_galatia.jpg"
        system.hyperspaceAnchor
        val magnetar = system.initStar("MPC_magnetar", "MPC_star_magnetar", 180f, 700f, 10f, 0.2f, 6f)
        system.lightColor = Color(255, 255, 255)
        val xVariation = MathUtils.getRandomNumberInRange(
            niko_MPC_magnetarStarScript.X_COORD_VARIATION_LOWER_BOUND,
            niko_MPC_magnetarStarScript.X_COORD_VARIATION_UPPER_BOUND
        )
        val yVariation = MathUtils.getRandomNumberInRange(
            niko_MPC_magnetarStarScript.Y_COORD_VARIATION_LOWER_BOUND,
            niko_MPC_magnetarStarScript.Y_COORD_VARIATION_UPPER_BOUND
        )
        val xCoord = niko_MPC_magnetarStarScript.BASE_X_COORD_FOR_SYSTEM + xVariation
        val yCoord = niko_MPC_magnetarStarScript.BASE_Y_COORD_FOR_SYSTEM + yVariation
        system.location.set(xCoord, yCoord)

        val script = niko_MPC_magnetarStarScript(magnetar)
        script.start()

        val renderStartOne = magnetar.radius + 50f
        val renderEndOne = magnetar.radius + 20000f
        val effectMiddleDistOne = 0f
        val effectSizeBothWaysOne = renderEndOne + 4000f
        val paramsOne = MagneticFieldTerrainPlugin.MagneticFieldParams(
            effectSizeBothWaysOne,  // terrain effect band width
            effectMiddleDistOne,  // terrain effect middle radius
            magnetar,  // entity that it's around
            renderStartOne,  // visual band start
            renderEndOne,  // visual band end
            Color(50, 110, 110, 50),  // base color
            1f,  // probability to spawn aurora sequence, checked once/day when no aurora in progress
            Color(50, 20, 110, 130),
            Color(150, 30, 120, 150),
            Color(200, 50, 130, 190),
            Color(250, 70, 150, 240),
            Color(200, 80, 130, 255),
            Color(75, 0, 160),
            Color(127, 0, 255)
        )
        val magnetarField = system.addTerrain("MPC_magnetarField", paramsOne)

        // PLANETS AND EXTERNAL STUFF
        val planetOneInitialAngle = MathUtils.getRandomNumberInRange(0f, 360f)
        val planetOne = system.addPlanet(
            "MPC_magnetarPlanetOne",
            magnetar,
            "$sysName 1",
            Planets.PLANET_LAVA,
            planetOneInitialAngle,
            80f,
            2600f,
            130f
        )
        planetOne.market.addCondition(Conditions.ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.RARE_ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.NO_ATMOSPHERE)
        planetOne.market.addCondition(Conditions.VERY_HOT)
        planetOne.market.addCondition(Conditions.RUINS_SCATTERED)

        for (planet in system.planets) {
            if (planet.isStar) continue
            planet.market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)
            planet.market.addCondition(Conditions.IRRADIATED)
            planet.market.addCondition("niko_MPC_magnetarCondition")
        }

        // TAGS AND STUFF

        system.addTag(Tags.THEME_SPECIAL)
        system.addTag(Tags.THEME_UNSAFE)
        system.addTag(Tags.THEME_HIDDEN)
        system.addTag(Tags.THEME_INTERESTING)

        system.type = StarSystemGenerator.StarSystemType.DEEP_SPACE

        system.autogenerateHyperspaceJumpPoints(true, true)

        for (jumpPoint in system.autogeneratedJumpPointsInHyper) {
            if (jumpPoint.isStarAnchor) {
                jumpPoint.addTag(Tags.STAR_HIDDEN_ON_MAP)
            }
            var range = HyperspaceAbyssPluginImpl.JUMP_POINT_DETECTED_RANGE
            if (jumpPoint.isGasGiantAnchor) {
                range = HyperspaceAbyssPluginImpl.GAS_GIANT_DETECTED_RANGE
            } else if (jumpPoint.isStarAnchor) {
                range = HyperspaceAbyssPluginImpl.STAR_DETECTED_RANGE
            }

            AbyssalRogueStellarObjectEPEC.setAbyssalDetectedRange(jumpPoint, range)
        }
        for (nascentWell in system.autogeneratedNascentWellsInHyper) {
            AbyssalRogueStellarObjectEPEC.setAbyssalDetectedRange(nascentWell, NASCENT_WELL_DETECTED_RANGE)
        }

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] = system
    }

    private fun generateRandomBaryonEmitters(): MutableSet<SectorEntityToken> {
        val objectives = HashSet<SectorEntityToken>()
        if (Global.getSector().memoryWithoutUpdate["\$MPC_generatedBaryonEmitters"] == true) return objectives

        for (iterSystem in Global.getSector().starSystems.shuffled()) {
            if (!iterSystem.isProcgen) continue
            if (iterSystem.hasTag(Tags.THEME_SPECIAL)) continue

            if (MPC_coronaResistScript.interferenceDetected(iterSystem)) continue

            val stableLocations = iterSystem.getEntitiesWithTag(Tags.STABLE_LOCATION)
            if (stableLocations.isEmpty()) continue // need at least one

            var canSpawnEmitter = false
            for (terrain in iterSystem.terrainCopy) {
                val plugin = terrain.plugin
                if (plugin is PulsarBeamTerrainPlugin ||
                    plugin is EventHorizonPlugin ||
                    plugin is HyperspaceTerrainPlugin)
                {
                    canSpawnEmitter = true
                    break
                }
            }
            if (!canSpawnEmitter) continue
            if (MathUtils.getRandom().nextFloat() <= DANGEROUS_TERRAIN_EMITTER_SPAWN_CHANCE) {
                val randLocation = stableLocations.random()
                val objective = staticBuildObjective(randLocation, "MPC_baryonEmitterStandard", Factions.NEUTRAL, true)
                if (objective != null) objectives += objective
            }

        }
        niko_MPC_debugUtils.log.info("generated ${objectives.size} random baryon emitters")
        Global.getSector().memoryWithoutUpdate["\$MPC_generatedBaryonEmitters"] = true
        return objectives
    }

    fun staticBuildObjective(stableLocation: SectorEntityToken, type: String, factionId: String, nonFunctional: Boolean = false): SectorEntityToken? {
        if (stableLocation.hasTag(Tags.NON_CLICKABLE)) return null
        if (stableLocation.hasTag(Tags.FADING_OUT_AND_EXPIRING)) return null
        val loc: LocationAPI = stableLocation.containingLocation
        val built: SectorEntityToken = loc.addCustomEntity(
            null,
            null,
            type,  // type of object, defined in custom_entities.json
            factionId
        ) // faction
        if (nonFunctional) {
            built.memoryWithoutUpdate[MemFlags.OBJECTIVE_NON_FUNCTIONAL] = true
        }
        if (stableLocation.orbit != null) {
            built.orbit = stableLocation.orbit.makeCopy()
        }
        built.setLocation(stableLocation.location.x, stableLocation.location.y)
        loc.removeEntity(stableLocation)
        staticUpdateOrbitingEntities(loc, stableLocation, built)

        //entity.setContainingLocation(null);
        built.memoryWithoutUpdate["\$originalStableLocation"] = stableLocation
        return built
    }

    fun staticUpdateOrbitingEntities(loc: LocationAPI?, prev: SectorEntityToken, built: SectorEntityToken?) {
        if (loc == null) return
        for (other in loc.allEntities) {
            if (other === prev) continue
            if (other.orbit == null) continue
            if (other.orbitFocus === prev) {
                other.orbitFocus = built
            }
        }
    }

    private fun generateCoronaImmunityStuff() {
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_SYSTEM] != null) return

        var starSystem: StarSystemAPI? = null
        for (iterSystem in Global.getSector().starSystems.shuffled()) {
            if (iterSystem.allEntities.any { it.market?.isInhabited() == true }) continue
            if (!iterSystem.isProcgen) continue
            if (iterSystem.secondary != null) continue
            if (iterSystem.hasTag(Tags.THEME_CORE) || iterSystem.hasTag(Tags.THEME_REMNANT) || iterSystem.hasTag(Tags.THEME_SPECIAL)) continue
            iterSystem.getPulsarInSystem() ?: continue
            starSystem = iterSystem
            break
        }
        if (starSystem == null) return

        var pulsarTerrain: PulsarBeamTerrainPlugin? = null
        for (terrain in starSystem.terrainCopy) {
            if (terrain.plugin !is PulsarBeamTerrainPlugin) continue
            pulsarTerrain = terrain.plugin as PulsarBeamTerrainPlugin
            break
        }
        if (pulsarTerrain == null) return
        val pulsar = starSystem.getPulsarInSystem()

        val angle = niko_MPC_reflectionUtils.get("pulsarAngle", pulsarTerrain) as? Float ?: 0f

        val station = DerelictThemeGenerator.addSalvageEntity(starSystem, "MPC_station_corona_resist", Factions.NEUTRAL)
        //station.setDefenderOverride(DefenderDataOverride(Factions.MERCENARY, 1f, 200f, 300f))
        station.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION] = true
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION_GLOBAL] = true

        val orbitRadius = pulsar.radius + 2000f // currently arbitrary
        station.setCircularOrbitPointingDown(pulsar, angle, orbitRadius, pulsarTerrain.getApproximateOrbitDays())
        station.name = "Pristine Research Station" // TODO: do this via the spec (why isnt it doing it already)

        MPC_coronaResistStationScript(station, pulsarTerrain, orbitRadius).start()
        station.addScript(MPC_coronaResistFleetManagerScript(station, 1f, 0, 4, 25f, 3, 20))

        starSystem.tags += Tags.THEME_UNSAFE
        starSystem.tags += Tags.THEME_SPECIAL
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_SYSTEM] = starSystem

        val fleet = genCoronaResistCoreFleet(station)

        fleet.setFaction(Factions.PIRATES)
        fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_DEFENDER] = true
        fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_DEFENDER_CORE] = true
        fleet.addEventListener(coronaResistStationCoreFleetListener())
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOLD_VS_STRONGER] = true

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_HOSTILE] = true

        station.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_STATION_DEFENDER_FLEET] = fleet

        val stationOne = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.STATION_RESEARCH, Factions.NEUTRAL)
        stationOne.setCircularOrbitPointingDown(starSystem.star, 0f, orbitRadius + 4000f, 180f)

        val cacheOne = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.SUPPLY_CACHE, Factions.NEUTRAL)
        val cacheTwo = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.SUPPLY_CACHE, Factions.NEUTRAL)
        val cacheThree = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.SUPPLY_CACHE, Factions.NEUTRAL)
        val cacheFour = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.EQUIPMENT_CACHE, Factions.NEUTRAL)

        cacheOne.setCircularOrbitPointingDown(starSystem.star, 50f, orbitRadius + 3000f, 90f)
        cacheTwo.setCircularOrbitPointingDown(starSystem.star, 110f, orbitRadius + 2000f, 80f)
        cacheThree.setCircularOrbitPointingDown(stationOne, 50f, 2000f, 90f)
        cacheFour.setCircularOrbitPointingDown(starSystem.star, 0f, orbitRadius - 1000f, 60f)

        val jumpPoint = starSystem.jumpPoints.randomOrNull() ?: return
        val miningStationOne = starSystem.addSalvageEntity(MathUtils.getRandom(), Entities.STATION_MINING, Factions.NEUTRAL)
        miningStationOne.setCircularOrbitPointingDown(jumpPoint, 120f, 300f, 20f)

        //station.memoryWithoutUpdate["\$defenderFleet"] = fleet
        //station.memoryWithoutUpdate["\$hasDefenders"] = true
    }

    private fun genCoronaResistCoreFleet(station: SectorEntityToken): CampaignFleetAPI {
        val params = FleetParamsV3(
            station.market,
            station.locationInHyperspace,
            Factions.MERCENARY,
            1f,
            FleetTypes.MERC_PATROL,
            100f,  // combatPts, minus the legion xiv's FP
            4f,  // freighterPts
            4f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            3f,  // utilityPts
            0f // qualityMod
        )
        params.averageSMods = 0
        params.random = StarSystemGenerator.random
        val fleet = FleetFactoryV3.createFleet(params)
        fleet.name = "Skulioda Marauders"

        val newFlagship: FleetMemberAPI = if (niko_MPC_settings.MCTE_loaded && MCTE_settings.PULSAR_EFFECT_ENABLED) {
            fleet.fleetData.addFleetMember("legion_xiv_skulioda")
        } else {
            fleet.fleetData.addFleetMember("legion_xiv_Elite")
        }
        newFlagship.shipName = niko_MPC_ids.SKULIODA_SHIP_NAME
        val commander = genCoronaResistFleetCommander()
        newFlagship.captain = commander
        fleet.commander = commander

        fleet.inflateIfNeeded()
        fleet.inflater = null

        niko_MPC_miscUtils.refreshCoronaDefenderFleetVariables(fleet)

        fleet.fleetData.sort()
        fleet.isNoFactionInName = true

        return fleet
    }

    private fun genCoronaResistFleetCommander(): PersonAPI {
        val person = Global.getFactory().createPerson()
        person.name = FullName("Jensen", "Skulioda", FullName.Gender.MALE)
        person.gender = FullName.Gender.MALE
        val personality = if (niko_MPC_settings.MCTE_loaded && MCTE_settings.PULSAR_EFFECT_ENABLED) {
            Personalities.RECKLESS
        } else {
            Personalities.AGGRESSIVE
        }
        person.setPersonality(personality)

        person.setFaction(Factions.PIRATES)

        val stats = person.stats
        stats.level = 7

        stats.setSkillLevel(Skills.CARRIER_GROUP, 1f)
        stats.setSkillLevel(Skills.FIGHTER_UPLINK, 1f)

        stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2f)
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
        stats.setSkillLevel(Skills.BALLISTIC_MASTERY, 1f)
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 1f)
        stats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2f)

        person.portraitSprite = "graphics/portraits/portrait_mercenary06.png"
        person.memoryWithoutUpdate[niko_MPC_ids.SKULIODA_MEMORY_TAG] = true

        return person
    }
}
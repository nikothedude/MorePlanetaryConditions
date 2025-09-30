package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.listeners.BaseFleetEventListener
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin
import com.fs.starfarer.api.impl.campaign.JumpPointInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.WarningBeaconEntityPlugin
import com.fs.starfarer.api.impl.campaign.enc.AbyssalRogueStellarObjectEPEC
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.procgen.DefenderDataOverride
import com.fs.starfarer.api.impl.campaign.procgen.MagFieldGenPlugin
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.MiscellaneousThemeGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.ShipRecoverySpecialCreator
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager
import com.fs.starfarer.api.impl.campaign.shared.WormholeManager.WormholeItemData
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.EventHorizonPlugin
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import com.fs.starfarer.api.loading.CampaignPingSpec
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.PingScript
import com.fs.starfarer.ui.new
import data.coronaResistStationCoreFleetListener
import data.scripts.campaign.econ.industries.missileLauncher.MPC_remnantMissileCarrierScript
import data.scripts.campaign.magnetar.MPC_magnetarMothershipScript
import data.scripts.campaign.magnetar.MPC_magnetarThreatFleetManager
import data.scripts.campaign.magnetar.niko_MPC_magnetarField
import data.scripts.campaign.magnetar.niko_MPC_magnetarStarScript
import data.scripts.campaign.terrain.niko_MPC_mesonFieldGenPlugin
import data.scripts.everyFrames.niko_MPC_baseNikoScript
import data.utilities.*
import data.utilities.niko_MPC_marketUtils.addConditionIfNotPresent
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils.getApproximateOrbitDays
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.addEntity
import org.magiclib.kotlin.addSalvageEntity
import org.magiclib.kotlin.getMarketsInLocation
import org.magiclib.kotlin.getPulsarInSystem
import org.magiclib.kotlin.hasRuins
import org.magiclib.kotlin.isSpecialMod
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.setDefenderOverride
import org.magiclib.kotlin.setSalvageSpecial
import org.magiclib.util.MagicCampaign
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
        generateFortressSystem()
    }

    private fun generateFortressSystem() {
        if (niko_MPC_settings.FORTRESS_SYSTEM_DISABLED) return
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.FORTRESS_SYSTEM] != null) return

        var candidates = HashSet<StarSystemAPI>()
        var picked: StarSystemAPI? = null
        val allNexi = MiscellaneousThemeGenerator.getRemnantStations(false, false)
        val nexiSystems = allNexi.map { it.containingLocation }

        for (system in nexiSystems) {
            if (system !is StarSystemAPI) continue
            if (system.hasTag(Tags.THEME_SPECIAL) || system.hasTag(Tags.THEME_HIDDEN) || system.getMarketsInLocation().any { it.isInhabited() }) continue
            if (system.hasTag(Tags.SYSTEM_ABYSSAL)) continue

            val planets = system.planets
            if (planets.isEmpty()) continue
            candidates += system
            break
        }
        if (candidates.isEmpty()) return
        val nebulaCandiates = candidates.filter { sys -> sys.hasSystemwideNebula() && sys.planets.filter { !it.isStar }.size > 1f }
        if (nebulaCandiates.isNotEmpty()) candidates = nebulaCandiates.toHashSet()
        picked = candidates.maxBy { it.planets.size }
        var nexus: CampaignFleetAPI = allNexi.find { it.containingLocation == picked }!!

        val hyperspace = Global.getSector().hyperspace
        val beacon = hyperspace.getCustomEntitiesWithTag(Tags.WARNING_BEACON).minBy { MathUtils.getDistance(picked.hyperspaceAnchor, it) }
        //FortressBeaconPingScript(beacon).start()
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.PING_ID_KEY] = "MPC_fortressBeacon"
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.PING_FREQ_KEY] = 2f
        beacon.addTag("MPC_fortressBeacon")

        val fleet = generateMissileCarrierFleet(picked, nexus.location)
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE,
            nexus,
            Float.MAX_VALUE,
            "scanning for targets"
        )
        fleet.addAssignment(
            FleetAssignment.DEFEND_LOCATION,
            picked.planets.random(),
            Float.MAX_VALUE
        )
        fleet.addEventListener(RemnantMissileCarrierListener())
        fleet.memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER] = true
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER_ACTIVE] = true

        val localPlanets = picked.planets.filter { !it.isStar }.toMutableSet()
        val innermostPlanet = localPlanets.minBy { MathUtils.getDistance(picked.center, it) } ?: genInnermostPlanet(picked)
        localPlanets -= innermostPlanet
        setupInnermostPlanet(innermostPlanet)
        var secondInnermostPlanet: PlanetAPI? = null
        if (localPlanets.isNotEmpty()) {
            secondInnermostPlanet = localPlanets.minBy { MathUtils.getDistance(picked.center, it) }!!
            localPlanets -= secondInnermostPlanet
        } else {
            secondInnermostPlanet = genSecondPlanet(picked, innermostPlanet)
        }
        setupSecondPlanet(secondInnermostPlanet)

        val battlestation = picked.addSalvageEntity(
            MathUtils.getRandom(),
            "MPC_abandonedBattlestation",
            Factions.NEUTRAL,
            null
        )
        battlestation.setCircularOrbitPointingDown(
            innermostPlanet,
            MathUtils.getRandomNumberInRange(0f, 360f),
            innermostPlanet.radius + battlestation.radius + 30f,
            -20f * innermostPlanet.radius
        )

        if (!innermostPlanet.market.hasRuins()) {
            innermostPlanet.market.addCondition(Conditions.RUINS_EXTENSIVE)
        }

        val starfortEntity = picked.addSalvageEntity(
            MathUtils.getRandom(),
            "MPC_abandonedStarFort",
            Factions.NEUTRAL,
            null
        )
        starfortEntity.setCircularOrbitPointingDown(
            secondInnermostPlanet,
            MathUtils.getRandomNumberInRange(0f, 360f),
            secondInnermostPlanet.radius + starfortEntity.radius + 30f,
            20f * secondInnermostPlanet.radius
        )

        if (!secondInnermostPlanet.market.hasRuins()) {
            secondInnermostPlanet.market.addCondition(Conditions.RUINS_VAST)
        }

        for (jumpPoint in picked.jumpPoints + picked.planets.filter { it.isGasGiant }) {
            val spotter = picked.addSalvageEntity(
                MathUtils.getRandom(),
                "MPC_missileSpotter",
                Factions.NEUTRAL,
                null
            )

            spotter.setCircularOrbitWithSpin(
                jumpPoint,
                MathUtils.getRandomNumberInRange(0f, 360f),
                jumpPoint.radius + 70f,
                60f,
                -30f,
                30f
            )
        }

        val terrain = picked.terrainCopy
        val stars = picked.planets.filter { it.isStar }
        for (star in stars) {
            if (star.isBlackHole) continue
            if (!terrain.any { it.type == Terrain.MAGNETIC_FIELD && it.orbitFocus == star }) {
                val widthToUse = 1500f
                var visStartRadius = (star.radius * 2f)
                var visEndRadius = visStartRadius + widthToUse
                var bandWidth = (visEndRadius - visStartRadius) * 0.6f
                var midRadius = (visStartRadius + visEndRadius) / 2
                var auroraProbability = 1f
                val auroraIndex =
                    (MagFieldGenPlugin.auroraColors.size * StarSystemGenerator.random.nextDouble()).toInt()

                val ringParams = MagneticFieldTerrainPlugin.MagneticFieldParams(
                    bandWidth, midRadius,
                    star,
                    visStartRadius, visEndRadius,
                    niko_MPC_settings.hyperMagFieldColors.random(),
                    auroraProbability,
                    *MagFieldGenPlugin.auroraColors[auroraIndex],
                )
                val magfield = picked.addTerrain(
                    Terrain.MAGNETIC_FIELD,
                    ringParams
                ) as CampaignTerrainAPI
                magfield.setLocation(star.location.x, star.location.y)
                magfield.setCircularOrbit(star, 0f, 0f, 100f)
            }

            if (!terrain.any { (it.type == Terrain.ASTEROID_BELT || it.type == Terrain.RING) && it.orbitFocus == star }) {
                val ring = picked.addRingBand(
                    star,
                    "misc",
                    "rings_dust0",
                    256f,
                    1,
                    Color.WHITE,
                    256f,
                    star.radius + 7000f,
                    200f,
                    Terrain.RING,
                    null
                )
                ring.setLocation(star.location.x, star.location.y)
                ring.setCircularOrbit(star, 0f, 0f, 200f)
            }
        }

        picked.addTag(Tags.THEME_SPECIAL)

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER] = fleet
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.FORTRESS_SYSTEM] = picked
    }

    private fun setupInnermostPlanet(planet: PlanetAPI) {

        planet.changeType(getInnermostPlanetType(), MathUtils.getRandom())

        val market = planet.market

        val disallowedConds = listOf(
            Conditions.RUINS_SCATTERED, Conditions.RUINS_VAST, Conditions.RUINS_EXTENSIVE, Conditions.RUINS_WIDESPREAD,
            Conditions.VERY_HOT,
            Conditions.COLD, Conditions.VERY_COLD, Conditions.POOR_LIGHT,
            Conditions.ORE_SPARSE, Conditions.ORE_ABUNDANT, Conditions.ORE_MODERATE, Conditions.ORE_ULTRARICH,
            Conditions.VOLATILES_TRACE, Conditions.VOLATILES_PLENTIFUL, Conditions.VOLATILES_DIFFUSE, Conditions.VOLATILES_ABUNDANT,
            Conditions.NO_ATMOSPHERE, Conditions.HABITABLE
        )

        for (condId in disallowedConds) {
            market.removeCondition(condId)
        }

        market.addConditionIfNotPresent(Conditions.ORE_RICH)
        market.addConditionIfNotPresent(Conditions.THIN_ATMOSPHERE)
        market.addConditionIfNotPresent(Conditions.HOT)
        market.addConditionIfNotPresent(Conditions.RUINS_EXTENSIVE)
    }

    private fun genInnermostPlanet(picked: StarSystemAPI): PlanetAPI {
        val star = picked.star

        val newPlanet = picked.addPlanet(
            "MPC_fortressPlanetOneManualSpawn",
            star,
            getInnermostPlanetName(),
            getInnermostPlanetType(),
            MathUtils.getRandomNumberInRange(0f, 360f),
            300f,
            star.radius + 700f,
            90f
        )

        return newPlanet
    }

    private fun getInnermostPlanetName(): String {
        return "Aithern"
    }

    fun getInnermostPlanetType(): String = Planets.BARREN_DESERT

    fun genSecondPlanet(picked: StarSystemAPI, firstPlanet: PlanetAPI): PlanetAPI {
        val star = picked.star
        val dist = MathUtils.getDistance(star, firstPlanet)

        val newPlanet = picked.addPlanet(
            "MPC_fortressPlanetOneManualSpawn",
            star,
            "Hanarat Skies",
            getSecondPlanetType(),
            MathUtils.getRandomNumberInRange(0f, 360f),
            600f,
            dist + 1000f,
            270f
        )

        return newPlanet
    }

    private fun setupSecondPlanet(planet: PlanetAPI) {

        planet.changeType(getSecondPlanetType(), MathUtils.getRandom())

        val market = planet.market

        val disallowedConds = listOf(
            Conditions.RUINS_SCATTERED, Conditions.RUINS_VAST, Conditions.RUINS_EXTENSIVE, Conditions.RUINS_WIDESPREAD,
            Conditions.VERY_HOT, Conditions.HOT,
            Conditions.COLD, Conditions.VERY_COLD, Conditions.POOR_LIGHT,
            Conditions.ORE_SPARSE, Conditions.ORE_ABUNDANT, Conditions.ORE_MODERATE, Conditions.ORE_ULTRARICH, Conditions.ORE_RICH,
            Conditions.RARE_ORE_SPARSE, Conditions.RARE_ORE_RICH, Conditions.RARE_ORE_ULTRARICH, Conditions.RARE_ORE_MODERATE, Conditions.RARE_ORE_ABUNDANT,
            Conditions.VOLATILES_TRACE, Conditions.VOLATILES_PLENTIFUL, Conditions.VOLATILES_DIFFUSE, Conditions.VOLATILES_ABUNDANT,
            Conditions.NO_ATMOSPHERE, Conditions.THIN_ATMOSPHERE,
            Conditions.TECTONIC_ACTIVITY, Conditions.EXTREME_TECTONIC_ACTIVITY,
            Conditions.TOXIC_ATMOSPHERE,
            Conditions.IRRADIATED,
            Conditions.ORGANICS_TRACE, Conditions.ORGANICS_COMMON, Conditions.ORGANICS_ABUNDANT, Conditions.ORGANICS_PLENTIFUL,
            Conditions.FARMLAND_POOR, Conditions.FARMLAND_RICH, Conditions.FARMLAND_ADEQUATE, Conditions.FARMLAND_BOUNTIFUL
        )

        for (condId in disallowedConds) {
            market.removeCondition(condId)
        }

        market.addConditionIfNotPresent(Conditions.RUINS_VAST)

        market.addConditionIfNotPresent(Conditions.ORE_SPARSE)
        market.addConditionIfNotPresent(Conditions.RARE_ORE_SPARSE)
        market.addConditionIfNotPresent(Conditions.ORGANICS_COMMON)
        market.addConditionIfNotPresent(Conditions.HABITABLE)
        market.addConditionIfNotPresent(Conditions.FARMLAND_ADEQUATE)

        market.addConditionIfNotPresent("niko_MPC_carnivorousFauna")
    }

    private fun getSecondPlanetType(): String = Planets.PLANET_TERRAN

    fun generateMissileCarrierFleet(containingLoc: LocationAPI, loc: Vector2f): CampaignFleetAPI {
        val FP = 325f

        val params = FleetParamsV3(
            null,
            Factions.REMNANTS,
            1f,
            FleetTypes.PATROL_LARGE,
            FP,  // combatPts
            0f,  // freighterPts
            0f,  // tankerPts
            0f,  // transportPts
            0f,  // linerPts
            0f,  // utilityPts
            0f
        )
        params.averageSMods = 3
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED

        val fleet = FleetFactoryV3.createFleet(params)
        fleet.name = "Carrier Group"

        containingLoc.addEntity(fleet)
        fleet.setLocation(loc.x, loc.y)

        fleet.inflateIfNeeded()
        fleet.inflater = null

        val member = fleet.fleetData.addFleetMember("MPC_lockbow_Ordnance")
        val newVariant = member.variant.clone()
        newVariant.hullVariantId = null
        newVariant.source = VariantSource.REFIT
        newVariant.originalVariant = null
        member.setVariant(newVariant, false, false)
        newVariant.originalVariant = null
        newVariant.addPermaMod(HullMods.AUTOMATED)
        newVariant.addPermaMod(HullMods.AUXILIARY_THRUSTERS, true)
        newVariant.addPermaMod(HullMods.REINFORCEDHULL, true)
        newVariant.addPermaMod(HullMods.TURRETGYROS, true)
        newVariant.addTag(Tags.VARIANT_ALWAYS_RECOVERABLE)

        for (member in fleet.fleetData.membersListCopy) {
            member.variant.originalVariant = null
        }

        fleet.fleetData.sort()
        newVariant.originalVariant = null

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_NO_JUMP] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE] = true

        fleet.makeImportant("MPC_remnantMissileCarrier")

        fleet.addTag("MPC_remnantMissileCarrierFleet")

        MPC_remnantMissileCarrierScript(fleet).start()

        member.captain = AICoreOfficerPluginImpl().createPerson(Commodities.ALPHA_CORE, Factions.REMNANTS, MathUtils.getRandom())
        val capStats = member.captain.stats

        capStats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2f)
        capStats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f)
        capStats.setSkillLevel(Skills.HELMSMANSHIP, 2f)
        capStats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f)
        capStats.setSkillLevel(Skills.POINT_DEFENSE, 2f)
        capStats.setSkillLevel(Skills.BALLISTIC_MASTERY, 2f)
        capStats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)

        fleet.commander = member.captain
        val commander = fleet.commander
        val comStats = commander.stats
        comStats.setSkillLevel(Skills.CREW_TRAINING, 2f)
        comStats.setSkillLevel(Skills.ELECTRONIC_WARFARE, 2f)
        comStats.setSkillLevel(Skills.CYBERNETIC_AUGMENTATION, 2f)
        comStats.setSkillLevel(Skills.TACTICAL_DRILLS, 2f)

        member.repairTracker.cr = member.repairTracker.maxCR

        fleet.fleetData.setSyncNeeded()
        fleet.fleetData.syncIfNeeded()

        fleet.isDiscoverable = true
        fleet.discoveryXP = 100f

        return fleet
    }

    class RemnantMissileCarrierListener: BaseFleetEventListener() {

        override fun reportBattleOccurred(
            fleet: CampaignFleetAPI?,
            primaryWinner: CampaignFleetAPI?,
            battle: BattleAPI?
        ) {
            super.reportBattleOccurred(fleet, primaryWinner, battle)

            if (fleet == null) return
            if (!fleet.fleetData.membersListCopy.any { it.hullId == "MPC_lockbow" }) {
                Global.getSector().memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER_ACTIVE] = false
                Global.getSector().memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER] = null
                fleet.memoryWithoutUpdate.unset(niko_MPC_ids.REMNANT_MISSILE_CARRIER)
            }
        }

        override fun reportFleetDespawnedToListener(
            fleet: CampaignFleetAPI?,
            reason: CampaignEventListener.FleetDespawnReason?,
            param: Any?
        ) {
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.REMNANT_MISSILE_CARRIER_ACTIVE] = false
            fleet!!.memoryWithoutUpdate.unset(niko_MPC_ids.REMNANT_MISSILE_CARRIER)
        }

    }

    private fun generateMagnetar() {
        if (niko_MPC_settings.MAGNETAR_DISABLED) return
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] != null) return

        val sysName = "Perseus NM 2231+9CB"
        val system = Global.getSector().createStarSystem(sysName)
        system.backgroundTextureFilename = "graphics/backgrounds/background_galatia.jpg"
        system.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "MPC_magnetarAmbience"
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
        magnetar.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_STAR_SCRIPT_MEMID] = script
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
        val magnetarField = system.addTerrain("MPC_magnetarField", paramsOne) as CampaignTerrainAPI

        // PLANETS AND EXTERNAL STUFF
        val planetOneInitialAngle = MathUtils.getRandomNumberInRange(0f, 360f)
        val planetOne = system.addPlanet(
            "MPC_magnetarPlanetOne",
            magnetar,
            "$sysName 1",
            Planets.PLANET_LAVA,
            planetOneInitialAngle,
            70f,
            2600f,
            130f
        )
        planetOne.market.addCondition(Conditions.ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.RARE_ORE_ULTRARICH)
        planetOne.market.addCondition(Conditions.NO_ATMOSPHERE)
        planetOne.market.addCondition(Conditions.VERY_HOT)
        planetOne.market.addCondition(Conditions.RUINS_SCATTERED)

        val planetTwo = system.addPlanet(
            "MPC_magnetarGasGiantOne",
            magnetar,
            "$sysName G1",
            Planets.GAS_GIANT,
            MathUtils.getRandomNumberInRange(0f, 360f),
            430f,
            15000f,
            612f,
        )
        planetTwo.market.addCondition(Conditions.POOR_LIGHT)
        planetTwo.market.addCondition(Conditions.HIGH_GRAVITY)
        planetTwo.market.addCondition(Conditions.VERY_COLD)
        planetTwo.market.addCondition(Conditions.DENSE_ATMOSPHERE)
        planetTwo.market.addCondition(Conditions.VOLATILES_PLENTIFUL)
        planetTwo.market.addCondition(Conditions.RUINS_SCATTERED)
        planetTwo.descriptionIdOverride = "MPC_gasGiantIntrastellarCapture"

        val planetThree = system.addPlanet(
            "MPC_magnetarSystemPlanetTwo",
            magnetar,
            "$sysName 2",
            Planets.PLANET_LAVA_MINOR,
            Misc.normalizeAngle(planetOneInitialAngle - 50f),
            90f,
            6078f,
            100f
        )
        planetThree.market.addCondition(Conditions.VERY_HOT)
        planetThree.market.addCondition(Conditions.ORE_ULTRARICH)
        planetThree.market.addCondition(Conditions.RARE_ORE_RICH)
        planetThree.market.addCondition(Conditions.NO_ATMOSPHERE)
        planetThree.market.addCondition(Conditions.RUINS_SCATTERED)

        val mesonFieldOne = system.addTerrain(
            "MPC_mesonField",
            niko_MPC_mesonFieldGenPlugin.generateDefaultParams(planetThree)
        )
        mesonFieldOne.setCircularOrbit(planetThree, 0f, 0f, 100f)

        val planetFour = system.addPlanet(
            "MPC_magnetarSystemPlanetThree",
            magnetar,
            "$sysName 3",
            Planets.IRRADIATED,
            MathUtils.getRandomNumberInRange(0f, 360f),
            110f,
            9300f,
            365f
        )
        planetFour.market.addCondition(Conditions.IRRADIATED)
        planetFour.market.addCondition(Conditions.NO_ATMOSPHERE)
        planetFour.market.addCondition(Conditions.ORE_SPARSE)
        planetFour.market.addCondition(Conditions.RUINS_EXTENSIVE)

        planetFour.setDefenderOverride(DefenderDataOverride( // i manually do this in the magnetar star script
            niko_MPC_ids.OMEGA_DERELICT_FACTION_ID,
            100f,
            2f,
            2f
        ))

        val ringCenter = 5250f
        system.addRingBand(
            magnetar,
            "misc",
            "rings_dust0",
            312f,
            1,
            Color.white,
            312f,
            ringCenter,
            100f,
            Terrain.RING,
            null
        )


        /*val planetFive = system.addPlanet(
            "MPC_magnetarSystemPlanetFour",
            magnetar,
            "$sysName 4",
            Planets.BARREN_BOMBARDED,
            Misc.normalizeAngle(planetOneInitialAngle - 50f),
            90f,
            4600f,
            100f
        )*/

        val magnetarPlugin = magnetarField.plugin as niko_MPC_magnetarField
        for (planet in system.planets) {
            if (planet.isStar) continue
            if (!magnetarPlugin.containsEntity(planet)) continue
            if (!planet.isGasGiant) planet.market.addCondition(Conditions.EXTREME_TECTONIC_ACTIVITY)
            planet.market.addCondition(Conditions.IRRADIATED)
            planet.market.addCondition("niko_MPC_magnetarCondition")
        }
        system.memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_FIELD_MEMID] = magnetarPlugin

        // WORMHOLE

        system.addCustomEntity(null, null, Entities.STABLE_LOCATION, Factions.NEUTRAL).setCircularOrbitPointingDown(magnetar, 280f, 20000f, 720f)
        val sacrificialStableLocation = system.getEntitiesWithTag(Tags.STABLE_LOCATION).first()
        val itemData = WormholeItemData("MPC_magnetarWormhole", "MPC_hotel", "Hotel")
        val item = SpecialItemData(Items.WORMHOLE_ANCHOR, itemData.toJsonStr())
        val wormholeOne = WormholeManager.get().addWormhole(item, sacrificialStableLocation, null)
        wormholeOne.memoryWithoutUpdate[JumpPointInteractionDialogPluginImpl.UNSTABLE_KEY] = false

        val scavDebris = MagicCampaign.createDebrisField(
            "MPC_scavVictimDebrisField",
            350f,
            1.5f,
            Float.MAX_VALUE,
            0f,
            750,
            0f,
            null,
            0,
            0.5f,
            true,
            250,
            wormholeOne,
            30f,
            300f,
            Float.MAX_VALUE // barely moves
        )
        scavDebris.addTag(niko_MPC_ids.IMMUNE_TO_OMEGA_CLEARING)
        scavDebris.addTag("MPC_scavVictimDebrisField")
        val derelictApogee = MagicCampaign.createDerelict(
            "apogee_Balanced",
            ShipRecoverySpecial.ShipCondition.WRECKED,
            true,
            200,
            true,
            scavDebris,
            20f,
            50f,
            Float.MAX_VALUE,
            true
        )

        MagicCampaign.addSalvage(
            null,
            scavDebris,
            MagicCampaign.lootType.WEAPON,
            "shockrepeater", // a premonition of what you may face
            1
        )
        /*

        val scavDebrisParams = DebrisFieldParams(

        )
        scavDebrisParams.source = DebrisFieldTerrainPlugin.DebrisFieldSource.BATTLE
        scavDebrisParams.baseSalvageXP = 500
        scavDebrisParams.density =
        val scavVictimDebrisField = system.addDebrisField(scavDebrisParams, MathUtils.getRandom())
        scavVictimDebrisField.isDiscoverable = true
        scavVictimDebrisField.sensorProfile =*/

        system.addCustomEntity("MPC_magnetarWormholeProbe", null, "MPC_magnetarWormholeProbe", Factions.NEUTRAL, null).setCircularOrbitPointingDown(wormholeOne, 0f, 350f, 30f)

        // SHIELDS
        system.addCustomEntity("MPC_magnetarShieldOne", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 3493f, 90f)
        system.addCustomEntity("MPC_magnetarShieldTwo", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 5762f, 90f)
        system.addCustomEntity("MPC_magnetarShieldThree", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 4197f, -90f)
        system.addCustomEntity("MPC_magnetarShieldFour", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 2400f, 90f)
        system.addCustomEntity("MPC_magnetarShieldFive", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 7420f, -90f)
        system.addCustomEntity("MPC_magnetarShieldSix", null, "MPC_magnetarShield", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), 2900f, -90f)
        system.addCustomEntity("MPC_magnetarShield_hijacked", null, "MPC_magnetarShield_hijacked", Factions.NEUTRAL, null).setCircularOrbitPointingDown(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), ringCenter, -90f)

        // THE TOTEM - i agreed that if hal solod magnetar with an afflictor id add a totem for his hubris. and. he
        // soloed it with like 500 stacks of modded nonsense but a prOMISE IS A PROMISE
        val perShipData = ShipRecoverySpecial.PerShipData("afflictor_Strike", ShipRecoverySpecial.ShipCondition.WRECKED)
        perShipData.sModProb = 100f
        perShipData.shipName = "Hal's Hubris"
        val totemParams = DerelictShipEntityPlugin.DerelictShipData(
            perShipData,
            false
        )
        val totem = BaseThemeGenerator.addSalvageEntity(
            system,
            Entities.WRECK,
            Factions.NEUTRAL,
            totemParams
        )
        val creator = ShipRecoverySpecialCreator(null, 0, 0, false, null, null)
        totem.setSalvageSpecial(creator)
        totem.tags += "MPC_totemToHalHubris"
        totem.isDiscoverable = true
        totem.discoveryXP = 50f
        totem.setCircularOrbitWithSpin(magnetar, MathUtils.getRandomNumberInRange(0f, 360f), ringCenter, 270f, 5f, -5f)

        // OMEGA CACHES
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_magnetarOmegaCache", Factions.NEUTRAL), 1.9f).setCircularOrbitWithSpin(magnetar, 250f, 1208f, 365f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_magnetarOmegaCache", Factions.NEUTRAL), 1.9f).setCircularOrbitWithSpin(magnetar, 310f, 985f, 342f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_magnetarOmegaCache", Factions.NEUTRAL), 2.2f).setCircularOrbitWithSpin(magnetar, 20f, 2400f, 342f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_magnetarOmegaCache", Factions.NEUTRAL), 2.2f).setCircularOrbitWithSpin(magnetar, 220f, 2200f, 342f, -10f, 10f)

        // WEAPON CACHES / PROBES
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 1.3f).setCircularOrbitWithSpin(magnetar, 20f, 5000f, 500f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_weapons_cache", Factions.NEUTRAL), 0.8f).setCircularOrbitWithSpin(magnetar, 250f, 6100f, -400f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 0.9f).setCircularOrbitWithSpin(magnetar, 110f, 3200f, -360f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 1.3f).setCircularOrbitWithSpin(magnetar, 10f, 6200f, -200f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_weapons_cache", Factions.NEUTRAL), 1.1f).setCircularOrbitWithSpin(magnetar, 90f, 6700f, 200f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 1.1f).setCircularOrbitWithSpin(magnetar, 40f, 3400f, -200f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 1.1f).setCircularOrbitWithSpin(magnetar, 100f, 4300f, -200f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_weapons_cache", Factions.NEUTRAL), 1.1f).setCircularOrbitWithSpin(magnetar, 330f, 4700f, 200f, -10f, 10f)

        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_probe", Factions.NEUTRAL), 0.7f).setCircularOrbitWithSpin(planetTwo, 100f, planetTwo.radius + 300f, 50f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_weapons_cache_small", Factions.NEUTRAL), 0.7f).setCircularOrbitWithSpin(planetOne, 100f, planetOne.radius + 400f, 50f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_weapons_cache_small", Factions.NEUTRAL), 0.7f).setCircularOrbitWithSpin(planetFour, 100f, planetFour.radius + 300f, 50f, -10f, 10f)

        // EQUIPMENT CACHES
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_equipment_cache", Factions.NEUTRAL), 1.6f).setCircularOrbitWithSpin(magnetar, 20f, 2100f, 120f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_equipment_cache", Factions.NEUTRAL), 4f).setCircularOrbitWithSpin(magnetar, 210f, 10000f, 500f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_equipment_cache", Factions.NEUTRAL), 4f).setCircularOrbitWithSpin(magnetar, 30f, 11000f, 500f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_equipment_cache_small", Factions.NEUTRAL), 1.2f).setCircularOrbitWithSpin(planetThree, 20f, planetThree.radius + 600f, -90f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_corrupted_equipment_cache_small", Factions.NEUTRAL), 1.2f).setCircularOrbitWithSpin(planetFour, 30f, planetFour.radius + 900f, 90f, -10f, 10f)

        // SURVEY SHIPS
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_survey_ship", Factions.NEUTRAL), 1.6f).setCircularOrbitWithSpin(magnetar, 116f, 3900f, 120f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_survey_ship", Factions.NEUTRAL), 1.6f).setCircularOrbitWithSpin(planetOne, 3f, 2000f, 30f, -10f, 10f)
        makeEntityHackable(system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_survey_ship", Factions.NEUTRAL), 1.6f).setCircularOrbitWithSpin(magnetar, 90f, 5900f, 300f, -10f, 10f)

        // THE RESEARCH STATION - THE DOMAINS ATTEMPT TO HARNESS ITS POWER
        val researchStation = system.addSalvageEntity(MathUtils.getRandom(), "MPC_station_researchMagnetarOne", Factions.NEUTRAL)
        researchStation.setCircularOrbitPointingDown(magnetar, 30f, magnetar.radius + 40f, 20f)
        makeEntityHackable(researchStation, niko_MPC_magnetarStarScript.MIN_DAYS_PER_PULSE * 0.4f)

        // THE MOTHERSHIP - THE SOURCE
        val mothership = system.addSalvageEntity(MathUtils.getRandom(), "MPC_omegaDerelict_mothership", Factions.NEUTRAL)
        mothership.setCircularOrbitWithSpin(magnetar, 20f, 700f, 50f, -10f, 10f)
        mothership.addScript(MPC_magnetarMothershipScript(mothership, 1f, 11, 11, 20f, 80, 100)) // VERY THREATENING

        val dispersalUnitOne = system.addCustomEntity("MPC_magnetarDispersalUnitOne", null, "MPC_magnetarSensorScrambler", Factions.NEUTRAL)
        val dispersalUnitTwo = system.addCustomEntity("MPC_magnetarDispersalUnitTwo", null, "MPC_magnetarSensorScrambler", Factions.NEUTRAL)
        val dispersalUnitThree = system.addCustomEntity("MPC_magnetarDispersalUnitThree", null, "MPC_magnetarSensorScrambler", Factions.NEUTRAL)

        val dispersalDist = MathUtils.getDistance(magnetar.location, wormholeOne.location) + 1000f
        val dispersalOrbitDur = 700f

        dispersalUnitOne.setCircularOrbitPointingDown(magnetar, 120f, dispersalDist, dispersalOrbitDur)
        dispersalUnitTwo.setCircularOrbitPointingDown(magnetar, 240f, dispersalDist, dispersalOrbitDur)
        dispersalUnitThree.setCircularOrbitPointingDown(magnetar, 0f, dispersalDist, dispersalOrbitDur)

        /*mothership.memoryWithoutUpdate["\$defenderFleet"] = createOmegaMothershipDefenders()
        mothership.memoryWithoutUpdate["\$hasDefenders"] = true
        mothership.memoryWithoutUpdate["\$hasStation"] = true
        mothership.memoryWithoutUpdate["\$hasNonStation"] = true*/

        //SALVAGE_SPEC_ID_OVERRIDE
        //SALVAGE_SPEC_ID_OVERRIDE
        //SALVAGE_SPEC_ID_OVERRIDE
        //SALVAGE_SPEC_ID_OVERRIDE
        // FUCKING USE THIS PLEEEEEEEEEEEEEEEEEASE

        // TAGS AND STUFF

        system.addTag(Tags.THEME_SPECIAL)
        system.addTag(Tags.THEME_UNSAFE)
        system.addTag(Tags.THEME_HIDDEN)
        system.addTag(Tags.THEME_INTERESTING)
        system.addTag("theme_magnetar")
        system.addTag(Tags.SYSTEM_ABYSSAL)
        //system.addTag(Tags.SYSTEM_CAN_SPAWN_THREAT)
        //system.addTag(Tags.NOT_RANDOM_MISSION_TARGET)

        system.type = StarSystemGenerator.StarSystemType.DEEP_SPACE

        system.autogenerateHyperspaceJumpPoints(true, true)

        MPC_abyssUtils.setupSystemForAbyss(system)

        system.addScript(MPC_magnetarThreatFleetManager())

        val anchor = system.hyperspaceAnchor
        val beacon = Global.getSector().hyperspace.addCustomEntity(
            "MPC_magnetarBeacon",
            "Scrambled Beacon",
            "MPC_magnetarBeacon",
            Factions.NEUTRAL
        )
        beacon.setCircularOrbitPointingDown(anchor, 100f, 300f, 65f)
        val glowColor = Color(0, 250, 147, 255)
        val pingColor = Color(0, 250, 147, 255)
        Misc.setWarningBeaconColors(beacon, glowColor, pingColor)
        AbyssalRogueStellarObjectEPEC.setAbyssalDetectedRange(beacon, 2800f) // its a beacon, of course you can see it

        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.MAGNETAR_SYSTEM] = system
    }

    class FortressBeaconPingScript(val beacon: SectorEntityToken): niko_MPC_baseNikoScript() {

        val interval = IntervalUtil(3f, 3f) // seconds

        override fun startImpl() {
            beacon.addScript(this)
        }

        override fun stopImpl() {
            beacon.removeScript(this)
        }

        override fun runWhilePaused(): Boolean = false

        override fun advance(amount: Float) {
            if (beacon.isExpired) {
                delete()
                return
            }
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                firePing()
            }
        }

        private fun firePing() {
            val custom = CampaignPingSpec()
            custom.width = 15f
            custom.range = 500f
            custom.duration = 0.5f
            custom.alphaMult = 1f
            custom.inFraction = 0.1f
            custom.num = 5
            custom.delay = 0.3f
            custom.color = Misc.getNegativeHighlightColor()
            //custom.sounds.add("default_campaign_ping")
            Global.getSector().addPing(beacon, custom)
            Global.getSoundPlayer().playSound("default_campaign_ping", 1f, 5f, beacon.location, Misc.ZERO)
        }
    }

    /*private fun createOmegaMothershipDefenders(): CampaignFleetAPI {
        val fleetPoints = 300f
        val defenderFleet = niko_MPC_derelictOmegaFleetConstructor.setupFleet(niko_MPC_derelictOmegaFleetConstructor.createFleet(fleetPoints, null, 100f))
        val mothership = defenderFleet.fleetData.addFleetMember("MPC_omega_derelict_mothership_Standard")

        mothership.captain = AICoreOfficerPluginImpl().createPerson(Commodities.OMEGA_CORE, niko_MPC_ids.OMEGA_DERELICT_FACTION_ID, MathUtils.getRandom())
        defenderFleet.fleetData.sort()

        return defenderFleet
    }*/

    private fun makeEntityHackable(entity: SectorEntityToken, hackDaysNeeded: Float): SectorEntityToken {
        entity.memoryWithoutUpdate["\$MPC_hackable"] = true
        entity.memoryWithoutUpdate["\$MPC_hackDurationNeeded"] = hackDaysNeeded

        return entity
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

    fun genCoronaResistFleetCommander(): PersonAPI {
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

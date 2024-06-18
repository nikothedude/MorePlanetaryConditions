package data.scripts.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.themes.DerelictThemeGenerator
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin
import data.coronaResistStationCoreFleetListener
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_miscUtils
import data.utilities.niko_MPC_miscUtils.getApproximateOrbitDays
import data.utilities.niko_MPC_reflectionUtils
import data.utilities.niko_MPC_settings
import niko.MCTE.settings.MCTE_settings
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.addSalvageEntity
import org.magiclib.kotlin.getPulsarInSystem

object niko_MPC_specialProcGenHandler {

    fun doSpecialProcgen(checkExisting: Boolean = false) {
        generateExplorationContent()

        //generateMesonFields(checkExisting)
    }

    private fun generateExplorationContent() {
        generateCoronaImmunityStuff()
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
            70f,  // combatPts, minus the legion xiv's FP
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
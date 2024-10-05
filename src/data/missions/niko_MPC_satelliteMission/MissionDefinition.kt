package data.missions.niko_MPC_satelliteMission

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.fleet.FleetGoal
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.mission.MissionDefinitionAPI
import com.fs.starfarer.api.mission.MissionDefinitionPlugin
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin
import data.scripts.campaign.niko_MPC_specialProcGenHandler

class MissionDefinition : MissionDefinitionPlugin {
    override fun defineMission(api: MissionDefinitionAPI) {
        api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false)
        api.initFleet(FleetSide.ENEMY, null, FleetGoal.ATTACK, true)

        api.setFleetTagline(FleetSide.PLAYER, "An unlucky yet resourceful salvage fleet")
        api.setFleetTagline(FleetSide.ENEMY, "Skulioda Marauders")

        api.addBriefingItem("The enemy defense satellites will provide long-ranged fire support")
        api.addBriefingItem("Your civilian ships can be militarized")
        api.addBriefingItem("Your ships have superior maneuverability - divide and conquer")
        api.addBriefingItem("The enemy captain is extremely competent - but so are you")

        api.addToFleet(FleetSide.ENEMY, "niko_MPC_defenseSatelliteCore_support", FleetMemberType.SHIP, "DSS Hbaye-Mari", false)
        api.addToFleet(FleetSide.ENEMY, "niko_MPC_defenseSatelliteCore_support", FleetMemberType.SHIP,  "DSS KHG-KH",false)

        val skuliodasFlagship = api.addToFleet(FleetSide.ENEMY, "legion_xiv_Elite", FleetMemberType.SHIP, true)
        val skulioda = niko_MPC_specialProcGenHandler.genCoronaResistFleetCommander()
        skuliodasFlagship.captain = skulioda
        skuliodasFlagship.variant.addPermaMod(HullMods.HEAVYARMOR, true)
        skuliodasFlagship.variant.addPermaMod(HullMods.EXPANDED_DECK_CREW, true)
        api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "mora_Support", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "mora_Torpedo", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "shrike_p_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "venture_p_Pirate", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "eradicator_pirates_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "colossus3_Pirate", FleetMemberType.SHIP, false)

        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.ENEMY, "lasher_Standard", FleetMemberType.SHIP, false)


        val plugin = Global.getSettings().getPlugin("officerLevelUp") as OfficerLevelupPlugin

        // (64 (54 w/o skills) DP)

        // (64 (54 w/o skills) DP)
        val playerFlagship = api.addToFleet(FleetSide.PLAYER, "harbinger_Strike", FleetMemberType.SHIP, true)
        val indies = Global.getSettings().createBaseFaction(Factions.INDEPENDENT)

        val officer = indies.createRandomPerson(FullName.Gender.FEMALE)
        officer.stats.setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2f)
        officer.stats.setSkillLevel(Skills.FIELD_MODULATION, 2f)
        officer.stats.setSkillLevel(Skills.HELMSMANSHIP, 2f)
        officer.stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f)
        officer.stats.setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2f)
        officer.stats.addXP(plugin.getXPForLevel(5))
        officer.stats.level = 5
        officer.name.first = "Irimitsu"
        officer.name.last = "Koritu"
        officer.portraitSprite = "graphics/portraits/portrait28.png"
        officer.setFaction(Factions.INDEPENDENT)
        playerFlagship.captain = officer

        api.addToFleet(FleetSide.PLAYER, "aurora_Strike", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "apogee_Balanced", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "venture_Exploration", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "venture_Exploration", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "heron_Strike", FleetMemberType.SHIP, false)
        //api.addToFleet(FleetSide.PLAYER, "aurora_Strike", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "medusa_Attack", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "omen_PD", FleetMemberType.SHIP, false)

        api.addToFleet(FleetSide.PLAYER, "buffalo_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "buffalo_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "buffalo_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "colossus_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "colossus_Standard", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "shepherd_Frontier", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "shepherd_Frontier", FleetMemberType.SHIP, false)
        api.addToFleet(FleetSide.PLAYER, "phaeton_Standard", FleetMemberType.SHIP, false)



        // Set up the map.
        val width = 17000f
        val height = 17000f
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f)
        val minX = -width / 2
        val minY = -height / 2

        // Add an asteroid field
        api.addAsteroidField(
            minX, minY + height / 2, 0f, 8000f,
            20f, 70f, 100
        )

        api.addObjective(minX + width * 0.25f, minY + height * 0.5f, "sensor_array")
        api.addObjective(minX + width * 0.5f, minY + height * 0.25f, "nav_buoy")
        api.addObjective(minX + width * 0.75f, minY + height * 0.75f, "comm_relay")

        api.addPlanet(0f, 0f, 50f, StarTypes.RED_GIANT, 250f, true)
        api.addPlanet(500f, -5000f, 75f, Planets.PLANET_LAVA, 75f, true)
    }
}
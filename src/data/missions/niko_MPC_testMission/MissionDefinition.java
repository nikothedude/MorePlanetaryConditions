package data.missions.niko_MPC_testMission;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
    @Override
    public void defineMission(MissionDefinitionAPI api) {

        api.initFleet(FleetSide.PLAYER, "ISS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "ISS", FleetGoal.ATTACK, true);

        api.addToFleet(FleetSide.PLAYER, "niko_MPC_defenseSatelliteCore_swarm", FleetMemberType.SHIP, null, false);
        api.addToFleet(FleetSide.PLAYER, "tempest_Attack", FleetMemberType.SHIP, "Oogik", false);
        api.addToFleet(FleetSide.ENEMY, "niko_MPC_defenseSatelliteCore_swarm", FleetMemberType.SHIP, null, false);

        // Set up the map.
        float width = 6000f;
        float height = 6000f;
        api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);

        float minX = -width/2;
        float minY = -height/2;

        // Add an asteroid field
        api.addAsteroidField(minX, minY + height / 2, 0, 8000f,
                20f, 70f, 100);

        api.addPlanet(0, 0, 50f, StarTypes.RED_GIANT, 250f, true);
    }
}

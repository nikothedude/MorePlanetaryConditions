package data.scripts.everyFrames.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static data.utilities.niko_MPC_ids.niko_MPC_isSatelliteHullId;

public class niko_MPC_newSatelliteDeployerScript extends BaseEveryFrameCombatPlugin {

    @Override
    public void init(CombatEngineAPI engine) {
        super.init(engine);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT) return;

        super.advance(amount, events);

        CombatEngineAPI engine = Global.getCombatEngine();

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        BattleAPI battle = playerFleet.getBattle();

        deploySatellitesForSide(FleetSide.PLAYER, engine);
        deploySatellitesForSide(FleetSide.ENEMY, engine);

        prepareForGarbageCollection();
        engine.removePlugin(this);
    }

    private void prepareForGarbageCollection() {
        return;
    }

    private void deploySatellitesForSide(FleetSide side, CombatEngineAPI engine) {
        List<FleetMemberAPI> satellites = new ArrayList<>();

        CombatFleetManagerAPI fleetManager = engine.getFleetManager(side);
        List<FleetMemberAPI> reserves = fleetManager.getReservesCopy();
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : ships) {
            if (ship.getHullSpec().hasTag(niko_MPC_isSatelliteHullId)) {
                satellites.add(ship.getFleetMember());
            }
        }
        for (FleetMemberAPI fleetMember : reserves) {
            if (fleetMember.getHullSpec().hasTag(niko_MPC_isSatelliteHullId)) {
                satellites.add(fleetMember);
            }
        }
        forceDeploySatellites(engine, fleetManager, side, satellites);
    }

    private void forceDeploySatellites(CombatEngineAPI engine, CombatFleetManagerAPI fleetManager, FleetSide side, List<FleetMemberAPI> satellites) {
        int size = satellites.size();
        if (size == 0) return;

        float mapWidth = engine.getMapWidth();
        float mapHeight = engine.getMapHeight();

        float initialXCoord = 0;
        if (size > 1) {
            initialXCoord = (float) (mapWidth*-0.45); // if we have more than one, we set it to be at the very edge of the map
        }
        float xOffset = 0;
        if (size > 1) {
            xOffset = (initialXCoord * -2) / (size - 1); //ex. if we start at 400, 2 satellites causes it to be 200, then 100, 50, etc
        }
        float xThresholdForWrapping = mapWidth;

        float initialYCoord = (float) (mapHeight*-0.35);
        float yOffset = 200f;

        float facing = 90f;

        if (side == FleetSide.ENEMY) {
            initialYCoord *= -1f;
            yOffset *= -1f;
            facing = 270f;
        }

        float yCoord = initialYCoord;
        float xCoord = initialXCoord;

        for (FleetMemberAPI satellite : satellites) {
            if (xCoord > xThresholdForWrapping) {
                xCoord = initialXCoord;
                yCoord += yOffset;
            }
            deploySatellite(satellite, new Vector2f(xCoord, yCoord), facing, fleetManager, side);
            xCoord += xOffset;
        }
    }

    private void deploySatellite(FleetMemberAPI satelliteFleetMember, Vector2f vector2f, float facing, CombatFleetManagerAPI fleetManager, FleetSide side) {
        ShipAPI satellite = fleetManager.spawnFleetMember(satelliteFleetMember, vector2f, facing, 0);
        satellite.getHullSpec().addTag("no_combat_chatter");
        int owner = 1;
        if (side == FleetSide.PLAYER) {
            owner = 0;
            satellite.setAlly(true);
        }
        String name = Global.getSector().getFaction(Factions.DERELICT).pickRandomShipName();
        satellite.getFleetMember().setShipName(name);
        satellite.getFleetMember().setAlly(true);
        satellite.getFleetMember().setOwner(owner);
        if (Global.getSettings().getModManager().isModEnabled("automatic-orders")) {
            satellite.getVariant().addMod("automatic_orders_no_retreat");
        }
        satellite.setFixedLocation(satellite.getLocation());
    }
}

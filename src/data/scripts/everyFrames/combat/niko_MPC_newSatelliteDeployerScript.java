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
import data.utilities.niko_MPC_debugUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static data.utilities.niko_MPC_ids.niko_MPC_isSatelliteHullId;

public class niko_MPC_newSatelliteDeployerScript extends BaseEveryFrameCombatPlugin {

    private float framesToWait = 2;

    @Override
    public void init(CombatEngineAPI engine) {
        super.init(engine);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCurrentState() != GameState.COMBAT) return;

        super.advance(amount, events);

        if (framesToWait > 0) {
            framesToWait--;
            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();

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

        int intOwner = (side == FleetSide.PLAYER ? 0 : 1);

        CombatFleetManagerAPI fleetManager = engine.getFleetManager(side);
        List<FleetMemberAPI> reserves = fleetManager.getReservesCopy();
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : ships) {
            if (ship.getHullSpec().hasTag(niko_MPC_isSatelliteHullId) && (ship.getOwner() == intOwner)) {
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
            initialXCoord = (float) (mapWidth*-0.39); // if we have more than one, we set it to be at the very edge of the map
        }
        float xOffset = 0;
        if (size > 1) {
            xOffset = (initialXCoord * -2) / (size - 1); //ex. if we start at 400, 2 satellites causes it to be 200, then 100, 50, etc
        }
        float xThresholdForWrapping = mapWidth;

        float initialYCoord = (float) (mapHeight*-0.27);
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
            ShipAPI satelliteShip = null;
            if (fleetManager.getDeployedCopy().contains(satellite)) {
                niko_MPC_debugUtils.displayError("satellite already deployed");
                satelliteShip = fleetManager.getShipFor(satellite);
            }
            deploySatellite(satellite, new Vector2f(xCoord, yCoord), facing, fleetManager, side, satelliteShip);
            xCoord += xOffset;
        }
    }

    private void deploySatellite(FleetMemberAPI satelliteFleetMember, Vector2f vector2f, float facing, CombatFleetManagerAPI fleetManager, FleetSide side, ShipAPI satellite) {
        if (satellite == null) {
            satellite = fleetManager.spawnFleetMember(satelliteFleetMember, vector2f, facing, 0);
        }
        satellite.getLocation().set(vector2f);

        int owner = 1;
        if (side == FleetSide.PLAYER) {
            owner = 0;
        }
        String name = Global.getSector().getFaction(Factions.DERELICT).pickRandomShipName();
        if (name == null) {
            niko_MPC_debugUtils.displayError("deploySatellite null name");
            name = "this name is an error, please report this to niko";
        }

        List<ShipAPI> modulesWithSatellite = (satellite.getChildModulesCopy());
        modulesWithSatellite.add(satellite);

        for (ShipAPI module : modulesWithSatellite) {
            if (module.getFleetMember() != null) {
                module.getFleetMember().setShipName(name);
                module.getFleetMember().setAlly(true);
                module.getFleetMember().setOwner(owner);
            }
            module.setOwner(owner);
            if (owner == 0) {
                module.setAlly(true);
            }
            if (Global.getSettings().getModManager().isModEnabled("automatic-orders")) {
                module.getVariant().addMod("automatic_orders_no_retreat");
            }
        }

        for (FighterWingAPI wing : satellite.getAllWings()) {
            for (ShipAPI fighter : wing.getWingMembers()) {
                fighter.getLocation().set(vector2f);
                if (owner == 0) {
                    fighter.setAlly(true);
                }
            }
        }

        satellite.setFixedLocation(vector2f);
    }
}

package data.scripts.campaign.plugins.battleCreation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.misc.niko_MPC_satelliteParams;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

import static data.utilities.niko_MPC_satelliteUtils.getEntitySatelliteParams;

public abstract class niko_MPC_baseSatelliteSpawnerBattlePlugin extends BattleCreationPluginImpl {

    private static final Logger log = Global.getLogger(niko_MPC_baseSatelliteSpawnerBattlePlugin.class);

    static {
        log.setLevel(Level.ALL);
    }

    public List<SectorEntityToken> entitiesOnPlayerSide;
    public List<SectorEntityToken> entitiesOnOtherSide;

    public int maxSatellitesPerSide = 20;
    /**
     * Hashmap of fleetside to shipapi, holding every satellite active for every side in combat. Todo: Remove it if one blows up
     */
    public HashMap<FleetSide, List<ShipAPI>> currentSatellitesForSide = new HashMap<>();
    public int maxSatellitesTotal = 40;

    private BattleCreationContext context;
    private MissionDefinitionAPI loader;

    public niko_MPC_baseSatelliteSpawnerBattlePlugin(List<SectorEntityToken> entitiesHostileToPlayer, List<SectorEntityToken> entitiesHostileToOpponent) {
        super();

        this.entitiesOnPlayerSide = entitiesHostileToPlayer;
        this.entitiesOnOtherSide = entitiesHostileToOpponent;
    }

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI loader) {
        super.initBattle(context, loader);

        addSatellites(Global.getCombatEngine());
    }

    private void addSatellites(CombatEngineAPI combatEngine) {

        List<ShipAPI> shipsOnSideOne = deploySatellites(combatEngine, entitiesOnPlayerSide, FleetSide.PLAYER);
        List<ShipAPI> shipsOnSideTwo = deploySatellites(combatEngine, entitiesOnOtherSide, FleetSide.ENEMY);
    }

    private List<ShipAPI> deploySatellites(CombatEngineAPI combatEngine, List<SectorEntityToken> entities, FleetSide side) {
        List<String> pickedVariants = new ArrayList<>();

        for (SectorEntityToken entity : entities) {
            if (atMax(side)) break;
            niko_MPC_satelliteParams params = getEntitySatelliteParams(entity);
            HashMap<String, Float> weightedVariants = params.weightedVariantIds;
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
            for (Map.Entry<String, Float> entry : weightedVariants.entrySet()) {
                picker.add(entry.getKey(), entry.getValue()); //add the keys and values to the picker in hashmap format, since
            } //the picker pick() uses the value as chance, key as value
            for (int i = 0; i < params.maxBattleSatellites; i++) {
                if (atMax(side)) break;
                pickedVariants.add(picker.pick());
            }
        }
        return spawnSatellites(combatEngine, pickedVariants, side);
    }

    private List<ShipAPI> spawnSatellites(CombatEngineAPI combatEngine, List<String> pickedVariants, FleetSide side) {
        List<ShipAPI> spawnedSatellites = new ArrayList<>();
        float facing = 0f;
        float yOffset = 5;
        float yCoordinate = 5;
        float xOffset = 5; // every time we add a new satellite we add this number to xCoordinate. this is to equally spaec all satellites apart
        float maxOrMinXTilWrapping = 100f; //todo: implement
        float initialXCoord = (((xOffset*pickedVariants.size())-xOffset)/-2); //plug this into desmos to see why i did it

        float xCoordinate = initialXCoord;

        if (side == FleetSide.ENEMY) { //the player spawns at the bottom, the enemy, at the top
            facing = 180f; //hoepfully this makes them look down
            yCoordinate = 50; //todo: i have no fucking clue if this is good
            yOffset *= -1; //invert it, so it moves down, not up
        }

        for (String variant : pickedVariants) {
            if (atMax(side)) break;
            Vector2f coordinates = new Vector2f(xCoordinate, yCoordinate);
            ShipAPI satellite = spawnNewSatellite(combatEngine, variant, side, coordinates, facing);
            xCoordinate += xOffset; //space the next one out
            spawnedSatellites.add(satellite);
        }
    return spawnedSatellites;
    }

    public ShipAPI spawnNewSatellite(CombatEngineAPI combatEngine, String variant, FleetSide side, Vector2f coordinates, float facing) {

        ShipAPI satellite = combatEngine.getFleetManager(FleetSide.PLAYER).spawnShipOrWing(variant, coordinates, facing);
        addedNewSatellite(side, satellite);
        satellite.getHullSpec().addTag("no_combat_chatter");
        int owner = 1;
        if (side == FleetSide.PLAYER) {
            owner = 0;
            satellite.setAlly(true);
        }
        String name = Global.getSector().getFaction(Factions.DERELICT).pickRandomShipName();
        satellite.getFleetMember().setShipName(name); //todo: placeholder
        satellite.getFleetMember().setAlly(true);
        satellite.getFleetMember().setOwner(owner);
        if (Global.getSettings().getModManager().isModEnabled("automatic-orders")) {
            satellite.getVariant().addMod("automatic_orders_no_retreat");
        }
        satellite.getHullSpec().addTag("$niko_MPC_satellite");
        satellite.setFixedLocation(satellite.getLocation());
        return satellite;
    }

    private void addedNewSatellite(FleetSide side, ShipAPI satellite) {
        currentSatellitesForSide.get(side).add(satellite);
    }

    private boolean atMax(FleetSide side) {
        int amountOfSatellitesInSide = currentSatellitesForSide.get(side).size();
        int amountOfSatellitesInTotal = currentSatellitesForSide.size();
        if (amountOfSatellitesInSide > maxSatellitesPerSide) {
            log.debug("Somehow, " + side + " exceeded the max satellites per side of " + maxSatellitesPerSide + " with " + amountOfSatellitesInSide + ".");
        }
        if (amountOfSatellitesInSide > maxSatellitesTotal) {
            log.debug("Somehow, " + side + " exceeded the max satellites of " + maxSatellitesTotal + " with " + amountOfSatellitesInSide + ".");
        }

        return (amountOfSatellitesInSide >= maxSatellitesPerSide || amountOfSatellitesInTotal >= maxSatellitesTotal);
    }

    private void repositionSatellites(List<ShipAPI> satellites) {
        Vector2f lastCoordinates = new Vector2f(0, 0);
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
    }
}

package data.scripts.everyFrames.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import com.fs.starfarer.loading.WeaponSpreadsheetLoader;
import data.scripts.campaign.terrain.satelliteBarrage.niko_MPC_defenseSatelliteBarrageTerrainPlugin;
import data.utilities.niko_MPC_debugUtils;
import data.utilities.niko_MPC_satelliteUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class niko_MPC_defenseSatelliteBarrageTerrainCombatEffect extends BaseEveryFrameCombatPlugin {

    private static final Logger log = Global.getLogger(niko_MPC_defenseSatelliteBarrageTerrainCombatEffect.class);

    static {
        log.setLevel(Level.ALL);
    }

    niko_MPC_defenseSatelliteBarrageTerrainPlugin terrain;
    float generalDelayBetweenShots = 0f;
    float generalDelayIncrement = 2f;

    HashMap<String, List<Float>> weaponsToSimulate = new HashMap<>();

    FleetSide side;

    public niko_MPC_defenseSatelliteBarrageTerrainCombatEffect(niko_MPC_defenseSatelliteBarrageTerrainPlugin terrain, BattleAPI.BattleSide side) {
        this(terrain, side, null);
    }

    public niko_MPC_defenseSatelliteBarrageTerrainCombatEffect(niko_MPC_defenseSatelliteBarrageTerrainPlugin terrain, BattleAPI.BattleSide side,
                                                               HashMap<String, List<Float>> weaponsToSimulate) {
        this.terrain = terrain;
        if (Global.getSector().getPlayerFleet().getBattle().getSide(side).contains(Global.getSector().getPlayerFleet())) {
            this.side = FleetSide.PLAYER;
        }
        else this.side = FleetSide.ENEMY;

        if (weaponsToSimulate != null) {
            this.weaponsToSimulate = weaponsToSimulate;
        }
        else {
            this.weaponsToSimulate.put("niko_MPC_longranged_heavymauler", new ArrayList<>(Arrays.asList(25f, 1300f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_hellbore", new ArrayList<>(Arrays.asList(40f, 750f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_mjolnir", new ArrayList<>(Arrays.asList(11f, 1200f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_reaper", new ArrayList<>(Arrays.asList(1f, 800f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_hammer", new ArrayList<>(Arrays.asList(7f, 1000f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_gauss", new ArrayList<>(Arrays.asList(5f, 1900f)));
            this.weaponsToSimulate.put("niko_MPC_longranged_hveldriver", new ArrayList<>(Arrays.asList(13f, 1500f)));
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {
        super.init(engine);

        applyTooltip(engine);
    }

    private void applyTooltip(final CombatEngineAPI engine) {
        final Object key1 = new Object();
        final String icon = Global.getSettings().getSpriteName("ui", "icon_tactical_cr_penalty");

        final String name2 = terrain.getTerrainName();

        boolean allied = side == FleetSide.PLAYER;

        String AlliedOrHostile = allied ? "Allied" : "Hostile";
        engine.addPlugin(new niko_MPC_artilleryWarningScript(allied, key1, icon, name2, AlliedOrHostile));
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, events);

        decrementGeneralDelay(amount);
        if (generalDelayBetweenShots > 0) return;
        FleetSide otherSide = (side == FleetSide.PLAYER) ? FleetSide.ENEMY : FleetSide.PLAYER;
        int otherSideInt = (otherSide == FleetSide.ENEMY) ? 1 : 0;

        CombatEngineAPI engine = Global.getCombatEngine();

        List<ShipAPI> enemyShips = new ArrayList<>();
        for (ShipAPI ship : engine.getShips()) {
            if (ship.getOwner() == otherSideInt) {
                enemyShips.add(ship);
            }
        }
        if (enemyShips.size() == 0) return;

        float xCoord = 0;
        float yCoord = 0;

        if (0.5 > Math.random()) { //if this is true, the projectile will come from the top or bottom
            xCoord = (float) ThreadLocalRandom.current().nextDouble((engine.getMapWidth()*-1), engine.getMapWidth());
            yCoord = (0.5 > Math.random()) ? engine.getMapHeight() : engine.getMapHeight()*-1;
        }
        else { //else, it comes from the sides
            xCoord = (0.5 > Math.random()) ? engine.getMapWidth() : engine.getMapWidth()*-1;
            yCoord = (float) ThreadLocalRandom.current().nextDouble((engine.getMapHeight()*-1), engine.getMapHeight());
        }

        Vector2f firingPoint = new Vector2f(xCoord, yCoord);

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        for (Map.Entry<String, List<Float>> entry : weaponsToSimulate.entrySet()) {
            List<Float> floatList = entry.getValue();
            picker.add(entry.getKey(), floatList.get(0)); //add the keys and values to the picker in hashmap format, since
        } //the picker pick() uses the value as chance, key as value

        String weaponToSimulate = picker.pick();
        float projectileSpeed = weaponsToSimulate.get(weaponToSimulate).get(1);
        Random rand = new Random();
        ShipAPI randomShip = enemyShips.get(rand.nextInt(enemyShips.size()));

        Vector2f fireAtPoint = AIUtils.getBestInterceptPoint(firingPoint, projectileSpeed, randomShip.getLocation(), randomShip.getVelocity());

        if (fireAtPoint != null) {
            float xProjOffset = (float) ThreadLocalRandom.current().nextDouble(-20f, 20f);
            float yProjOffset = (float) ThreadLocalRandom.current().nextDouble(-20f, 20f);
            fireAtPoint.x += xProjOffset;
            fireAtPoint.y += yProjOffset;
            float angle = VectorUtils.getAngle(firingPoint, fireAtPoint);
            CombatEntityAPI projectile = engine.spawnProjectile(null, null, weaponToSimulate, firingPoint, angle, null);
            incrementGeneralDelay(generalDelayIncrement);
        }
        else {
            log.debug("fireAtPoint was null when artillery was tried to be fired");
            log.debug(firingPoint + " = " + firingPoint.x + " " + firingPoint.y, new Exception("stacktracegen"));
        }


    }

    private void incrementGeneralDelay(float increment) {
        generalDelayBetweenShots += increment;
    }

    private void decrementGeneralDelay(float amount) {
        generalDelayBetweenShots = Math.max(0, generalDelayBetweenShots - amount);
    }
}

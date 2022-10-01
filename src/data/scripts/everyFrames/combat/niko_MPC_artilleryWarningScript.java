package data.scripts.everyFrames.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

// niko_MPC_antiAsteroidSatellites
public class niko_MPC_artilleryWarningScript extends BaseEveryFrameCombatPlugin {

    public boolean allied = false;
    public Object key;
    public String icon;
    public String name;
    public String alliedOrHostile;

    public ShipAPI playerShip;

    public niko_MPC_artilleryWarningScript(boolean allied, Object key, String icon, String name, String alliedOrHostile) {
        this.allied = allied;
        this.key = key;
        this.icon = icon;
        this.name = name;
        this.alliedOrHostile = alliedOrHostile;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI newPlayerShip = engine.getPlayerShip();
        if (newPlayerShip == playerShip) return;

        engine.maintainStatusForPlayerShip(key, icon, name, alliedOrHostile + " satellites firing upon battlefield, " +
                "expect artillery fire", allied);
    }
}

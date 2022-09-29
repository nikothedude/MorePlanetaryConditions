package data.scripts.everyFrames.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.utilities.niko_MPC_satelliteUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class niko_MPC_satellitePositionDebugger extends BaseEveryFrameCombatPlugin {

    private static final Logger log = Global.getLogger(niko_MPC_satellitePositionDebugger.class);

    static {
        log.setLevel(Level.ALL);
    }


    ShipAPI satellite;
    Vector2f idealPosition;
    float facing;

    public niko_MPC_satellitePositionDebugger(ShipAPI satellite, Vector2f idealPosition, float facing) {
        this.satellite = satellite;
        this.idealPosition = idealPosition;
        this.facing = facing;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, events);

        if (!satellite.isAlive()) {
            prepareForGarbageCollection();
            return;
        }
        if (satellite.getFacing() != facing) {
            satellite.setFacing(facing);
            for (ShipAPI module : satellite.getChildModulesCopy()) {
                module.setFacing(facing);
            }
        }
    }

    private void prepareForGarbageCollection() {
        satellite = null;
        Global.getCombatEngine().removePlugin(this);
    }
}

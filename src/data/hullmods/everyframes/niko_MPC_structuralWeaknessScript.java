package data.hullmods.everyframes;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.utilities.niko_MPC_debugUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class niko_MPC_structuralWeaknessScript extends BaseEveryFrameCombatPlugin {

    public ShipAPI holder;
    public Set<ShipAPI> modulesToDestroy;
    public CombatEngineAPI engine;


    public niko_MPC_structuralWeaknessScript(ShipAPI holder, Set<ShipAPI> modulesToDestroy) {
        this.holder = holder;
        this.modulesToDestroy = modulesToDestroy;
    }

    public void init(CombatEngineAPI engine) {
        this.engine = engine;

        if (!ensureVariablesNotNull()) {
            removeSelfFromEngine();
            return;
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        boolean destroyModules = !getHolder().isAlive();
        boolean isDone = false;
        Iterator<ShipAPI> iterator = getModulesToDestroy().iterator();
        while (iterator.hasNext()) {
            ShipAPI module = iterator.next();
            if (!module.isAlive()) {
                iterator.remove();
                continue;
            }
            if (destroyModules) {
                destroyModule(module);
                iterator.remove();
                isDone = true;
            }
        }
        if (isDone || getModulesToDestroy().isEmpty()) {
            removeSelfFromEngine();
        }
    }

    public void destroyModule(ShipAPI module) {
        module.setHitpoints(0.0000000000000000001f);
        while (module.isAlive()) {
            Vector2f damagePoint = module.getLocation();
            float damageAmount = 100;
            engine.applyDamage(module, damagePoint, damageAmount, DamageType.ENERGY, 0, true, false, null, false);
        }
    }

    public void removeSelfFromEngine() {
        engine.removePlugin(this);
        prepareForGarbageCollection();
    }

    private boolean ensureVariablesNotNull() {
        boolean result = true;

        if (engine == null) {
            niko_MPC_debugUtils.displayError("engine null on structural weakness script init");
            result = false;
        }

        if (getHolder() == null) {
            niko_MPC_debugUtils.displayError("structural weakness null holder on init");
            result = false;
        }
        if (getModulesToDestroy() == null) {
            niko_MPC_debugUtils.displayError("structural weakness modulesToDestroy null on init, holder: " + getHolder());
            result = false;
        }
        else {
            if (getModulesToDestroy().size() == 0) {
                niko_MPC_debugUtils.displayError("structural weakness modulesToDestroy.size() == 0 on init, holder: " + getHolder());
                result = false;
            }
        }
        return result;
    }

    public Set<ShipAPI> getModulesToDestroy() {
        return modulesToDestroy;
    }

    public ShipAPI getHolder() {
        return holder;
    }

    public void prepareForGarbageCollection() {
        holder = null;
        modulesToDestroy = null;
        engine = null;
    }
}

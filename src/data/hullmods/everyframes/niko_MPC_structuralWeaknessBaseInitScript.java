package data.hullmods.everyframes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// suicidal everyframe that only serves to create the big boy script
public abstract class niko_MPC_structuralWeaknessBaseInitScript extends BaseEveryFrameCombatPlugin {

    public List<String> weaknessTagIds;

    public ShipAPI ship;

    public niko_MPC_structuralWeaknessBaseInitScript(List<String> weaknessTagIds, ShipAPI ship) {
        this.weaknessTagIds = weaknessTagIds;
        this.ship = ship;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        super.advance(amount, events);
        Set<ShipAPI> modulesToDestroy = new HashSet<>();
        ShipAPI otherShip = ship;

        while (otherShip.getParentStation() != null) { // since we're a module and we're searching through all modules,
            otherShip = otherShip.getParentStation(); //keep going up the chain until you get the core ship,
            // so that we can compare all their modules tags to our own
        }

        List<ShipAPI> modules = otherShip.getChildModulesCopy();

        for (ShipAPI module : modules) {
            if (module.isStationModule()) { //sanity
                boolean doneLooping = false;

                for (String tag : module.getHullSpec().getTags()) {
                    if (weaknessTagIds.contains(tag)) {
                        if (foundCompatibleWeakness(tag, module)) {
                            modulesToDestroy.add(module);
                        }
                        if (stopIteration(tag, module)) {
                            doneLooping = true;
                            break;
                        }
                    }
                }
            }
        }

        if (modulesToDestroy.size() > 0) {
            initializeWeaknessScript(ship, modulesToDestroy);
        }
        removeSelfFromEngine();
    }

    public void removeSelfFromEngine() {
        Global.getCombatEngine().removePlugin(this); //we were only ever needed for one frame
        prepareForGarbageCollection();
    }

    public void prepareForGarbageCollection() {
        ship = null;
        weaknessTagIds = null;
    }

    public void initializeWeaknessScript (ShipAPI ship, Set<ShipAPI> modulesToDestroy) {
        CombatEngineAPI engine = Global.getCombatEngine();
        niko_MPC_structuralWeaknessScript script = createNewWeaknessScript(engine, ship, modulesToDestroy);
        engine.addPlugin(script);
        return;
    }

    protected abstract niko_MPC_structuralWeaknessScript createNewWeaknessScript(CombatEngineAPI engine, ShipAPI ship, Set<ShipAPI> modulesToDestroy);

    public boolean foundCompatibleWeakness(String id, ShipAPI module) {
        return true;
    }

    public boolean stopIteration(String id, ShipAPI module) {
        return true;
    }
}
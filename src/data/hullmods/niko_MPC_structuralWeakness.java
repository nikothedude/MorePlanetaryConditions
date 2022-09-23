package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.hullmods.everyframes.niko_MPC_structuralWeaknessBaseInitScript;

import java.util.ArrayList;
import java.util.List;

public abstract class niko_MPC_structuralWeakness extends BaseHullMod {

    public List<String> weaknessTagIds = new ArrayList<>();

    public niko_MPC_structuralWeakness() {
        weaknessTagIds.add(niko_MPC_structuralWeaknessIds.universalWeaknessTag);

        addTypeSpecificWeaknessIds();
    }

    protected abstract void addTypeSpecificWeaknessIds();

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);

        initializeWeaknessScript(ship);
        // reasoning for adding a script: a lot of things are set in the 1st frame of combat, many of which are essential for fucking with modules
        // without this, we wouldnt be able to get the parent ship, thus we wouldnt be able to iterate through any of the modules
    }

    public void initializeWeaknessScript(ShipAPI ship) {
        niko_MPC_structuralWeaknessBaseInitScript script = createNewWeaknessInitScript(weaknessTagIds, ship);
        Global.getCombatEngine().addPlugin(script);
    }

    public abstract niko_MPC_structuralWeaknessBaseInitScript createNewWeaknessInitScript(List<String> weaknessTagIds, ShipAPI ship);
}

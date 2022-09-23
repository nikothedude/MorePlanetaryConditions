package data.hullmods.everyframes;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;
import java.util.Set;

public class niko_MPC_structuralWeaknessInitScript extends niko_MPC_structuralWeaknessBaseInitScript{
    public niko_MPC_structuralWeaknessInitScript(List<String> weaknessTagIds, ShipAPI ship) {
        super(weaknessTagIds, ship);
    }

    @Override
    protected niko_MPC_structuralWeaknessScript createNewWeaknessScript(CombatEngineAPI engine, ShipAPI ship, Set<ShipAPI> modulesToDestroy) {
        return (new niko_MPC_structuralWeaknessScript(ship, modulesToDestroy));
    }
}

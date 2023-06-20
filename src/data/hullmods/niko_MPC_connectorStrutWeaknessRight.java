package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.hullmods.everyframes.niko_MPC_structuralWeaknessBaseInitScript;
import data.hullmods.everyframes.niko_MPC_structuralWeaknessInitScript;

import java.util.Collection;
import java.util.List;

public class niko_MPC_connectorStrutWeaknessRight extends niko_MPC_structuralWeakness {

    @Override
    protected void addTypeSpecificWeaknessIds() {
        weaknessTagIds.add(niko_MPC_structuralWeaknessIds.satelliteConnectorStrutWeaknessRightTag);
    }

    @Override
    public niko_MPC_structuralWeaknessBaseInitScript createNewWeaknessInitScript(List<String> weaknessTagIds,
                                                                                 ShipAPI ship) {
        return (new niko_MPC_structuralWeaknessInitScript(weaknessTagIds, ship));
    }

}

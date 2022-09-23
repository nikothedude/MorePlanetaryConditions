package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class niko_MPC_graviticSupercomputer extends BaseHullMod {

    public final float rangePercent = 500f;
    public final float visionIncrement = 5000f;
    public final float recoilPercent = 70f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, rangePercent);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, rangePercent);
        stats.getMissileWeaponRangeBonus().modifyPercent(id, rangePercent);

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, -rangePercent/2);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, -rangePercent/2);

        stats.getSightRadiusMod().modifyFlat(id, visionIncrement);

        stats.getRecoilPerShotMult().modifyPercent(id, recoilPercent);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)Math.round(rangePercent) + "%";
        else if (index == 1) return "" + (int)Math.round(recoilPercent) + "%";
        return null;
    }
}

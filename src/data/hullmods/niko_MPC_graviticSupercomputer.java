package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class niko_MPC_graviticSupercomputer extends BaseHullMod {

    public final float rangeMult = 6.5f;
    public final float pdRangeMult = 0.7f;
    public final float visionIncrement = 5000f;
    public final float recoilMult = 0.3f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyMult(id, rangeMult);
        stats.getEnergyWeaponRangeBonus().modifyMult(id, rangeMult);
        stats.getMissileWeaponRangeBonus().modifyMult(id, rangeMult);

        stats.getNonBeamPDWeaponRangeBonus().modifyMult(id, pdRangeMult);

        stats.getSightRadiusMod().modifyFlat(id, visionIncrement);

        stats.getRecoilPerShotMult().modifyMult(id, recoilMult);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)Math.round(rangeMult) + "00" + "%";
        else if (index == 1) return "" + (int)Math.round(recoilMult) + "00" + "%";
        return null;
    }
}

package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class niko_MPC_graviticSupercomputer extends BaseHullMod {

    public static final float rangeMult = 10000f;
    public static final float recoilMult = 70f;
    public static final float weaponSpeedMult = 125f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyPercent(id, rangeMult);
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, rangeMult);
        stats.getMissileWeaponRangeBonus().modifyPercent(id, rangeMult);

        stats.getRecoilPerShotMult().modifyPercent(id, recoilMult);

        stats.getProjectileSpeedMult().modifyPercent(id, weaponSpeedMult);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 0) return "" + (int)Math.round(rangeMult) + "%";
        return null;
    }
}

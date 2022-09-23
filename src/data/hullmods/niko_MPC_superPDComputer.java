package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class niko_MPC_superPDComputer extends BaseHullMod {

    public final float missileDamagePercent = 50f;
    public final float rangePercent = 5000f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {

        stats.getNonBeamPDWeaponRangeBonus().modifyPercent(id, rangePercent);
        stats.getBeamPDWeaponRangeBonus().modifyPercent(id, rangePercent);

        stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
        stats.getDynamic().getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f);
        stats.getDamageToMissiles().modifyPercent(id, missileDamagePercent);
    }

    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if (index == 1) return "" + (int)Math.round(rangePercent) + "%";
        else if (index == 0) return "" + (int)Math.round(missileDamagePercent) + "%";
        return null;
    }

}

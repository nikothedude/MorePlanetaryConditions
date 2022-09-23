package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class niko_MPC_noExplosionDamage extends BaseHullMod {

    public float hybridMult = 0f;

    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyMult(id, hybridMult);
        stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT).modifyMult(id, hybridMult);
    }

}

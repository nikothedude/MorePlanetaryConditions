package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import kotlin.math.abs
import kotlin.math.roundToInt

class MPC_kantasFavored: BaseHullMod() {

    companion object {
        const val MISSILE_AMMO_MULT = 2f

        const val SHIELD_DAMAGE_TAKEN_MULT = 0.3f
        const val SHIELD_PIERCE_CHANCE_MULT = 0.4f
        const val SHIELD_ARC_MULT = 5.25f
        const val SHIELD_RAISE_RATE_MULT = 2.1f

        const val FLUX_DISSIPATION_BONUS = 200f

        const val HULL_MULT = 0.6f
        const val ARMOR_MULT = 0.1f

        const val ACCEL_MULT = 3f

        const val RANGE_MULT = 1.3f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
        if (hullSize == null || stats == null || id == null) return

        stats.ballisticWeaponRangeBonus.modifyMult(id, RANGE_MULT)
        stats.energyWeaponRangeBonus.modifyMult(id, RANGE_MULT)

        stats.missileAmmoBonus.modifyMult(id, MISSILE_AMMO_MULT)

        stats.shieldDamageTakenMult.modifyMult(id, SHIELD_DAMAGE_TAKEN_MULT)
        stats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, SHIELD_PIERCE_CHANCE_MULT)
        stats.shieldArcBonus.modifyMult(id, SHIELD_ARC_MULT)
        stats.shieldUnfoldRateMult.modifyMult(id, SHIELD_RAISE_RATE_MULT)

        stats.fluxDissipation.modifyFlat(id, FLUX_DISSIPATION_BONUS)

        stats.hullBonus.modifyMult(id, HULL_MULT)
        stats.armorBonus.modifyMult(id, ARMOR_MULT)

        stats.acceleration.modifyMult(id, ACCEL_MULT)
        stats.deceleration.modifyMult(id, ACCEL_MULT)
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        super.applyEffectsAfterShipCreation(ship, id)
        if (ship == null || id == null) return

    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            0 -> "favored ship"
            1 -> "kanta clan"
            2 -> {
                "" + ((1f - SHIELD_DAMAGE_TAKEN_MULT) * 100f).roundToInt() + "%"
            }
            3 -> {
                "" + ((1f - SHIELD_PIERCE_CHANCE_MULT) * 100f).roundToInt() + "%"
            }
            4 -> {
                "" + (abs(1f - SHIELD_ARC_MULT) * 100f).roundToInt() + "%"
            }
            5 ->{
                "${FLUX_DISSIPATION_BONUS.toInt()}"
            }
            6 -> {
                "" + ((abs(1f - RANGE_MULT)) * 100f).roundToInt() + "%"
            }
            7 -> {
                "" + (abs(1f - MISSILE_AMMO_MULT) * 100f).roundToInt() + "%"
            }
            8 -> {
                "" + ((1f - HULL_MULT) * 100f).roundToInt() + "%"
            }
            9 -> {
                "" + ((1f - ARMOR_MULT) * 100f).roundToInt() + "%"
            }

            else -> null
        }
    }
}
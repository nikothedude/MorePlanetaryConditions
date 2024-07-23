package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.campaign.ids.Stats
import kotlin.math.roundToInt

class niko_MPC_fighterSolarShielding: BaseHullMod() {

    companion object {
        const val CORONA_EFFECT_MULT = 0.25f
        const val ENERGY_DAMAGE_MULT = 0.7f

        const val SMOD_CORONA_EFFECT_MULT = 0f
    }

    override fun applyEffectsToFighterSpawnedByShip(fighter: ShipAPI?, ship: ShipAPI?, id: String?) {
        if (fighter == null || ship == null || id == null) return

        val sModded = isSMod(ship)
        var coronaMult = if (sModded) SMOD_CORONA_EFFECT_MULT else CORONA_EFFECT_MULT

        fighter.mutableStats.energyDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_MULT)
        fighter.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, coronaMult)
    }

    override fun getSModDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return if (index == 0) "" + ((1f - SMOD_CORONA_EFFECT_MULT) * 100f).roundToInt() + "%" else null
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        return when (index) {
            0 -> {
                "" + ((1f - CORONA_EFFECT_MULT) * 100f).roundToInt() + "%"
            }

            1 -> {
                "" + ((1f - ENERGY_DAMAGE_MULT) * 100f).roundToInt() + "%"
            }

            else -> null
        }
    }
}
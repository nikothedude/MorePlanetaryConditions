package data.scripts.campaign.skills

import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect
import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_battleUtils.isPD

class MPC_fractalineOptimization {

    companion object {
        const val TIMEFLOW_PERCENT = 10f

        const val RANGE_MATCH_INCREASE = 300f
    }

    class Level1: ShipSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return "+${TIMEFLOW_PERCENT.toInt()}% timeflow"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            stats?.timeMult?.modifyPercent(id, TIMEFLOW_PERCENT, "Fractaline Optimizations")
        }

        override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
            stats?.timeMult?.unmodify(id)
        }
    }

    class Level2: AfterShipCreationSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return "Increases base range of all non-PD ballistic and energy weapons by ${RANGE_MATCH_INCREASE.toInt()} up to the longest range " +
                    "\namongst non-PD ballistic and energy weapons"
        }

        override fun getEffectPerLevelDescription(): String? {
            return null
        }

        override fun getScopeDescription(): LevelBasedEffect.ScopeDescription {
            return LevelBasedEffect.ScopeDescription.PILOTED_SHIP
        }

        override fun apply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            return
        }

        override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) {
            return
        }

        override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
            if (ship == null) return

            ship.addListener(MPC_fractalineOptimizationRangeListener(ship))
        }

        override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
            if (ship == null) return

            ship.removeListenerOfClass(MPC_fractalineOptimizationRangeListener::class.java)
        }
    }

    class MPC_fractalineOptimizationRangeListener(
        val ourShip: ShipAPI
    ): WeaponBaseRangeModifier {
        val weaponsToRangeBuff = HashMap<WeaponAPI, Float>()

        init {
            val viableWeapons = ourShip.allWeapons.filter {
                it.type != WeaponAPI.WeaponType.MISSILE &&
                it.type != WeaponAPI.WeaponType.COMPOSITE &&
                it.type != WeaponAPI.WeaponType.SYNERGY &&
                it.type != WeaponAPI.WeaponType.UNIVERSAL &&
                !it.isDecorative &&
                !it.isPD()
            }

            var highestRange: Float = 0f

            for (weapon in viableWeapons) {
                val baseRange = Misc.getAdjustedBaseRange(weapon.spec.maxRange, ourShip, weapon)
                if (baseRange > highestRange) {
                    highestRange = baseRange
                }
            }

            for (weapon in viableWeapons) {
                val baseRange = Misc.getAdjustedBaseRange(weapon.spec.maxRange, ourShip, weapon)
                val rangePreClamp = (baseRange + RANGE_MATCH_INCREASE)
                val rangePostClamp = (rangePreClamp.coerceAtMost(highestRange))
                val buff = RANGE_MATCH_INCREASE - (rangePreClamp - rangePostClamp)
                weaponsToRangeBuff[weapon] = buff
            }
        }

        override fun getWeaponBaseRangePercentMod(ship: ShipAPI?, weapon: WeaponAPI?): Float {
            return 0f
        }

        override fun getWeaponBaseRangeMultMod(ship: ShipAPI?, weapon: WeaponAPI?): Float {
            return 1f
        }

        override fun getWeaponBaseRangeFlatMod(ship: ShipAPI?, weapon: WeaponAPI?): Float {
            if (weapon == null) return 0f
            return weaponsToRangeBuff[weapon] ?: 0f
        }
    }

}
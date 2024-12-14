package data.scripts.campaign.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.*
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.isAutomated

class MPC_battlemind {

    companion object {
        const val CR_INCREMENT = 0.2f

        const val MAX_EFFECT_RANGE = 5000f
        const val RANGE_FOR_MAX_BONUS = 1000f
        const val BASE_TIME_BONUS = 15f
        const val BASE_RANGE_PERCENT = 20f
    }

    class Level1: AfterShipCreationSkillEffect {
        override fun getEffectDescription(level: Float): String {
            return "Friendly automated ships within ${MAX_EFFECT_RANGE}su gain bonus timeflow and weapon range"
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
            if (ship == null || id == null) return
            if (Global.getCombatEngine().customData["MPC_battlemindScript"] != null) return
            val plugin = MPC_battlemindScript(ship, id)
            Global.getCombatEngine().customData["MPC_battlemindScript"] = plugin
            Global.getCombatEngine()?.addPlugin(plugin)
        }

        override fun unapplyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
            if (ship == null || id == null) return
            val plugin = Global.getCombatEngine().customData["MPC_battlemindScript"] as? MPC_battlemindScript ?: return
            Global.getCombatEngine()?.removePlugin(plugin)
        }
    }

    class MPC_battlemindScript(
        val parent: ShipAPI,
        val id: String
    ): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(0.1f, 0.11f)
        val affectedShips = HashSet<ShipAPI>()
        val engine = Global.getCombatEngine()
        var maintainingStatus = false

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (!parent.isAlive || !engine.isEntityInPlay(parent)) {
                clearAffected()
                return
            }
            if (maintainingStatus) {
                engine.maintainStatusForPlayerShip(
                    "${id}1",
                    "graphics/icons/hullsys/temporal_shell.png",
                    "Battlemind",
                    "Bonus timeflow & weapon range",
                    false
                )
            }

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                checkProximity()
            }
        }

        private fun checkProximity() {
            clearAffected()
            val iterator = engine.shipGrid.getCheckIterator(parent.location, MAX_EFFECT_RANGE, MAX_EFFECT_RANGE)
            while (iterator.hasNext()) {
                val ship = iterator.next() as? ShipAPI ?: continue
                if (ship.owner != parent.owner) continue
                if (ship.isFighter) continue
                if (ship == parent) continue
                if (!ship.isAutomated()) continue
                if (ship.isHulk) continue

                val distance = MathUtils.getDistance(parent.location, ship.location)
                apply(ship, distance)
                affectedShips += ship
            }
        }

        private fun clearAffected() {
            if (affectedShips.isEmpty()) return
            val copy = affectedShips.toList()
            affectedShips.clear()
            copy.forEach { unapply(it) }
        }

        fun apply(ship: ShipAPI, distance: Float) {
            val adjustedDist = (distance - RANGE_FOR_MAX_BONUS).coerceAtLeast(0f)
            if (adjustedDist > MAX_EFFECT_RANGE) return
            val mult = ((1 - (1 / (MAX_EFFECT_RANGE / adjustedDist))))
            if (mult <= 0f) return

            ship.mutableStats.timeMult.modifyPercent(id, BASE_TIME_BONUS * mult)

            ship.mutableStats.ballisticWeaponRangeBonus.modifyPercent(id, BASE_RANGE_PERCENT * mult)
            ship.mutableStats.energyWeaponRangeBonus.modifyPercent(id, BASE_RANGE_PERCENT * mult)

            if (engine.playerShip == ship) {
                maintainingStatus = true
            }
        }
        fun unapply(ship: ShipAPI) {
            ship.mutableStats.timeMult.unmodify(id)

            ship.mutableStats.ballisticWeaponRangeBonus.unmodify(id)
            ship.mutableStats.energyWeaponRangeBonus.unmodify(id)

            if (engine.playerShip == ship) {
                maintainingStatus = false
            }
        }
    }
}
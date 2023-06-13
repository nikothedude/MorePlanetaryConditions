package data.scripts.shipsystems.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.DamperFieldStats
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.shipsystems.niko_MPC_overclockedTargettingSystemStats
import data.utilities.niko_MPC_battleUtils.isPD
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f

class niko_MPC_overclockedTargettingSystemAI: niko_MPC_coreOverclockedTargettingSystemAI {

    private var engine: CombatEngineAPI = Global.getCombatEngine()
    private var system: ShipSystemAPI? = null
    private var ship: ShipAPI? = null
    private val runOnce = false
    val interval: IntervalUtil = IntervalUtil(0.5f, 0.5f)

    val usageThreshold = 1.02f

    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        this.ship = ship
        this.system = system
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (engine.isPaused || ship == null || ship!!.shipAI == null) {
            return
        }
        if (system == null) return
        if (ship!!.isHoldFire) return
        if (system!!.isActive) return
        if (system!!.isCoolingDown) return
        if (ship!!.fluxTracker.isOverloaded) return

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            val missiles = AIUtils.getEnemyMissilesOnMap(ship)
            val ships = AIUtils.getEnemiesOnMap(ship)
            if (ships.isEmpty() && missiles.isEmpty()) return
            var score = 0.0
            if (ship!!.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)) score -= 20.0
            val scaredOfFlux = ship!!.fluxLevel > 0.75
            if (scaredOfFlux) score -= 5.0

            var highestModifiedRange = 0f
            var highestRange = 0f
            var lowestRange: Float? = null
            var highestModifiedPDRange = 0f
            var highestPDRange = 0f
            var lowestPDRange: Float? = null
            for (weapon in ship!!.usableWeapons) {
                if (lowestRange == null) lowestRange = weapon.range
                if (highestRange < weapon.range) highestRange = weapon.range
                if (lowestRange > weapon.range) lowestRange = weapon.range
                val modifiedRange = weapon.range * niko_MPC_overclockedTargettingSystemStats.rangeMult
                if (highestModifiedRange < modifiedRange) highestModifiedRange = modifiedRange

                if (weapon.isPD()) {
                    if (lowestPDRange == null) lowestPDRange = weapon.range
                    if (highestPDRange < weapon.range) highestPDRange = weapon.range
                    if (lowestPDRange > weapon.range) lowestPDRange = weapon.range
                    val modifiedPDRange = weapon.range * niko_MPC_overclockedTargettingSystemStats.rangeMult
                    if (highestModifiedPDRange < modifiedPDRange) highestModifiedPDRange = modifiedRange
                }
            }
            for (ship in ships) {
                val distance = Misc.getDistance(this.ship!!.location, ship.location)
                if (distance > highestModifiedRange) continue
                var scoreMult = 1f
                if (ship.isFighter) scoreMult /= 5f
                if (ship == this.ship!!.shipTarget) scoreMult *= 40f
                val anchor = highestRange * 1.05
                score += ((distance / anchor)-1) * scoreMult
            }
            if (score > 0) score /= ships.size else score *= ships.size
            for (missile in missiles) {
                val distance = Misc.getDistance(this.ship!!.location, missile.location)
                if (distance > highestModifiedPDRange) continue
                val anchor = highestPDRange * 1.05
                var scoreMult = 0.1f
                if (missile.engineController.isFlamedOut) scoreMult /= 2f
                score += (((distance / anchor)-1) * scoreMult).coerceAtMost(0.0)
            }
            if (activateIfPossible(score)) return
        }
    }

    private fun activateIfPossible(score: Double): Boolean {
        if (score >= usageThreshold) {
            ship!!.useSystem()
            return true
        }
        return false
    }
}
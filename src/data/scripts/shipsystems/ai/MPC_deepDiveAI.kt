package data.scripts.shipsystems.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f

class MPC_deepDiveAI: ShipSystemAIScript {
    protected var system: ShipSystemAPI? = null
    protected var ship: ShipAPI? = null
    val engine = Global.getCombatEngine()
    val interval: IntervalUtil = IntervalUtil(0.1f, 0.2f)

    override fun init(ship: ShipAPI?, system: ShipSystemAPI?, flags: ShipwideAIFlags?, engine: CombatEngineAPI?) {
        this.ship = ship
        this.system = system
    }

    override fun advance(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (engine.isPaused || ship == null || ship!!.shipAI == null) {
            return
        }
        if (system == null) return
        if (system!!.isActive) return
        if (system!!.isCoolingDown) return
        if (ship!!.fluxTracker.isOverloaded) return

        interval.advance(amount)
        if (interval.intervalElapsed()) {
            tryToActivate(amount, missileDangerDir, collisionDangerDir, target)
        }
    }

    open fun tryToActivate(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        doLogic(amount, missileDangerDir, collisionDangerDir, target)
    }

    open fun doLogic(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        val parent = ship!!.parentStation
        if (parent != null && parent != ship) {
            if (!parent.system.isActive) return
        }

        /*if (target == null) {
            val ships = AIUtils.getEnemiesOnMap(ship)
            val missiles = AIUtils.getEnemyMissilesOnMap(ship)
            if (ships.isEmpty() && missiles.isEmpty()) return
            activateIfPossible(usageThreshold.toDouble())
            return
        }
        var lowestRange: Float? = null
        for (weapon in ship!!.usableWeapons) {
            if (lowestRange == null) lowestRange = weapon.range
            if (lowestRange > weapon.range) lowestRange = weapon.range
        }
        if (lowestRange == null) return

        val distance = Misc.getDistance(this.ship!!.location, target.location)
        if (distance < lowestRange) return
        activateIfPossible(usageThreshold.toDouble())

    }

    fun activateIfPossible(score: Double): Boolean {
        if (score >= usageThreshold) {
            ship!!.useSystem()
            return true
        }
        return false
    }*/
    }
}
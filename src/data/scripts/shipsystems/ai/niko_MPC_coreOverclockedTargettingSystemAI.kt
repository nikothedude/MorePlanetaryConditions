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

open class niko_MPC_coreOverclockedTargettingSystemAI: ShipSystemAIScript {

    open val overloadTime = 0f

    protected var engine: CombatEngineAPI = Global.getCombatEngine()
    protected var system: ShipSystemAPI? = null
    protected var ship: ShipAPI? = null
    protected val runOnce = false
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
            tryToActivate(amount, missileDangerDir, collisionDangerDir, target)
        }
    }

    open fun tryToActivate(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        doLogic(amount, missileDangerDir, collisionDangerDir, target)
    }

    open fun doLogic(amount: Float, missileDangerDir: Vector2f?, collisionDangerDir: Vector2f?, target: ShipAPI?) {
        if (target == null) {
            val ships = AIUtils.getEnemiesOnMap(ship)
            val missiles = AIUtils.getEnemyMissilesOnMap(ship)
            if (ships.isEmpty() && missiles.isEmpty()) return 
            activateIfPossible(usageThreshold)
            return
        }
        var lowestRange: Float? = null
        for (weapon in ship!!.usableWeapons) {
            if (lowestRange == null) lowestRange = weapon.range
            if (lowestRange > weapon.range) lowestRange = weapon.range
        }

        val distance = Misc.getDistance(this.ship!!.location, target.location)
        if (distance < lowestRange) return
        activateIfPossible(usageThreshold)
        
    }

    private fun activateIfPossible(score: Double): Boolean {
        if (score >= usageThreshold) {
            ship!!.useSystem()
            return true
        }
        return false
    }
}
package data.scripts.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil

class niko_MPC_overclockedTargettingSystemOverloadScript(
    val ship: ShipAPI,
    val time: Float

) : BaseEveryFrameCombatPlugin() {

    val timer = IntervalUtil(time, time)

    override fun init(engine: CombatEngineAPI?) {
        super.init(engine)

        ship.fluxTracker.forceOverload(time)
    }

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)

        val engine = Global.getCombatEngine()
        if (engine.isPaused) return

        val globalTimeMult = engine.timeMult.modifiedValue
        val timeMult = ship.mutableStats.timeMult.modifiedValue

        val calculatedAmount = amount*(globalTimeMult * timeMult)
        timer.advance(calculatedAmount)
        if (timer.intervalElapsed()) {
            ship.fluxTracker.stopOverload()
            engine.removePlugin(this)
        }
    }
}
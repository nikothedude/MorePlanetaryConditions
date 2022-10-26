package data.scripts.everyFrames.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import data.utilities.niko_MPC_settings
import org.apache.log4j.Level
import org.lwjgl.util.vector.Vector2f

class niko_MPC_satellitePositionDebugger(var satellite: ShipAPI, var idealPosition: Vector2f, var facing: Float) :
    BaseEveryFrameCombatPlugin() {
    var preventTurn: Boolean = niko_MPC_settings.PREVENT_SATELLITE_TURN
    var timeToLive = 20f

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        super.advance(amount, events)
        if (!preventTurn) {
            timeToLive -= amount
            if (amount <= 0) {
                prepareForGarbageCollection()
                return  // this script serves 2 purposes: fix incorrect facing on battle start and prevent turning
                // due to the 1st this script needs to live for a bit even if preventTurn is false
            }
        }
        if (!satellite.isAlive) {
            prepareForGarbageCollection()
            return
        }
        if (satellite.facing != facing) {
            satellite.facing = facing
            for (module in satellite.childModulesCopy) {
                module.facing = facing
            }
        }
    }

    private fun prepareForGarbageCollection() {
        Global.getCombatEngine().removePlugin(this)
    }
}

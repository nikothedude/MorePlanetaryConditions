package data.scripts.everyFrames.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI

class MPC_aegisRocketPodsCombatScript: BaseEveryFrameCombatPlugin() {
    var initialized = false

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        super.advance(amount, events)

        if (!initialized) {
            val playerFleet = Global.getSector()?.playerFleet ?: return

            initialized = true
        }
    }
}
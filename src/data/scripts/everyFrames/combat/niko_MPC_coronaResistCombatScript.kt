package data.scripts.everyFrames.combat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import data.utilities.niko_MPC_ids

class niko_MPC_coronaResistCombatScript: BaseEveryFrameCombatPlugin() {
    val skipShips: MutableSet<ShipAPI> = HashSet()
    val interval: IntervalUtil = IntervalUtil(0.2f, 0.3f)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine() ?: return
        if (engine.isPaused) return

        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        for (ship in engine.ships) {
            if (skipShips.contains(ship)) continue
            skipShips += ship
            val fleetMember = ship.fleetMember ?: continue
            val fleet = fleetMember.fleetData?.fleet ?: continue
            val coronaResistAmount = fleet.memoryWithoutUpdate[niko_MPC_ids.CORONA_RESIST_MEMORY_FLAG] as? Float ?: continue
            ship.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult("niko_MPC_coronaResistFleetStuff", coronaResistAmount)
        }
    }
}
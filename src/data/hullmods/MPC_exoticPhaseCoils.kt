package data.hullmods

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.impl.combat.MoteControlScript
import data.scripts.shipsystems.MPC_spatialBreachStats
import data.utilities.niko_MPC_stringUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.max

class MPC_exoticPhaseCoils: BaseHullMod() {

    companion object {
        const val PHASE_COOLDOWN_INCREASE = 2f
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI, id: String?) {
        stats.phaseCloakCooldownBonus.modifyMult(id, PHASE_COOLDOWN_INCREASE)
    }

    override fun getDescriptionParam(index: Int, hullSize: HullSize?): String? {
        if (index == 0) return niko_MPC_stringUtils.toPercent(PHASE_COOLDOWN_INCREASE - 1)
        return null
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        super.advanceInCombat(ship, amount)
        if (ship == null) return
        if (ship.isHulk) return

        val data = MoteControlScript.getSharedData(ship) ?: return
        val maxMotes = MPC_spatialBreachStats.MAX_MOTES
        val fraction: Float = data.motes.size / (max(1f, maxMotes.toFloat()))
        var volume = fraction * 3f
        if (volume > 1f) volume = 1f
        for (mote in data.motes.toMutableList()) {
            if (!Global.getCombatEngine().isEntityInPlay(mote)) {
                data.motes.remove(mote)
            }
        }
        if (data.motes.size > 3) {
            val com = Vector2f()
            for (mote in data.motes) {
                Vector2f.add(com, mote.location, com)
            }
            com.scale(1f / data.motes.size)
            //Global.getSoundPlayer().playLoop("mote_attractor_loop", ship, 1f, volume, com, new Vector2f());
            Global.getSoundPlayer().playLoop("mote_attractor_loop_dark", ship, 1f, volume, com, Vector2f())
        }
    }
}
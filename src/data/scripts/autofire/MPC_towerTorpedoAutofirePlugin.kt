package data.scripts.autofire

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.AutofireAIPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import org.lwjgl.util.vector.Vector2f

class MPC_towerTorpedoAutofirePlugin(val ourWeapon: WeaponAPI): AutofireAIPlugin {

    var disabled = false
    var shouldFire = false

    override fun advance(amount: Float) {
        /*disabled = false
        val engine = Global.getCombatEngine()
        val weapon = weapon
        val ship = weapon?.ship ?: return

        var incomingDamage = ship.aiFlags.getCustom(ShipwideAIFlags.AIFlags.HAS_INCOMING_DAMAGE) as? Float
        if (incomingDamage != null) {
            val needsHelp = ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)
            if (needsHelp) incomingDamage *= PANICKING_MULT

            if (incomingDamage >= personalitiesToDamageThresholds[ship.captain?.personalityAPI?.id ?: Personalities.STEADY]!!) {
                shouldFire = true
            } else {
                shouldFire = false
            }
        }*/
    }

    override fun shouldFire(): Boolean {
        return false // handled in MPC_towerWpnScript
    }

    override fun forceOff() {
        disabled = true
    }

    override fun getTarget(): Vector2f? {
        return null
    }

    override fun getTargetShip(): ShipAPI? {
        return null
    }

    override fun getWeapon(): WeaponAPI? {
        return ourWeapon
    }

    override fun getTargetMissile(): MissileAPI? {
        return null
    }
}
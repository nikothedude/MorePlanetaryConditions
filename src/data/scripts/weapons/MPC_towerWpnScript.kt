package data.scripts.weapons

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin
import com.fs.starfarer.api.combat.ShipwideAIFlags
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.genir.aitweaks.core.extensions.fluxLeft
import data.scripts.weapons.MPC_collisionUtils.analyzeHit
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

class MPC_towerWpnScript: EveryFrameWeaponEffectPlugin {

    companion object {
        val personalitiesToDamageThresholds = hashMapOf(
            Pair(Personalities.TIMID, 700f),
            Pair(Personalities.CAUTIOUS, 1400f),
            Pair(Personalities.STEADY, 2500f),
            Pair(Personalities.AGGRESSIVE, 3000f),
            Pair(Personalities.RECKLESS, 4000f)
        )

        const val NEEDS_HELP_THRESH_MULT = 0.9f
        const val LOW_FLUX_THRESH_MULT = 2f
        const val CRITICAL_HULL_THRESH_MULT = 0.5f
        const val CRITICAL_FLUX_THRESH_MULT = 0.5f
    }

    val interval = IntervalUtil(0.1f, 0.12f)

    override fun advance(
        amount: Float,
        engine: CombatEngineAPI,
        weapon: WeaponAPI
    ) {
        if (engine.isPaused) return

        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        val ship = weapon.ship ?: return
        if (!ship.isAlive || ship.isHulk || ship.fluxTracker.isOverloaded) return

        if (weapon.ammo <= 0f) return
        if (weapon.cooldownRemaining > 0f) return
        if (ship == engine.playerShip && ship.ai == null) return

        var incomingDamage = 0f
        val facing = weapon.arcFacing + ship.facing
        val okFireAngle = 30f // arbitrary, fixme later
        val wpnLoc = weapon.location
        val iteratedProjectiles = HashSet<DamagingProjectileAPI>()
        for (proj in engine.projectiles.filter { it.owner != ship.owner }) {
            val hit = analyzeHit(proj, ship)
            if (hit == null) {
                if (Global.getSettings().isDevMode) engine.addFloatingText(proj.location, "NO HIT", 10f, Color.CYAN, proj, 0f, 0f)
                continue
            }
            if (hit.target != ship) {
                if (Global.getSettings().isDevMode) engine.addFloatingText(proj.location, "NON-TARGET", 10f, Color.GREEN, proj, 0f, 0f)
                continue
            }

            val projAngle = Misc.normalizeAngle(proj.facing - 180)

            if (projAngle >= facing - okFireAngle && projAngle <= facing + okFireAngle) {
                var damage = proj.damage.damage
                if (proj.damageType == DamageType.FRAGMENTATION) damage *= 0.5f
                if (proj.customData["MPC_towerTorpAlreadyCountered"] == true) damage *= 0.3f
                incomingDamage += damage

                iteratedProjectiles += proj

                if (Global.getSettings().isDevMode) engine.addFloatingText(proj.location, "ANGLE OK", 10f, Color.RED, proj, 0f, 0f)

            } else {
                if (Global.getSettings().isDevMode) engine.addFloatingText(proj.location, "OUT OF ANGLE", 20f, Color.WHITE, proj, 0f, 0f)
            }
        }
        
        if (incomingDamage > 0f) {
            val needsHelp = ship.aiFlags.hasFlag(ShipwideAIFlags.AIFlags.NEEDS_HELP)
            var threshold = personalitiesToDamageThresholds[ship.captain?.personalityAPI?.id ?: Personalities.STEADY]!!
            if (needsHelp) threshold *= NEEDS_HELP_THRESH_MULT
            val lowFlux = ship.fluxLevel <= 0.1f
            val criticalHull = ship.hullLevel <= 0.17f
            val criticalFlux = ship.fluxLevel >= 0.87f
            if (ship.shield != null || ship.phaseCloak != null) {
                if (criticalFlux) {
                    threshold *= CRITICAL_FLUX_THRESH_MULT
                } else if (lowFlux) {
                    threshold *= LOW_FLUX_THRESH_MULT
                }
            }
            if (criticalHull) {
                threshold *= CRITICAL_HULL_THRESH_MULT
                threshold = threshold.coerceAtMost((ship.hitpoints * 1.1f + (ship.fluxTracker.maxFlux - ship.fluxTracker.currFlux)))
            }

            if (ship.customData["MPC_towerTorpAlreadyFired"] == true) {
                threshold *= 3f
                ship.setCustomData("MPC_towerTorpAlreadyFired", false)
            }

            threshold *= MathUtils.getRandomNumberInRange(0.9f, 1.1f)
            if (incomingDamage >= threshold) {
                weapon.isForceNoFireOneFrame = false
                weapon.setForceFireOneFrame(true)

                ship.setCustomData("MPC_towerTorpAlreadyFired", true)
                iteratedProjectiles.forEach { it.setCustomData("MPC_towerTorpAlreadyCountered", true) }
            } else {
                weapon.isForceNoFireOneFrame = true
            }
        }
    }
}
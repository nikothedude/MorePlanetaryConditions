package data.scripts.weapons

import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.combat.CombatUtils

open class MPC_interceptScript: OnFireEffectPlugin {
    override fun onFire(projectile: DamagingProjectileAPI?, weapon: WeaponAPI?, engine: CombatEngineAPI?) {
        if (projectile == null || engine == null) return
        if (projectile !is MissileAPI || projectile.ai !is GuidedMissileAI) return

        engine.addPlugin(MPC_interceptMissileScript(projectile, projectile.ai as GuidedMissileAI, engine))
    }

    class MPC_interceptMissileScript(val projectile: MissileAPI, val ai: GuidedMissileAI, val engine: CombatEngineAPI): BaseEveryFrameCombatPlugin() {

        private val SEARCH_RANGE = 10000
        private val DANGER_RANGE = 500
        private val MEMBERS: MutableMap<Int, CombatEntityAPI> = HashMap()
        var ran = false

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (engine.isPaused) {
                return
            }
            if (!engine.isEntityInPlay(projectile) || projectile.isFading || projectile.isFizzling) {
                engine.removePlugin(this)
                return
            }
            val target = ai.target

            if (target is MissileAPI) return
            val newTarget = findRandomMissileWithinRange(projectile)
            ran = true
            if (newTarget == null) return
            ai.target = newTarget
        }

        private fun findRandomMissileWithinRange(missile: MissileAPI): CombatEntityAPI? {
            val source = missile.source
            val closest = AIUtils.getNearestEnemyMissile(source)
            return if (closest != null && MathUtils.isWithinRange(source, closest, (2 * SEARCH_RANGE).toFloat())) {
                //if a missile come too close, or the closest is still far, target this one
                if (MathUtils.isWithinRange(source, closest, DANGER_RANGE.toFloat())) {
                    closest
                } else {
                    //if the missiles are in normal range
                    val epicenter = source.location
                    MEMBERS.clear()
                    MEMBERS[0] = closest
                    var nbKey = 1
                    //seek all nearby missiles, and if they are hostile add them to the hashmap with a entry number
                    for (tmp in CombatUtils.getMissilesWithinRange(epicenter, SEARCH_RANGE.toFloat())) {
                        if (tmp != null && tmp.owner != source.owner) {
                            MEMBERS[nbKey] = tmp
                            nbKey++
                        }
                    }
                    //choose a random integer within the number of entries, and return the coresponding missile
                    val chooser = Math.round(Math.random() * nbKey).toInt()
                    MEMBERS[chooser]
                }
            } else {
                //if no missiles are neaby, try fighters
                MEMBERS.clear()
                var nbKey = 0
                for (tmp in AIUtils.getNearbyEnemies(source, SEARCH_RANGE.toFloat())) {
                    if (tmp != null && (tmp.isDrone || tmp.isFighter) && tmp.owner != source.owner) {
                        MEMBERS[nbKey] = tmp
                        nbKey++
                    }
                }
                null
            }
        }

    }

}
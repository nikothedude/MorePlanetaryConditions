package data.scripts.ai

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import org.lazywizard.lazylib.FastTrig
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.util.MagicRender
import java.awt.Color

class MPC_interceptorMissileAI(val missile: MissileAPI, val launchingShip: ShipAPI?): MissileAIPlugin, GuidedMissileAI {
    private val SEARCH_RANGE = 1000
    private val DANGER_RANGE = 250
    private val DAMPING = 0.1f
    private val EXPLOSION_COLOR = Color(255, 0, 0, 255)
    private val PARTICLE_COLOR = Color(240, 200, 50, 255)
    private val NUM_PARTICLES = 20

    private var engine: CombatEngineAPI? = null
    private var target: CombatEntityAPI? = null
    private val MEMBERS: MutableMap<Int, CombatEntityAPI> = HashMap()
    private var lead: Vector2f? = Vector2f()
    private var timer = 0f
    private var check = 0.25f
    private var launch = true

    //data
    private var MAX_SPEED = 0f

    init {
        MAX_SPEED = missile.maxSpeed
    }

    override fun advance(amount: Float) {
        if (engine !== Global.getCombatEngine()) {
            engine = Global.getCombatEngine()
        }
        if (Global.getCombatEngine().isPaused || missile.isFading || missile.isFizzling) {
            return
        }

        // if there is no target, assign one
        if (target == null || !Global.getCombatEngine().isEntityInPlay(target) || target!!.owner == missile.owner) {
            missile.giveCommand(ShipCommand.ACCELERATE)
            setTarget(findRandomMissileWithinRange(missile))
            return
        }
        timer += amount
        //finding lead point to aim to
        if (launch || timer >= check) {
            launch = false
            timer -= check
            val dist = MathUtils.getDistanceSquared(missile, target)
            if (dist < 1250) {
                proximityFuse()
                return
            }
            check = Math.min(
                0.25f,
                Math.max(
                    0.025f,
                    2 * dist / 4000000
                )
            )
            lead = AIUtils.getBestInterceptPoint(
                missile.location,
                MAX_SPEED,
                target!!.location,
                target!!.velocity
            )
            if (lead == null) {
                lead = target!!.location
            }
        }

        //best velocity vector angle for interception
        var correctAngle = VectorUtils.getAngle(
            missile.location,
            lead
        )

        //velocity angle correction
        val offCourseAngle = MathUtils.getShortestRotation(
            VectorUtils.getFacing(missile.velocity),
            correctAngle
        )
        val correction = (MathUtils.getShortestRotation(
            correctAngle,
            VectorUtils.getFacing(missile.velocity) + 180
        )
                * 0.5f * FastTrig.sin((MathUtils.FPI / 90 * Math.min(Math.abs(offCourseAngle), 45f)).toDouble())
            .toFloat()) //damping when the correction isn't important

        //modified optimal facing to correct the velocity vector angle as soon as possible
        correctAngle = correctAngle + correction

        //turn the missile
        val aimAngle = MathUtils.getShortestRotation(missile.facing, correctAngle)
        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT)
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT)
        }
        missile.giveCommand(ShipCommand.ACCELERATE)

        // Damp angular velocity if we're getting close to the target angle
        if (Math.abs(aimAngle) < Math.abs(missile.angularVelocity) * DAMPING) {
            missile.angularVelocity = aimAngle / DAMPING
        }
    }

    fun getRandomTargetWithinRange(missile: MissileAPI): CombatEntityAPI? {
        val missile = findRandomMissileWithinRange(missile)
        if (missile != null) return missile

        return null
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
            //choose a random integer within the number of entries, and return the coresponding target
            if (nbKey != 0) {
                val chooser = Math.round(Math.random() * nbKey).toInt()
                MEMBERS[chooser]
            } else if (AIUtils.getNearestEnemy(source) != null) {
                AIUtils.getNearestEnemy(source)
            } else {
                null
            }
        }
    }

    fun proximityFuse() {
        val closeMissiles = AIUtils.getNearbyEnemyMissiles(missile, 50f)
        for (cm in closeMissiles) {
            engine!!.applyDamage(
                cm,
                cm.location,
                200 * 800 / MathUtils.getDistanceSquared(missile, target),
                DamageType.FRAGMENTATION,
                0f,
                false,
                true,
                missile
            )
        }
        if (MagicRender.screenCheck(0.5f, missile.location)) {
            engine!!.addHitParticle(
                missile.location,
                Vector2f(),
                100f,
                1f,
                0.25f,
                EXPLOSION_COLOR
            )
            for (i in 0 until NUM_PARTICLES) {
                val axis = Math.random().toFloat() * 360
                val range = Math.random().toFloat() * 100
                engine!!.addHitParticle(
                    MathUtils.getPoint(missile.location, range / 5, axis),
                    MathUtils.getPoint(Vector2f(), range, axis),
                    2 + Math.random().toFloat() * 2,
                    1f,
                    1 + Math.random().toFloat(),
                    PARTICLE_COLOR
                )
            }
        }
        engine!!.applyDamage(
            missile,
            missile.location,
            missile.hitpoints * 2f,
            DamageType.FRAGMENTATION,
            0f,
            false,
            false,
            missile
        )
    }

    override fun getTarget(): CombatEntityAPI? {
        return target
    }

    override fun setTarget(target: CombatEntityAPI?) {
        this.target = target
    }
}


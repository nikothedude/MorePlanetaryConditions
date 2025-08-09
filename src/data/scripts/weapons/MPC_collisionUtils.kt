package data.scripts.weapons

import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import data.scripts.weapons.Direction.Companion.direction
import data.scripts.weapons.RotationMatrix.Companion.rotated
import org.lazywizard.lazylib.ext.isZeroVector
import org.lwjgl.util.vector.Vector2f
import kotlin.Float.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MPC_collisionUtils {
    const val DEGREES_TO_RADIANS: Float = 0.017453292F
    const val RADIANS_TO_DEGREES: Float = 57.29578F

data class QuadSolution(val x1: Float, val x2: Float) {
        val smallerNonNegative: Float?
            get() = when {
                // Equation has no positive solutions.
                x1 < 0f && x2 < 0f -> null

                // x2 is the only positive solution.
                x1 < 0f -> x2

                // x1 is the only positive solution.
                x2 < 0f -> x1

                else -> x1
            }

        val largerNonNegative: Float?
            get() = when {
                // Equation has no positive solutions.
                x1 < 0f && x2 < 0f -> null

                // x2 is the only positive solution.
                x1 < 0f -> x2

                // x1 is the only positive solution.
                x2 < 0f -> x1

                else -> x2
            }
    }

    data class Hit(
        val target: CombatEntityAPI,
        val range: Float,
        val type: Type,
    ) {
        enum class Type {
            SHIELD,
            HULL,
            ALLY,
            ROTATE_BEAM // placeholder type for mock hit used by beams rotating to a new target
        }
    }

    val ShipAPI.timeMult: Float
        get() = mutableStats.timeMult.modifiedValue

    val ShipAPI.timeAdjustedVelocity: Vector2f
        get() = velocity * timeMult

    val CombatEntityAPI.timeAdjustedVelocity: Vector2f
        get() = (this as? ShipAPI)?.timeAdjustedVelocity ?: velocity

    data class BallisticTarget(
        val location: Vector2f,
        val velocity: Vector2f,
        val radius: Float,
        val entity: CombatEntityAPI,
    ) {
        val LinearMotion = LinearMotion(
            position = location,
            velocity = velocity,
        )

        companion object {
            fun collisionRadius(entity: CombatEntityAPI): BallisticTarget {
                return BallisticTarget(
                    entity.location,
                    entity.timeAdjustedVelocity,
                    entity.collisionRadius,
                    entity,
                )
            }

            fun shieldRadius(ship: ShipAPI): BallisticTarget {
                return BallisticTarget(
                    ship.shieldCenterEvenIfNoShield,
                    ship.timeAdjustedVelocity,
                    ship.shieldRadiusEvenIfNoShield,
                    ship,
                )
            }
        }
    }

    /** Weapon attack parameters: accuracy and delay until attack. */
    data class BallisticParams(val accuracy: Float, val delay: Float) {
        companion object {
            val defaultBallisticParams = BallisticParams(1f, 0f)
        }
    }

    /*fun DamagingProjectileAPI.willHitTarget(ship: ShipAPI): Boolean {
        solve()
    }*/

    fun solve(pv: LinearMotion, r: Float): QuadSolution? {
        return solve(pv, 0f, 0f, r, 0f)
    }

    fun solve(pv: LinearMotion, q: Float, w: Float, r: Float, cosA: Float): QuadSolution? {
        return solve(pv.position, pv.velocity, q, w, r, cosA)
    }

    fun solve(p: Vector2f, v: Vector2f, q: Float, w: Float, r: Float, cosA: Float): QuadSolution? {
        val a = (v.x * v.x) + (v.y * v.y) - (w * w)
        val b = (p.x * v.x) + (p.y * v.y) - (w * q) + (w * r * cosA)
        val c = (p.x * p.x) + (p.y * p.y) - (r * r) - (q * q) + (2 * q * r * cosA)

        return quad(a, 2 * b, c)
    }

    fun quad(a: Float, b: Float, c: Float): QuadSolution? {
        // Equation is degenerated to const case.
        if (a == 0f && b == 0f) {
            return null
        }

        // Equation is degenerated to linear case.
        if (a == 0f) {
            val x = -c / b
            return QuadSolution(x, x)
        }

        val delta = b * b - 4 * a * c

        // Equation has no real solutions.
        if (delta < 0) {
            return null
        }

        val d = sqrt(delta)
        val x1 = (-b + d) / (2 * a)
        val x2 = (-b - d) / (2 * a)

        return QuadSolution(min(x1, x2), max(x1, x2))
    }

    val DamagingProjectileAPI.Velocity: Vector2f
        get() {
            // Workaround for vanilla bug where projectile velocity returns
            // zero vector in the frame the energy projectile was spawned.
            if (elapsed == 0f && velocity.isZeroVector()) {
                val shipVelocity = weapon?.ship?.velocity ?: Vector2f()
                return shipVelocity + facing.direction.unitVector * moveSpeed
            }

            return velocity
        }


    val DamagingProjectileAPI.linearMotion: LinearMotion
        get() = LinearMotion(
            position = location,
            velocity = Velocity
        )

    val CombatEntityAPI.linearMotion: LinearMotion
        get() = LinearMotion(
            position = location,
            velocity = timeAdjustedVelocity,
        )

    fun analyzeHit(projectile: DamagingProjectileAPI, target: CombatEntityAPI): Hit? {
        val projectileMotion = projectile.linearMotion - target.linearMotion

        return analyzeHit(projectileMotion, target)
    }

    fun analyzeHit(projectileMotion: LinearMotion, target: CombatEntityAPI): Hit? {
        // Simple circumference collision is enough for missiles and fighters.
        if (target !is ShipAPI) {
            return willHitCircumference(projectileMotion, BallisticTarget.collisionRadius(target))?.let { hitRange ->
                Hit(target, hitRange, Hit.Type.HULL)
            }
        }

        // Check shield hit.
        willHitShield(projectileMotion, target as ShipAPI)?.let { hitRange ->
            return Hit(target, hitRange, Hit.Type.SHIELD)
        }

        // Check collision radius hit before testing bounds, to increases performance.
        willHitCircumference(projectileMotion, BallisticTarget.collisionRadius(target))
            ?: return null

        // Check bounds hit.
        return willHitBounds(projectileMotion, target)?.let { hitRange ->
            Hit(target, hitRange, Hit.Type.HULL)
        }
    }

    private fun willHitShield(projectileMotion: LinearMotion, target: CombatEntityAPI): Float? {
        when {
            target !is ShipAPI -> return null

            target.shield == null -> return null

            target.shield.isOff -> return null
        }

        val shield = target.shield
        val projectileFlightDistance = solve(projectileMotion, shield.radius)?.smallerNonNegative
            ?: return null

        val hitPoint = projectileMotion.positionAfter(projectileFlightDistance)

        return if (shield.isHit(hitPoint)) {
            projectileFlightDistance
        } else {
            null
        }
    }

    fun ShieldAPI.isHit(hitPoint: Vector2f): Boolean {
        return Arc(activeArc, facing.direction).contains(hitPoint)
    }

    private fun willHitBounds(projectileMotion: LinearMotion, target: CombatEntityAPI): Float? {
        return collision(projectileMotion.position, projectileMotion.velocity, target)
    }

    fun collision(position: Vector2f, velocity: Vector2f, target: CombatEntityAPI): Float? {
        // Check if there's a possibility of collision.
        val bounds = target.exactBounds ?: return null

        // Rotate vector coordinates from target frame of
        // reference to target bounds frame of reference.
        val r = (-target.facing.direction).rotationMatrix
        val p = position.rotated(r)
        val v = velocity.rotated(r)

        // Equation for the collision point: [s.p1 + k (s.p2 - s.p1) = p + t v]
        var closest = MAX_VALUE
        bounds.origSegments.forEach { segment ->
            val q = segment.p1 - p
            val d = segment.p2 - segment.p1

            val m = crossProductZ(d, v)
            if (m == 0f) {
                return@forEach // lines are parallel
            }

            val k = crossProductZ(v, q) / m
            if (k < 0 || k > 1) {
                return@forEach // no collision
            }

            val t = crossProductZ(d, q) / m
            if (t >= 0) {
                closest = min(t, closest)
            }
        }

        return if (closest != MAX_VALUE) closest else null
    }

    private fun willHitCircumference(projectileMotion: LinearMotion, target: BallisticTarget): Float? {
        return solve(projectileMotion, target.radius)?.smallerNonNegative
    }

    fun crossProductZ(a: Vector2f, b: Vector2f): Float {
        return a.x * b.y - a.y * b.x
    }


}
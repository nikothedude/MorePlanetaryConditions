package data.scripts.weapons

import data.scripts.weapons.Direction.Companion.direction
import data.scripts.weapons.MPC_collisionUtils.RADIANS_TO_DEGREES
import org.lwjgl.util.vector.Vector2f
import kotlin.math.atan2
import kotlin.math.sqrt

operator fun Vector2f.plus(b: Vector2f): Vector2f {
    return Vector2f(x + b.x, y + b.y)
}

operator fun Vector2f.minus(b: Vector2f): Vector2f {
    return Vector2f(x - b.x, y - b.y)
}

operator fun Vector2f.unaryMinus(): Vector2f {
    return Vector2f(-x, -y)
}

operator fun Vector2f.times(b: Float): Vector2f {
    return Vector2f(x * b, y * b)
}

operator fun Vector2f.div(b: Float): Vector2f {
    return Vector2f(x / b, y / b)
}

fun Vector2f.resized(length: Float): Vector2f {
    return if (isZero) {
        Vector2f()
    } else {
        this * length / this.length
    }
}

fun Vector2f.clampLength(maxLength: Float): Vector2f {
    return if (lengthSquared > maxLength * maxLength) {
        resized(maxLength)
    } else {
        copy
    }
}

val Vector2f.copy: Vector2f
    get() = Vector2f(x, y)


val Vector2f.length: Float
    get() = sqrt(lengthSquared.toDouble()).toFloat()

val Vector2f.lengthSquared: Float
    get() = x * x + y * y

val Vector2f.isZero: Boolean
    get() = x == 0f && y == 0f

val Vector2f.isNonZero: Boolean
    get() = x != 0f || y != 0f

val Vector2f.facing: Direction
    get() = (if (isZero) 0f else atan2(y, x) * RADIANS_TO_DEGREES).direction

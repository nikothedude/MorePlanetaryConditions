package data.scripts.weapons

import data.scripts.weapons.MPC_collisionUtils.DEGREES_TO_RADIANS
import org.lwjgl.util.vector.Vector2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin

@JvmInline
value class Direction private constructor(val degrees: Float) {

    val radians: Float
        get() = degrees * DEGREES_TO_RADIANS

    val rotationMatrix: RotationMatrix
        get() = RotationMatrix(degrees)

    /** Absolute value of the angle; range: [0, 180] */
    val length: Float
        get() = abs(degrees)

    val isZero: Boolean
        get() = degrees == 0f

    /** The sign (`-1.0`, `0.0`, or `1.0`) of the angle in degrees. */
    val sign: Float
        get() = degrees.sign

    val unitVector: Vector2f
        get() {
            val radians = degrees * DEGREES_TO_RADIANS
            val x = cos(radians)
            val y = sin(radians)
            return Vector2f(x, y)
        }

    operator fun plus(other: Direction): Direction {
        return makeDirection(degrees + other.degrees)
    }

    operator fun plus(other: Float): Direction {
        return makeDirection(degrees + other)
    }

    operator fun minus(other: Direction): Direction {
        return makeDirection(degrees - other.degrees)
    }

    operator fun minus(other: Float): Direction {
        return makeDirection(degrees - other)
    }

    operator fun div(f: Float): Direction {
        return makeDirection(degrees / f)
    }

    operator fun times(f: Float): Direction {
        return makeDirection(degrees * f)
    }

    operator fun unaryMinus(): Direction {
        return makeDirection(-degrees)
    }

    override fun toString(): String {
        return degrees.toString()
    }

    companion object {
        private fun makeDirection(degrees: Float): Direction {
            return Direction(degrees - round(degrees / 360) * 360)
        }

        val Float.direction: Direction
            get() = makeDirection(this)
    }
}
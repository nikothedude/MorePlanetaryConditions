package data.utilities

import org.jetbrains.annotations.Contract
import kotlin.math.roundToInt

object niko_MPC_mathUtils {
    @JvmStatic
    @Contract("null -> 0.0")
    fun ensureIsJsonValidFloat(number: Double?): Double {
        if (number == null || number.isNaN() || number.isInfinite()) {
            niko_MPC_debugUtils.log.info("ensureIsJsonValidFloat rectifying invalid float $number to 0.0d")
            return 0.0
        }
        return number
    }

    @JvmStatic
    fun Float.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }

    @JvmStatic
    fun Int.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }


}
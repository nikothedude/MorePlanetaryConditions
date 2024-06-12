package data.utilities

object niko_MPC_stringUtils {
    fun toPercent(num: Float): String {
        return String.format("%.0f", num * 100) + "%"
    }
}
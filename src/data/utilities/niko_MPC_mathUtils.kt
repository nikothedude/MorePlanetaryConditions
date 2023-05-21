package data.utilities

import data.scripts.campaign.econ.conditions.overgrownNanoforge.overgrownNanoforgeCommodityDataStore
import org.jetbrains.annotations.Contract
import org.lazywizard.lazylib.MathUtils
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
    @Contract("null -> 0.0")
    fun Double.ensureIsJsonValidFloat(): Double {
        return niko_MPC_mathUtils.ensureIsJsonValidFloat(this)
    }

    @JvmStatic
    @Contract("null -> 0.0")
    fun Float.ensureIsJsonValidFloat(): Double {
        return niko_MPC_mathUtils.ensureIsJsonValidFloat(this.toDouble())
    }

    @JvmStatic
    fun Float.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }

    @JvmStatic
    fun Int.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }

    inline fun randomlyDistributeBudgetAcrossCommodities(
        entries: Collection<String>,
        remainingScore: Float,
        getMin: (budget: Float, remainingRuns: Int, entry: String) -> Float = { budget, remainingRuns, entry, ->
            if (budget < 0) (budget - remainingRuns).roundToMultipleOf(overgrownNanoforgeCommodityDataStore[entry]!!.cost) else 0f},) : MutableMap<String, Int> {
        val distributedMap = randomlyDistributeNumberAcrossEntries(
            entries,
            remainingScore,
            getMin,
            getMax = { budget: Float, remainingRuns: Int, entry: Any ->
                if (budget > 0) (budget - remainingRuns).roundToMultipleOf(overgrownNanoforgeCommodityDataStore[entry]!!.cost) else 0f},
            modifyScoreForMap = { score: Float, commodityId: Any -> (score/overgrownNanoforgeCommodityDataStore[commodityId]!!.cost).coerceAtLeast(0f) })

        val convertedMap = HashMap<String, Int>()
        for (entry in distributedMap.entries) {
            val commodityId = entry.key as? String ?: continue
            val score = entry.value.toInt()

            convertedMap[commodityId] = score
        }
        return convertedMap
    }
    @JvmOverloads
    @JvmStatic
    /**
     * Randomly distributes [remainingScore] across all items in [entries] into a [HashMap] of type [T] -> [Float], with the value
     * being the assigned value. The cumulative values are guaranteed to sum up to [remainingScore].
     * */
    inline fun <T: Any> randomlyDistributeNumberAcrossEntries(
        entries: Collection<T>,
        remainingScore: Float,
        getMin: (budget: Float, remainingRuns: Int, entry: T) -> Float = { budget, _, _, -> if (budget <= 0) budget else 0f},
        getMax: (budget: Float, remainingRuns: Int, entry: T) -> Float = { budget, _, _ -> if (budget > 0) budget else 0f },
        modifyScoreForMap: (budget: Float, entry: T) -> Float = { budget, _ -> budget.coerceAtLeast(0f) }
    ): HashMap<T, Float> {

        var remainingScore = remainingScore
        var remainingRuns = entries.size
        val entriesToScore: HashMap<T, Float> = HashMap()
        for (entry in entries) {
            remainingRuns--
            if (remainingScore <= 0) {
                entriesToScore[entry] = 0f
                continue
            }
            val min = getMin(remainingScore, remainingRuns, entry)
            if (remainingScore < min) continue
            val max = getMax(remainingScore, remainingRuns, entry).coerceAtLeast(min)
            if (remainingRuns <= 0) { //we're at the end, so we can avoid the math and just assign it
                entriesToScore[entry] = modifyScoreForMap(max, entry)
                break
            }

            val score = MathUtils.getRandomNumberInRange(min, max)
            remainingScore -= score
            entriesToScore[entry] = modifyScoreForMap(score, entry)
        }
        return entriesToScore
    }
}
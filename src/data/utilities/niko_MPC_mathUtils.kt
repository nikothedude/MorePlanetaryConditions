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
    fun Float.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }

    @JvmStatic
    fun Int.roundToMultipleOf(anchor: Float): Float {
        return anchor*((this) / anchor).roundToInt()
    }

    fun randomlyDistributeBudgetAcrossCommodities(
        entries: Collection<Any>,
        remainingScore: Float,
        min: Float = 0f) : MutableMap<Any, Float> {
        return randomlyDistributeNumberAcrossEntries(
            entries,
            remainingScore,
            min,
            getMax = { budget: Float, remainingRuns: Int, entry: Any -> (budget - remainingRuns).roundToMultipleOf(overgrownNanoforgeCommodityDataStore[entry]!!.cost) },
            modifyScoreForMap = { score: Float, commodityId: Any -> (score/overgrownNanoforgeCommodityDataStore[commodityId]!!.cost).coerceAtLeast(0f) })
    }
    @JvmOverloads
    @JvmStatic
    inline fun randomlyDistributeNumberAcrossEntries(
        entries: Collection<Any>,
        remainingScore: Float,
        min: Float = 0f,
        getMax: (budget: Float, remainingRuns: Int, entry: Any) -> Float = { budget, _, _ -> budget },
        modifyScoreForMap: (budget: Float, entry: Any) -> Float = { budget, _ -> budget.coerceAtLeast(0f) }
    ): MutableMap<Any, Float> {

        var remainingScore = remainingScore
        var remainingRuns = entries.size
        val entriesToScore: MutableMap<Any, Float> = HashMap()
        for (entry in entries) {
            remainingRuns--
            if (remainingScore <= 0) {
                entriesToScore[entry] = 0f
                continue
            }
            val max = getMax(remainingScore, remainingRuns, entry)
            if (remainingRuns <= 0) {
                entriesToScore[entry] = max
                break
            }

            val score = MathUtils.getRandomNumberInRange(min, max)
            remainingScore -= score
            entriesToScore[entry] = modifyScoreForMap(score, entry)
        }
        return entriesToScore
    }


}
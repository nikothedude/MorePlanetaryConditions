package data.scripts.campaign.magnetar.crisis.intel.sabotage

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import com.sun.javafx.css.FontUnits.Weight
import data.scripts.campaign.magnetar.crisis.intel.sabotage.MPC_IAIICSabotageCondition.Companion.removeSabotage
import niko.MCTE.utils.MCTE_mathUtils.roundTo
import java.util.*

abstract class MPC_IAIICSabotage(val market: MarketAPI, val params: MPC_IAIICSabotageParams) {

    abstract val baseName: String
    var name: String = generateName()
    val id = "${this.javaClass::getName}_${Misc.genUID()}"

    data class MPC_IAIICSabotageParams(
        var timeLeft: Float,
        val mult: Float = 1f
    )

    abstract fun apply()
    abstract fun unapply()

    abstract fun createDesc(info: TooltipMakerAPI)

    fun adjustTimeLeft(days: Float) {
        params.timeLeft = (params.timeLeft + days).coerceAtLeast(0f)
        if (params.timeLeft <= 0f) {
            remove()
        }
    }
    fun remove() {
        market.removeSabotage(this)
    }

    private fun generateName(): String {
        val picker = WeightedRandomPicker<String>()
        getPossibleNames().forEach { picker.add(it.key, it.value) }
        val picked = picker.pick() ?: "ERROR"
        return picked
    }
    fun getNameWithTimer(): String {
        return "$name (${getTimeLeftString()})"
    }

    fun getTimeLeftString(): String {
        return "${params.timeLeft.roundTo(1)} days remaining"
    }

    abstract fun getPossibleNames(): MutableMap<String, Float>
}
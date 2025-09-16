package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.niko_MPC_baseNikoCondition
import org.magiclib.kotlin.isMilitary
import kotlin.math.floor

class MPC_hegemonyUnrestCondition: niko_MPC_baseNikoCondition() {

    fun getMaxDecrement(): Int {
        var base = 4f
        if (market == null) return 0
        if (market.isMilitary()) return 5
        if (market.id == "eventide") return 6
        if (market.id == "chicomoztoc") return 7

        return floor(base).toInt()
    }

    override fun isTransient(): Boolean = false

    override fun apply(id: String) {
        super.apply(id)

        val unrestLevel = MPC_hegemonyUnrestScript.getUnrestLevel()
        val maxDecrement = getMaxDecrement()

        val decrement = floor(maxDecrement * unrestLevel)
        market.stability.modifyFlat(id, -decrement, name)
    }

    override fun unapply(id: String?) {
        super.unapply(id)

        market.stability.unmodify(id)
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        if (tooltip == null) return

        val stability = floor(getMaxDecrement() * MPC_hegemonyUnrestScript.getUnrestLevel()).toInt()

        if (stability > 0) {
            tooltip.addPara(
                "%S stability",
                5f,
                Misc.getNegativeHighlightColor(),
                "-${stability}"
            )

            if (market.id == "eventide") {
                tooltip.addPara(
                    "The unrest is especially bad on Eventide, the home of the aristocracy. Not an officer can find harbor without " +
                            "being sneered at.",
                    5f
                )
            }
            if (market.id == "chicomoztoc") {
                tooltip.addPara(
                    "Chicomoztoc, the industrial heartland of the Hegemony, especially struggles to withstand the onslaught.",
                    5f
                )
            }
        } else {
            tooltip.addPara(
                "The unrest has not yet spread to ${market.name} - but it is only a matter of time before the populace listens closer to the talking heads.",
                5f
            )
        }
    }

}
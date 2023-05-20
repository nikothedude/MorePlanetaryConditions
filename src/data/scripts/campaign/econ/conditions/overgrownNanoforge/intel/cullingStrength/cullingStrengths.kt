package data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength

import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.MEDIUM_SCORE_THRESHOLD
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_EXTREME
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_HIGH
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_LOW
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_MEDIUM
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_MINIMAL
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_VERY_HIGH
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.cullingStrength.cullingStrengthsValues.THRESHOLD_MULT_VERY_LOW
import java.awt.Color

object cullingStrengthsValues {
    const val THRESHOLD_MULT_MINIMAL = 0.25f

    const val THRESHOLD_MULT_VERY_LOW = 0.5f
    const val THRESHOLD_MULT_LOW = 0.75f
    const val THRESHOLD_MULT_MEDIUM = 1f
    const val THRESHOLD_MULT_HIGH = 1.25f
    const val THRESHOLD_MULT_VERY_HIGH = 1.50f
    const val THRESHOLD_MULT_EXTREME = 1.75f
    const val MEDIUM_SCORE_THRESHOLD = 200f
}

enum class cullingStrengths(
    val color: Color,
    val ourName: String,
    val scoreThreshold: Float
) {
    NONE(Misc.getNegativeHighlightColor(), "Minimal", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_MINIMAL),
    VERY_LOW(Misc.getNegativeHighlightColor(), "Very Low", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_VERY_LOW),
    LOW(Misc.getNegativeHighlightColor(), "Low", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_LOW),
    MEDIUM(Misc.getHighlightColor(), "Medium", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_MEDIUM),
    HIGH(Misc.getPositiveHighlightColor(), "High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_HIGH),
    VERY_HIGH(Misc.getPositiveHighlightColor(), "Very High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_VERY_HIGH),
    EXTREMELY_HIGH(Misc.getPositiveHighlightColor(), "Extremely High", MEDIUM_SCORE_THRESHOLD * THRESHOLD_MULT_EXTREME);

    companion object {
        fun getStrengthFromScore(score: Float): cullingStrengths {
            var highestThreshold = NONE
            for (entry in values()) {
                if (entry.scoreThreshold > score) continue
                if (entry.scoreThreshold < highestThreshold.scoreThreshold) continue

                highestThreshold = entry
            }
            return highestThreshold
        }
    }
}

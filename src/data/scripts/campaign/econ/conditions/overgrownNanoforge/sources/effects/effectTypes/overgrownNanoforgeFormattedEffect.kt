package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories

abstract class overgrownNanoforgeFormattedEffect(
    handler: overgrownNanoforgeHandler
): overgrownNanoforgeRandomizedEffect(handler), simpleFormat {

    override fun getDescription(): String {
        return formatDesc(getBaseFormat(), isPositive())
    }

    override fun getHighlights(): MutableMap<String, Color> {
        val superValue = super.getHighlights()
        superValue[getAdjective(isPositive())] = getPositiveOrNegativeColor()
        superValue[getChange(isPositive())] = getPositiveOrNegativeColor()
        return superValue
    }

    override fun getAdjectiveCharReplacement(positive: Boolean): String = "%s"
    override fun getChangeCharReplacement(positive: Boolean): String = "%s"

    abstract fun getBaseFormat(): String
}
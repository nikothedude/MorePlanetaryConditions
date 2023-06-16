package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import java.awt.Color

abstract class overgrownNanoforgeFormattedEffect(
    handler: overgrownNanoforgeHandler
): overgrownNanoforgeRandomizedEffect(handler), simpleFormat {

    override fun getDescription(): String {
        return formatDesc(getBaseFormat(), isPositive())
    }

    /*override fun getAdjectiveCharReplacement(positive: Boolean): String = "%s"
    override fun getChangeCharReplacement(positive: Boolean): String = "%s"*/

    abstract fun getBaseFormat(): String
}
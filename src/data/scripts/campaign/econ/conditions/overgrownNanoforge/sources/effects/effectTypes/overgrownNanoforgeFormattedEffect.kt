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

    abstract fun getBaseFormat(): String
}
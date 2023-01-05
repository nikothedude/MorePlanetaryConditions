package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

abstract class overgrownNanoforgeEffect(
    val nanoforge: overgrownNanoforgeIndustry
) {

    abstract fun getCategory(): overgrownNanoforgeEffectCategories
    abstract fun getName(): String
    abstract fun getDescription(): String

    abstract fun apply()
    abstract fun unapply()

    open fun delete() {
        return
    }

    fun getMarket(): MarketAPI = nanoforge.market
    fun getIndustry(): overgrownNanoforgeIndustry = nanoforge
    open fun getId(): String = nanoforge.toString() + this.toString()
}
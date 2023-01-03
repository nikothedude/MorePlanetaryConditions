package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeSupplyData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

abstract class overgrownNanoforgeEffectSource(
    val data: overgrownNanoforgeRandomizedEffect = HashSet(),
    val industry: overgrownNanoforgeIndustry,
    val id: Any,
    ) {
    open fun delete() {
        unapply()
        return
    }

    open fun init() {
        return
    }

    open fun apply() {
        industry.sources += this
    }

    open fun unapply() {
        industry.sources -= this
    }

    fun getMarket(): MarketAPI {
        return industry.market
    }

    open fun getConvertedId(): String {
        return id.toString()
    }

    open fun getDesc(): String {
        return industry.currentName
    }

}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

abstract class overgrownNanoforgeEffectSource(
    val industry: overgrownNanoforgeIndustry,
    val effects: MutableSet<overgrownNanoforgeEffect>
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
        applyEffects()
    }

    fun applyEffects() {
        for (effect in effects) effect.apply()
    }

    open fun unapply(unapplyEffects: Boolean = true) {
        industry.sources -= this
        if (unapplyEffects) unapplyEffects()
    }

    fun unapplyEffects() {
        for (effect in effects) effect.unapply()
    }

    fun getMarket(): MarketAPI {
        return industry.market
    }

    fun getId(): String {
        return this.toString()
    }

    open fun getDesc(): String {
        return industry.currentName
    }

}
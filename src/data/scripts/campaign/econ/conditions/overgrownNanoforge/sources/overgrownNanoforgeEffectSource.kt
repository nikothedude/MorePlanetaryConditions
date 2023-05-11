package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

abstract class overgrownNanoforgeEffectSource(
    val handler: overgrownNanoforgeHandler,
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
        applyEffects()
    }

    fun applyEffects() {
        for (effect in effects) effect.apply()
    }

    open fun unapply(unapplyEffects: Boolean = true) {
        if (unapplyEffects) unapplyEffects()
    }

    fun unapplyEffects() {
        for (effect in effects) effect.unapply()
    }

    fun getMarket(): MarketAPI {
        return handler.market
    }

    fun getId(): String {
        return this.toString()
    }

    open fun getDesc(): String {
        return handler.getCurrentName()
    }

}
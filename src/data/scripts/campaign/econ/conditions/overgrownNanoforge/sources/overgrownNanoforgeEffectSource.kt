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

    fun getEffectsOfCategory(category: overgrownNanoforgeEffectCategories): MutableSet<overgrownNanoforgeEffect> {
        val effectsOfCategory: MutableList<overgrownNanoforgeEffect> = HashSet()

        for (effect in effects) {
            if (effect.getCategory() == category) {
                effectsOfCategory += effect
            }
        }
        return effectsOfCategory
    }

    open fun delete() {
        unapply()
        return
    }

    open fun init() {
        return
    }

    fun apply() {
        updateEffects()
    }

    private fun updateEffects() {
        if (!shouldApplyPositives()) {
            unapplyPositives()
        } else {
            applyPositives()
        }
        if (!shouldApplyNegatives()) {
            unapplyNegatives()
        } else {
            applyNegatives()
        }
    }

    fun applyPositives() {
        applyEffects(overgrownNanoforgeEffectCategories.BENEFIT)
    }

    fun unapplyPositives() {
        unapplyEffects(overgrownNanoforgeEffectCategories.BENEFIT)
    }

    fun applyNegatives() {
        applyEffects(overgrownNanoforgeEffectCategories.DEFICIT)
    }

    fun unapplyNegatives() {
        unapplyEffects(overgrownNanoforgeEffectCategories.DEFICIT)
    }

    fun applyEffects(category: overgrownNanoforgeEffectCategories) {
        for (effect in getEffectsOfCategory(category)) {
            effect.apply()
        }
    }
    
    fun unapplyEffects(category: overgrownNanoforgeEffectCategories) {
        for (effect in getEffectsOfCategory(category)) {
            effect.unapply()
        }
    }

    fun unapply(unapplyEffects: Boolean = true) {
        if (unapplyEffects) {
            unapplyAllEffects()
        }
    }

    fun unapplyAllEffects() {
        for (effect in effects) {
            effect.unapply()
        }
    }

    fun shouldApplyPositives(): Boolean {
        if (getMarket().exceedsMaxStructures()) return false
        val negativeBlocking = isNegativeEffectBlocking()

        return !negativeBlocking
    }

    fun shouldApplyNegatives(): Boolean = true

    fun isNegativeEffectBlocking(): Boolean {
        val negativeEffects = getEffectsOfCategory(overgrownNanoforgeEffectCategories.DEFICIT)
        for (negativeEffect in negativeEffects) {
            val blocking = negativeEffect.shouldBlockPositives()
            if (blocking) return true
        }
        return false
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
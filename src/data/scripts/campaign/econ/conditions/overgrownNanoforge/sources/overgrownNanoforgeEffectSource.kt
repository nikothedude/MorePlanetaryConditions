package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffectDescData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures

abstract class overgrownNanoforgeEffectSource(
    val handler: overgrownNanoforgeHandler,
    val effects: MutableSet<overgrownNanoforgeEffect>
    ) {

    fun getEffectsOfCategory(category: overgrownNanoforgeEffectCategories): MutableSet<overgrownNanoforgeEffect> {
        val effectsOfCategory: MutableSet<overgrownNanoforgeEffect> = HashSet()

        for (effect in effects) {
            if (effect.getCategory() == category) {
                effectsOfCategory += effect
            }
        }
        return effectsOfCategory
    }

    open fun delete() {
        for (effect in effects) effect.delete()
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
            effect.apply(this)
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

    fun shouldApplyCategory(category: overgrownNanoforgeEffectCategories): Boolean {
        return when (category) {
            overgrownNanoforgeEffectCategories.BENEFIT -> shouldApplyPositives()
            overgrownNanoforgeEffectCategories.DEFICIT -> shouldApplyNegatives()
            else -> true
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
            val blocking = negativeEffect.shouldBlockPositives(this)
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

    fun getEffectDescData(category: overgrownNanoforgeEffectCategories): MutableList<overgrownNanoforgeEffectDescData> {
        val data: MutableList<overgrownNanoforgeEffectDescData> = ArrayList()
        for (effect in getEffectsOfCategory(category)) {
            data += effect.getDescData(this)
        }
        return data
    }

    fun getCategoryDisabledReasons(): MutableMap<String, Array<String>> {
        val reasons = HashMap<String, Array<String>>()
        if (getMarket().exceedsMaxStructures()) {
            reasons["Because %s exceeds max structures, %s."] = arrayOf(getMarket().name, "benefits are disabled")
        }
        if (isNegativeEffectBlocking()) {
            reasons["Benefits are disabled due to a negative that blocks positives on disable being disabled."] = emptyArray()
        }

        return reasons
    }

}
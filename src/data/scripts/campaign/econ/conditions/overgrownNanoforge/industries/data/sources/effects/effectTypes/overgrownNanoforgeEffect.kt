package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures

abstract class overgrownNanoforgeEffect(
    val nanoforge: overgrownNanoforgeIndustry
) {

    abstract fun getCategory(): overgrownNanoforgeEffectCategories
    abstract fun getName(): String
    abstract fun getDescription(): String

    open fun apply() {
        if (!getMarket().exceedsMaxStructures()) applyBenefits()
        applyDeficits()
    }

    abstract fun applyBenefits()
    abstract fun applyDeficits()

    open fun unapply() {
        unapplyBenefits()
        unapplyDeficits()
    }

    abstract fun unapplyBenefits()
    abstract fun unapplyDeficits()

    open fun delete() {
        unapply()
    }

    fun getMarket(): MarketAPI = nanoforge.market
    fun getIndustry(): overgrownNanoforgeIndustry = nanoforge
    open fun getId(): String = nanoforge.toString() + this.toString()
}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures

abstract class overgrownNanoforgeEffect(
    val handler: overgrownNanoforgeHandler
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

    fun getMarket(): MarketAPI = handler.market
    fun getStructure(): baseOvergrownNanoforgeStructure? = handler.getStructure()
    open fun getId(): String = handler.toString() + this.toString()
}
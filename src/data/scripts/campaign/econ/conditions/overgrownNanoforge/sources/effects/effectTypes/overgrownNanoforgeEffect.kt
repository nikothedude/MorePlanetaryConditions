package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_marketUtils.exceedsMaxStructures

abstract class overgrownNanoforgeEffect(
    val handler: overgrownNanoforgeHandler,
    val id: overgrownNanoforgeEffectSource
): simpleFormat {

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
    open fun getId(): String = this.toString()
    open fun getNameForModifier(): String = "${handler.getCurrentName()}: ${getName()}"

    override fun getFormattedEffect(format: String, positive: Boolean, vararg args: Any): String {
        var effect = super.getFormattedEffect(format, positive, *args)
        if (effect.isNotEmpty()) effect = "${getName()}: $effect"
        return effect
    }

    companion object {
        fun format(formattedEffects: MutableList<String>): String {
            var addNewLine = false
            var finalString = ""

            for (string in formattedEffects) {
                var mutatedString = string
                if (addNewLine) {
                    mutatedString = "\n" + mutatedString
                }
                addNewLine = true // the first string gets no newline
                finalString += mutatedString
            }
            if (finalString.isEmpty()) finalString = "None"
            return finalString
        }
    }
}
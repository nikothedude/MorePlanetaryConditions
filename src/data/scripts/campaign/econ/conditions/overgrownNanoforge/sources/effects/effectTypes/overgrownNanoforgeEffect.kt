package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_miscUtils.formatStringsToLines
import java.awt.Color

abstract class overgrownNanoforgeEffect(
    open var handler: overgrownNanoforgeHandler
) {

    open var id: String = Misc.genUID()

    /*lateinit var cachedCategory: overgrownNanoforgeEffectCategories

    open fun init() {
        cachedCategory = getCategory()
    }*/

    // Should only have a single noticable effect, should not mix positives or negatives

    abstract fun getCategory(): overgrownNanoforgeEffectCategories

    fun getDescData(hostSource: overgrownNanoforgeEffectSource): overgrownNanoforgeEffectDescData {
        return overgrownNanoforgeEffectDescData(
            getEffectDescription(hostSource),
            getDisabledCriteriaString(),
            blockPositivesWhenDisabled(),
        )
    }
    fun getEffectDescription(hostSource: overgrownNanoforgeEffectSource): String = "${getName()}: ${getDisabledString(hostSource)} ${getDescription()}"

    private fun getDisabledString(hostSource: overgrownNanoforgeEffectSource): String {
        return if (isDisabled(hostSource)) "(DISABLED)" else ""
    }

    abstract fun getName(): String
    abstract fun getDescription(): String

    fun getPositiveOrNegativeColor(): Color {
        return if (isPositive()) Misc.getPositiveHighlightColor() else Misc.getHighlightColor()
    }

    open fun apply(hostSource: overgrownNanoforgeEffectSource) {

        if (shouldApply(hostSource)) {
            applyEffects()
        }
    }

    protected open fun shouldApply(hostSource: overgrownNanoforgeEffectSource): Boolean = (!isDisabled(hostSource))

    fun isDisabled(hostSource: overgrownNanoforgeEffectSource): Boolean {
        return (!hostSource.shouldApplyCategory(getCategory()) || getDisabledCriteria(hostSource))
    }

    protected abstract fun applyEffects()

    open fun unapply() {
        unapplyEffects()
    }

    protected abstract fun unapplyEffects()

    open fun delete() {
        unapply()
    }

    fun getMarket(): MarketAPI = handler.market
    open fun getStructure(): baseOvergrownNanoforgeStructure? = handler.getStructure()
    open fun getOurId(): String = id
    open fun getNameForModifier(): String = "${handler.getCurrentName()}: ${getName()}"

    fun isPositive(): Boolean {
        return getCategory() == overgrownNanoforgeEffectCategories.BENEFIT
    }

    fun shouldBlockPositives(hostSource: overgrownNanoforgeEffectSource): Boolean {
        if (isPositive()) return false
        if (!blockPositivesWhenDisabled()) return false

        return getDisabledCriteria(hostSource)
    }

    /** Used for determining if we should disable ourselves. */
    open fun getDisabledCriteria(hostSource: overgrownNanoforgeEffectSource): Boolean { return false }
    /** If [getDisabledCriteria] returns true, this is what controls the disabling of positive effects. */
    open fun blockPositivesWhenDisabled(): Boolean {
        if (isPositive()) return false

        return true
    }

    /** Should return a user-facing description of [getDisabledCriteria], if applicable. */
    protected open fun getDisabledCriteriaString(): String? { return null }
}

class overgrownNanoforgeEffectDescData(
    val description: String,
    protected val disablingCriteria: String? = null,
    protected val disablePositives: Boolean = false
) {

    protected fun canDisable(): Boolean = (disablingCriteria != null)

    fun getFullString(): String {
        var fullString = description
        val disableText = getDisableText()
        if (disableText != null) {
            fullString += "\n $disableText"
        }
        return fullString
    }

    fun getDisableText(): String? {
        if (!canDisable()) return null

        var first = "   Can be disabled if $disablingCriteria, which ${getBaseDisableConsequences()}"
        val extraConsequences = (getExtraDisableConsequences())
        var extraConsequencesString = ""
        if (extraConsequences != NO_CONSEQUENCE_STRING) {
            extraConsequencesString += "    Extra effects on disabled: \n " +
                                        "       $extraConsequences"
        }

        var finalString = first
        if (extraConsequencesString.isNotEmpty()) {
            finalString += "\n $extraConsequencesString"
        }

        return finalString
    }

    protected fun getBaseDisableConsequences(): String {
        return "forces it to stop applying its effects."
    }

    protected fun getExtraDisableConsequences(): String {
        var strings: MutableList<String> = ArrayList()

        if (disablePositives) {
            strings += "Positive effects will be disabled"
        }

        if (strings.isEmpty()) return NO_CONSEQUENCE_STRING
        return formatStringsToLines(strings)
    }

    companion object {
        const val NO_CONSEQUENCE_STRING: String = ""
    }
}
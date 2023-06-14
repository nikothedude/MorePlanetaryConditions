package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories

abstract class overgrownNanoforgeEffect(
    open val handler: overgrownNanoforgeHandler
) {

    open var id: String = Misc.genUID()

    var enabled: Boolean = true
        set(value) {
            val oldField = field
            field = value
            if (value != oldField) {
                if (value) {
                    apply()
                } else unapply()
            }
        }
    /*lateinit var cachedCategory: overgrownNanoforgeEffectCategories

    open fun init() {
        cachedCategory = getCategory()
    }*/

    // Should only have a single noticable effect, should not mix positives or negatives

    abstract fun getCategory(): overgrownNanoforgeEffectCategories
    abstract fun getName(): String
    abstract fun getDescription(): String

    fun getStringData(): stringData {
        return stringData(getDescription, getHighlights())
    }

    open fun getHighlights(): MutableMap<String, Color> {
        return hashMapOf(Pair<String, Color>(getName(), getNameHighlight()))
    }

    fun getNameHighlight(): Color {
        return getPositiveOrNegativeColor()
    }

    fun getPositiveOrNegativeColor(): Color {
        return if (isPositive()) Misc.getPositiveHighlightColor else Misc.getHighlightColor()
    }

    open fun apply() {

        if (shouldApply()) {
            applyEffects()
        }
    }

    //TODO: market max strcuture check
    protected open fun shouldApply(): Boolean = (enabled)

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
package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes

interface simpleFormat {
    open val positiveAdjective: String
        get() = "increased"
    open val negativeAdjective: String
        get() = "decreased"

    open val adjectiveChar: String
        get() = "%b"
    open val changeChar: String
        get() = "%c"

    abstract val baseFormat: String

    open fun getAllFormattedEffects(positive: Boolean): MutableList<String> {
        return arrayListOf(getFormattedEffect(positive = positive))
    }

    open fun getFormattedEffect(format: String = baseFormat, positive: Boolean, vararg args: Any): String {
        val adjectiveReplaced = format.replace(adjectiveChar, getAdjective(positive))
        return adjectiveReplaced.replace(changeChar, getChange(positive, *args))
    }

    fun getChange(positive: Boolean, vararg args: Any): String

    fun getAdjective(positive: Boolean): String {
        return if (positive) positiveAdjective else negativeAdjective
    }
}
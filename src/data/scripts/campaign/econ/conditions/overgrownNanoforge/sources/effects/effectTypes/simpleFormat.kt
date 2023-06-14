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

    open fun formatDesc(format: String, positive: Boolean): String {
        val adjectiveReplaced = format.replace(adjectiveChar, getAdjectiveCharReplacement(positive))
        return adjectiveReplaced.replace(changeChar, getChangeCharReplacement(positive))
    }

    open fun getAdjectiveCharReplacement(positive: Boolean): String = getAdjective(positive)
    open fun getChangeCharReplacement(positive: Boolean): String = getChange(positive)

    fun getChange(positive: Boolean): String

    fun getAdjective(positive: Boolean): String {
        return if (positive) positiveAdjective else negativeAdjective
    }
}
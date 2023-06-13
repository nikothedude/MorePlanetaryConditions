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

    fun formatDesc(format: String, positive: Boolean): String {
        val adjectiveReplaced = format.replace(adjectiveChar, getAdjective(positive))
        return adjectiveReplaced.replace(changeChar, getChange(positive))
    }

    fun getChange(positive: Boolean): String

    fun getAdjective(positive: Boolean): String {
        return if (positive) positiveAdjective else negativeAdjective
    }
}
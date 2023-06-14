package data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects

class stringData(
    var string: String = "",
    highlights: MutableMap<String, String> = HashMap()
) {

    private val highlights = highlights

    fun computeStringWidth(info: TooltipMakerAPI): Float {
        return 
    }

    fun addHighlight(
        highlighted: String,
        color: Color = Misc.getHighlightColor()
    ) {
        highlights[highlighted] = color
    }
    fun getHighlights(): Map<String, String> = highlights.toMap()

    fun format(data: MutableSet<stringData>): String {
        var addNewLine = false
        var finalString = ""

        for (entry in data) {
            val string = entry.data
            if (addNewLine) {
                string = "\n" + string
            }
            addNewLine = true // the first string gets no newline
            finalString += string
        }
        if (finalString.isEmpty()) finalString = "None"
        return finalString
    }
}
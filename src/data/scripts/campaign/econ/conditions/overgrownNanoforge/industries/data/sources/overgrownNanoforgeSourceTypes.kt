package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeSupplyData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

enum class overgrownNanoforgeSourceTypes(
    val chance: Float
) {
    INTERNAL(95f),
    STRUCTURE(5f)
}

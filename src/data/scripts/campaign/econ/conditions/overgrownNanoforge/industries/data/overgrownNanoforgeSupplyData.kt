package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeSupplyData(
    val id: Any,
    val commodites: MutableMap<String, Int>,
    val demand: MutableMap<String, overgrownNanoforgeDemandData> = HashMap(),
    val nanoforge: overgrownNanoforgeIndustry,
) {

}
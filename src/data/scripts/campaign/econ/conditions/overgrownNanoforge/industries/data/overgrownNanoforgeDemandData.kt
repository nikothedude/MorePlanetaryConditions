package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeDemandData(
    val id: Any,
    val commodites: MutableMap<String, Int>,
    val supply: MutableMap<String, overgrownNanoforgeSupplyData> = HashMap(),
    val nanoforge: overgrownNanoforgeIndustry,
) {
}
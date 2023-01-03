package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceIds.OVERGROWN_NANOFORGE_BASE_SOURCE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeIndustrySource(
    data: MutableSet<overgrownNanoforgeSupplyData>,
    industry: overgrownNanoforgeIndustry,
): overgrownNanoforgeEffectSource(data, industry, OVERGROWN_NANOFORGE_BASE_SOURCE) {

}

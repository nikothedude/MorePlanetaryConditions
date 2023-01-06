package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.overgrownNanoforgeSourceIds.OVERGROWN_NANOFORGE_BASE_SOURCE
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeIndustrySource(
    industry: overgrownNanoforgeIndustry, effects: MutableSet<overgrownNanoforgeEffect>,
): overgrownNanoforgeEffectSource(industry, effects) {

}

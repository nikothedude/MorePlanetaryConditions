package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data

import data.scripts.campaign.econ.conditions.overgrownNanoforge.handler.overgrownNanoforgeIndustryHandler
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry

class overgrownNanoforgeIndustrySource(
    handler: overgrownNanoforgeIndustryHandler, effects: MutableSet<overgrownNanoforgeEffect>,
): overgrownNanoforgeEffectSource(handler, effects) {

}

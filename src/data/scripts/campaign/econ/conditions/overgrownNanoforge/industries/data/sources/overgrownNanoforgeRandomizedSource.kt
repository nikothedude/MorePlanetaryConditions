package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.effectTypes.overgrownNanoforgeRandomizedEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_marketUtils.addJunkStructure
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkId

// More of a "holder class" for a list of effects, which we apply one by one.
// The source isn't the star of the show, it's the source of the effects, meaning the effects are the big boys here.
class overgrownNanoforgeRandomizedSource(
    nanoforge: overgrownNanoforgeIndustry,
    val params: overgrownNanoforgeRandomizedSourceParams,
    effects: MutableSet<overgrownNanoforgeEffect> = params.effects,
): overgrownNanoforgeEffectSource(nanoforge, effects) {

    var structure: overgrownNanoforgeJunk? = null

    override fun init() {
        super.init()
        if (params.type == overgrownNanoforgeSourceTypes.STRUCTURE) {
            val id = getMarket().getNextOvergrownJunkId() ?: return displayError("SOMETHING HAS GONE TERRIBLY WRONG")
            structure = getMarket().addJunkStructure(id, this)
        }
    }
}

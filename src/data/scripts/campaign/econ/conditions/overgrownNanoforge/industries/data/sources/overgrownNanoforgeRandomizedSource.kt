package data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources

import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeSupplyData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.sources.effects.overgrownNanoforgeSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.utilities.niko_MPC_marketUtils.addJunkStructure
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkId
import niko.MCTE.utils.MCTE_debugUtils.displayError

class overgrownNanoforgeRandomizedSource(
    data: MutableSet<overgrownNanoforgeSupplyData> = HashSet(),
    nanoforge: overgrownNanoforgeIndustry,
    id: Any,
    val params: overgrownNanoforgeRandomizedSourceParams
): overgrownNanoforgeEffectSource(data, nanoforge, id) {

    var structure: overgrownNanoforgeJunk? = null

    override fun init() {
        super.init()
        if (params.type == overgrownNanoforgeSourceTypes.STRUCTURE) {
            val id = getMarket().getNextOvergrownJunkId() ?: return displayError("SOMETHING HAS GONE TERRIBLY WRONG")
            structure = getMarket().addJunkStructure(id, this)
        }
    }

    override fun apply() {
        super.apply()

        val market = getMarket()
        val convertedId = getConvertedId()
        val desc = getDesc()
        market.stability.modifyFlatAlways(convertedId, params.stabilityIncrement, desc)
        market.accessibilityMod.modifyFlatAlways(getConvertedId(), params.accessibilityIncrement, desc)
        market.hazard
    }
}

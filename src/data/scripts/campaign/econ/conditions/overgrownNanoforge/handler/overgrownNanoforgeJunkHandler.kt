package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkId
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler

// The data store and "permament" representation of junk spawned by a overgrown nanoforge

// WHAT THIS CLSAS SHOULD HOLD
// 1. A source we apply when needed
// 2. Okay just everything the junk should hold
class overgrownNanoforgeJunkHandler(
    initMarket: MarketAPI,
    val masterHandler: overgrownNanoforgeIndustryHandler,
    initBuildingId: String?
): overgrownNanoforgeHandler(initMarket) {

    /* Should be the String ID of the building we currently have active, or will instantiate later. */
    var cachedBuildingId: String? = initBuildingId ?: getNewStructureId()
        set(value: String?) {
            if (value == null) {
                handleNullBuildingId()
            }
            field = value
        }

    private fun handleNullBuildingId() {
        delete()
    }

    override fun createStructure() {
        super.createStructure()
        cachedBuildingId = currentStructureId
    }

    // Shouldn't cause issues, since this is only called during the building's instantiation, right? Riiiiiight?
    // No we still need to keep a copy of our structure ID so we can actually grab it huhgh
    override fun getNewStructureId(): String? {
        return market.getNextOvergrownJunkId()
    }

    override fun getCoreHandler(): overgrownNanoforgeIndustryHandler {
        return masterHandler
    }

    override fun createBaseSource(): overgrownNanoforgeEffectSource {
        val sourceType = overgrownNanoforgeSourceTypes.adjustedPick() ?: overgrownNanoforgeSourceTypes.STRUCTURE
        val params = overgrownNanoforgeRandomizedSourceParams(masterHandler, sourceType)
        return overgrownNanoforgeRandomizedSource(masterHandler, params)
    }

    override fun getStructure(): overgrownNanoforgeJunk? {
        return (market.getIndustry(currentStructureId) as? overgrownNanoforgeJunk)
    }
}
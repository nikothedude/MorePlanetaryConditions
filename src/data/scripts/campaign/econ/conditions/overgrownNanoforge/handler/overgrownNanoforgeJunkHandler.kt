package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

// The data store and "permament" representation of junk spawned by a overgrown nanoforge

// WHAT THIS CLSAS SHOULD HOLD
// 1. A source we apply when needed
// 2. Okay just everything the junk should hold
class overgrownNanoforgeJunkHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeRandomizedSource,
    initBuildingId: String?
): overgrownNanoforgeHandler(initMarket, initBaseSource) {

    var buildingId: String? = initBuildingId ?: getNewStructureId()
        set(value: String?) {
            if (value == null) {
                handleNullBuildingId()
            }
            field == value
        }

    override fun init() {
        if (initBuildingId)
    }

    override fun apply() {
        super.apply()
        buildingId = getStructure()?.id
    }

    // Shouldn't cause issues, since this is only called during the building's instantiation, right? Riiiiiight?
    // No we still need to keep a copy of our structure ID so we can actually grab it huhgh
    override fun getNewStructureId(): String? {
        return market.getNextOvergrownJunkId()
    }
}
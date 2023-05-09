package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

// The data store and "permament" representation of junk spawned by a overgrown nanoforge

// WHAT THIS CLSAS SHOULD HOLD
// 1. A source we apply when needed
// 2. Okay just everything the junk should hold
class overgrownNanoforgeJunkHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeRandomizedSource
): overgrownNanoforgeHandler(initMarket, initBaseSource) {
    override fun init() {

    }
}
package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

/* The structure that creates this isnt guranteed to exist, this class exists to store data between decivs and shit so it's "consistant" */

// This should be considered the actual overgrown nanoforge structure. The building itself is only our hook into the API.
abstract class overgrownNanoforgeHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeEffectSource
) {
    val baseSource: overgrownNanoforgeEffectSource = initBaseSource

    var market: MarketAPI? = initMarket
        set(value: MarketAPI?) {
            if (field != null && value == null) {
                migratingToNullMarket()
            }
            field = value
        }

    open fun init() {
        addSelfToMarket(market)
    }

    open fun delete() {
        removeSelfFromMarket(market)
    }

    abstract fun addSelfToMarket(market: MarketAPI): Boolean
    abstract fun removeSelfFromMarket(market: MarketAPI): Boolean

    /** Called BEFORE market is made null. */
    open fun migratingToNullMarket() {
        val ourMarket = market ?: return
        removeSelfFromMarket(ourMarket)
    }

    /* Returns the structure this handler stores data of. Can be null if the structure doesn't exist. */
    abstract fun getStructure(): baseOvergrownNanoforgeStructure?

    open fun getStructureWithUpdate(): baseOvergrownNanoforgeStructure {
        if (getStructure() == null) createStructure()
        return getStructure()!!
    }

    open fun createStructure() {
        market!!.addIndustry(getNewStructureId())
    }

    abstract fun getNewStructureId(): String

}
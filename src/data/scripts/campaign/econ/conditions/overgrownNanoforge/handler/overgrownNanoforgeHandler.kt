package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

/* The structure that creates this isnt guranteed to exist, this class exists to store data between decivs and shit so it's "consistant" */

// This should be considered the actual overgrown nanoforge structure. The building itself is only our hook into the API.
abstract class overgrownNanoforgeHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeEffectSource
) {
    var currentStructureId: String? = null
    val baseSource: overgrownNanoforgeEffectSource = initBaseSource

    var market: MarketAPI? = initMarket
        set(value: MarketAPI?) {
            if (field != null && value == null) {
                migratingToNullMarket()
            } else if (field != value) {
                migrateToNewMarket(value)
            }
            field = value
        }

    open fun init() {
        addSelfToMarket()
    }

    open fun delete() {
        removeSelfFromMarket(market)
    }

    open fun apply() {
        if (shouldCreateStructure()) {
           createStructure()
        }
    }

    open fun unapply() {
        removeStructure()
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

    open fun getStructureWithUpdate(): baseOvergrownNanoforgeStructure? {
        if (getStructure() == null) createStructure()
        return getStructure()
    }

    open fun shouldCreateStructure(): Boolean {
        return (getStructure() == null)
    }

    open fun createStructure() {
        val newStructureId = getNewStructureId()
        market?.addIndustry(newStructureId)
        currentStructureId = newStructureId
    }

    open fun removeStructure() {
        market?.removeIndustry(getStructureId())
    }
    /* Should return the String ID of the structure we currently have active. */
    abstract fun getStructureId(): String?
    /* Should return the String ID of the next structure we want to build. */
    abstract fun getNewStructureId(): String?

}
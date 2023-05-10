package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

/* The structure that creates this isnt guranteed to exist, this class exists to store data between decivs and shit so it's "consistant" */

// This should be considered the actual overgrown nanoforge structure. The building itself is only our hook into the API.
abstract class overgrownNanoforgeHandler(
    initMarket: MarketAPI,
    initBaseSource: overgrownNanoforgeEffectSource
) {
    var currentStructureId: String? = null
    val baseSource: overgrownNanoforgeEffectSource = initBaseSource

    var deleted: Boolean = false

    var market: MarketAPI = initMarket
        set(value: MarketAPI) {
             if (field != value) {
                migrateToNewMarket(value)
            }
            field = value
        }

    open fun init() {
        addSelfToMarket()
    }

    open fun delete() {
        removeSelfFromMarket(market)

        for (source in getAllSources()) source.delete() // TODO: this will cause issues if delete calls unapply since this callchain calls source.unapply
        deleted = true
    }

    open fun apply() {
        if (shouldCreateNewStructure()) {
           createStructure()
        }
        for (source in getAllSources()) source.apply()
    }

    open fun unapply() {
        removeStructure()

        for (source in getAllSources()) source.unapply()
    }

    open fun addSelfToMarket(market: MarketAPI): Boolean {
        TODO()
        apply()
    }
    open fun removeSelfFromMarket(market: MarketAPI): Boolean {
        TODO()
        unapply()
    }

    /** Called BEFORE market is made null. */
    open fun migratingToNullMarket() {
        val ourMarket = market ?: return
        removeSelfFromMarket(ourMarket)
    }

    /* Returns the structure this handler stores data of. Can be null if the structure doesn't exist. */
    open fun getStructure(): baseOvergrownNanoforgeStructure? {
        return (market?.getIndustry(currentStructureId) as? baseOvergrownNanoforgeStructure) 
    }

    open fun getStructureWithUpdate(): baseOvergrownNanoforgeStructure? {
        if (!structurePresent()) createStructure()
        return getStructure()
    }

    open fun shouldCreateNewStructure(): Boolean {
        return (structurePresent())
    }

    protected open fun createStructure() {
        val newStructureId = getNewStructureId()
        market?.addIndustry(newStructureId)
        currentStructureId = newStructureId
    }

    open fun removeStructure() {
        market?.removeIndustry(currentStructureId))
        currentStructureId = null
    }

    open fun getAllSources(): MutableSet<overgrownNanoforgeEffectSource> {
        return hashSetOf(baseSource)
    }

    /* Should return the String ID of the next structure we want to build. */
    abstract fun getNewStructureId(): String?

    fun structurePresent(): Boolean {
        return (getStructure() != null)
    }

}
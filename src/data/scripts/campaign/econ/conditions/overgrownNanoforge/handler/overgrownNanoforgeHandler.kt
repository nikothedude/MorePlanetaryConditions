package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE_REGEN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE_REGEN
import org.lazywizard.lazylib.MathUtils

/* The structure that creates this isnt guranteed to exist, this class exists to store data between decivs and shit so it's "consistant" */

// This should be considered the actual overgrown nanoforge structure. The building itself is only our hook into the API.
abstract class overgrownNanoforgeHandler(
    initMarket: MarketAPI
) {
    open var growing: Boolean = true
    var currentStructureId: String? = null

    lateinit var baseSource: overgrownNanoforgeEffectSource
    var manipulationIntel: baseOvergrownNanoforgeManipulationIntel?

    var cullingResistance: Int = 0
    var cullingResistanceRegeneration: Int = 0
    var growthRating: Int = 0

    var unapplying: Boolean = false
    var deleting: Boolean = false

    abstract fun createBaseSource(): overgrownNanoforgeEffectSource

    var deleted: Boolean = false

    var market: MarketAPI = initMarket
        set(value: MarketAPI) {
             if (field != value) {
                migrateToNewMarket(value)
            }
            field = value
        }

    protected open fun migrateToNewMarket(newMarket: MarketAPI) {
        removeSelfFromMarket(market)
        addSelfToMarket(newMarket)
    }

    open fun init(initBaseSource: overgrownNanoforgeEffectSource? = null, resistance: Int? = null, resistanceRegen: Int? = null) {
        baseSource = initBaseSource ?: createBaseSource()
        cullingResistance = resistance ?: createBaseCullingResistance()
        cullingResistanceRegeneration = resistanceRegen ?: createBaseCullingResistanceRegeneration()

        if (!growing) {
            initWithGrowing()
        }
    }

    open fun initWithGrowing() {
        manipulationIntel = createManipulationIntel()
        addSelfToMarket(market)  
    }

    abstract fun createManipulationIntel(): baseOvergrownNanoforgeManipulationIntel

    open fun createBaseCullingResistance(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE,
            OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE)
    }

    open fun createBaseCullingResistanceRegeneration(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE_REGEN,
            OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE_REGEN)
    }

    open fun delete() {
        if (deleted || deleting) return
        deleting = true
        removeSelfFromMarket(market)

        manipulationIntel?.delete()

        for (source in getAllSources()) source.delete() // TODO: this will cause issues if delete calls unapply since this callchain calls source.unapply
        deleted = true
        deleting = false
    }

    open fun culled() {
        delete()
    }

    open fun apply(): Boolean {
        if (growing) return false
        applyEffects()
        return true
    }

    open fun applyEffects() {
        if (shouldCreateNewStructure()) {
            createStructure()
        }
        for (source in getAllSources()) source.apply()
    }

    open fun unapply() {
        if (unapplying) return
        unapplying = true
        removeStructure()

        for (source in getAllSources()) source.unapply()
        unapplying = false
    }

    open fun addSelfToMarket(market: MarketAPI) {
        apply()
    }

    open fun removeSelfFromMarket(market: MarketAPI) {
        unapply()
    }

    /** Called BEFORE market is made null. */
    open fun migratingToNullMarket() {
        val ourMarket = market ?: return
        removeSelfFromMarket(ourMarket)
    }

    /* Returns the structure this handler stores data of. Can be null if the structure doesn't exist. */
    open fun getStructure(): baseOvergrownNanoforgeStructure? {
        return (market.getIndustry(currentStructureId) as? baseOvergrownNanoforgeStructure)
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
        market.addIndustry(newStructureId)
        currentStructureId = newStructureId
    }

    open fun removeStructure() {
        market.removeIndustry(currentStructureId, null, false)
        currentStructureId = null
    }

    open fun getAllSources(): MutableSet<overgrownNanoforgeEffectSource> {
        val sources = HashSet<overgrownNanoforgeEffectSource>()
        if (this::baseSource.isInitialized) sources += baseSource
        return sources
    }

    /* Should return the String ID of the next structure we want to build. */
    abstract fun getNewStructureId(): String?

    fun structurePresent(): Boolean {
        return (getStructure() != null)
    }

    open fun getCurrentName(): String {
        if (getStructure() != null) return getStructure()!!.currentName
        return "placeholder"
    }

    /** Should return the "master" handler, AKA the nanoforge industry's handler. */
    abstract fun getCoreHandler(): overgrownNanoforgeIndustryHandler
    fun startDestroyingStructure() {
        getCoreHandler().intelBrain.startDestroyingStructure(this)
    }

}
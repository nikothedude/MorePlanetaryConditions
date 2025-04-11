package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.baseOvergrownNanoforgeStructure
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeManipulationIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffectDescData
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectCategories
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_marketUtils.isDeserializing
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_marketUtils.isValidTargetForOvergrownHandler
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_INDUSTRY_CULLING_RESISTANCE_REGEN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_INDUSTRY_CULLING_RESISTANCE_REGEN
import org.lazywizard.lazylib.MathUtils

/* The structure that creates this isnt guranteed to exist, this class exists to store data between decivs and shit so it's "consistant" */

// This should be considered the actual overgrown nanoforge structure. The building itself is only our hook into the API.
/**
 * The backend representation of the "growth" that may or may not hold a corresponding [baseOvergrownNanoforgeStructure] instance - 
 * but always has the capabilities to create one.
 *
 * Due to the fact there any many states where the structure does not exist (Growing, market uncolonized), this class MUST exist.
 * An added bonus is detaching from the API, which allows us to maintain code safety far more (fuck you deserialization).
 */
abstract class overgrownNanoforgeHandler(
    initMarket: MarketAPI,
    open var growing: Boolean = true
) {
    /** The id of the structure we currently have instantiated. Nullable if there is no structure. Used for 
     * getting our structure in [getStructure] - we get the industry with our ID.
     * TODO: This can be changed to just holding a ref to our industry - but what about junk handlers? Still need to store our ID, or at lesat our designation.
     */
    var currentStructureId: String? = null

    /**
     * The source of the effect we will apply. 
     * A bit of boilerplate right now. The original plan with this was to have a source list var that could hold multiple sources, but
     * its not totally needed. Low priority to change.
     */
    lateinit var baseSource: overgrownNanoforgeEffectSource

    var manipulationIntel: baseOvergrownNanoforgeManipulationIntel? = null
    var cullingResistance: Int = 0

    var cullingResistanceRegeneration: Int = 0

    var applying: Boolean = false
    var unapplying: Boolean = false
    var deleting: Boolean = false

    /*val aiCoreEffects: MutableMap<String, overgrownNanoforgeAICoreEffect> = HashMap()

    init {
        aiCoreEffects[Commodities.ALPHA_CORE] = overgrownNanoforgeAlphaCoreEffect(this)
        aiCoreEffects[Commodities.BETA_CORE] = overgrownNanoforgeBetaCoreEffect(this)
        aiCoreEffects[Commodities.GAMMA_CORE] = overgrownNanoforgeGammaCoreEffect(this)
    }*/ 
    // TODO: future mechanics

    abstract fun createBaseSource(): overgrownNanoforgeEffectSource

    var deleted: Boolean = false

    init {
        Global.getSector() //todo: remove
    }

    /** The [MarketAPI] instance we are beholden to. The market we apply our effects to, and around. */
    open var market: MarketAPI = initMarket
        set(value: MarketAPI) {
             if (field !== value) { //shit breaks if we dont do reference checking, hence the extra =
             // while loose equivilancy works a lot of the time (simple clones), sometimes it breaks shit terribly
                 if (field != null) {
                     removeSelfFromMarket(field)
                 }
                 field = value
                 addSelfToMarket(value)
                 migrateToNewMarket(value) //changed markets
                 return
            }
            field = value
        }

    open fun readResolve(): Any? {
        return this
    }

    /** Called when [market] is changed. */
    protected open fun migrateToNewMarket(newMarket: MarketAPI) {
    }

    open fun init(initBaseSource: overgrownNanoforgeEffectSource? = null, resistance: Int? = null, resistanceRegen: Int? = null) {
        baseSource = initBaseSource ?: createBaseSource()
        cullingResistance = resistance ?: createBaseCullingResistance()
        cullingResistanceRegeneration = resistanceRegen ?: createBaseCullingResistanceRegeneration()

        if (!growing) {
            initWithGrowing()
        }
    }

    fun getOurBrain(): overgrownNanoforgeSpreadingBrain {
        return getCoreHandler().intelBrain
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

    open fun delete(): Boolean {
        if (deleted || deleting) return false
        deleting = true
        removeSelfFromMarket(market)

        manipulationIntel?.delete()

        for (source in getAllSources()) source.delete() // TODO: this will cause issues if delete calls unapply since this callchain calls source.unapply
        deleted = true
        deleting = false

        return true
    }

    open fun culled() {
        ImGoingToRefactorThisSohard = true
        delete()
        ImGoingToRefactorThisSohard = false
    }

    open fun apply(): Boolean {
        if (applying) return false
        if (growing) return false
        applying = true

        applyEffects()

        applying = false
        return true
    }

    open fun applyEffects() {
        if (shouldCreateNewStructure()) {
            createStructure()
        }
        for (source in getAllSources()) source.apply()
    }

    open fun unapply(removeStructure: Boolean = true): Boolean {
        if (unapplying) return false
        unapplying = true

        if (removeStructure) {
            removeStructure()
        }

        for (source in getAllSources()) source.unapply()
        unapplying = false

        return true
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

    /** Returns the structure this handler stores data of. Can be null if the structure doesn't exist. */
    open fun getStructure(): baseOvergrownNanoforgeStructure? {
        return (market.getIndustry(currentStructureId) as? baseOvergrownNanoforgeStructure)
    }

    open fun getStructureWithUpdate(): baseOvergrownNanoforgeStructure? {
        updateStructure()
        return getStructure()
    }

    fun updateStructure() {
        if (!structurePresent() && shouldCreateNewStructure()) {
            createStructure()
        }
    }

    open fun shouldCreateNewStructure(): Boolean {
        return (!market.isDeserializing() && market.isInhabited() && market.isValidTargetForOvergrownHandler() && !structurePresent())
    }

    protected open fun createStructure() {
        val newStructureId = getNewStructureId()
        if (Global.getSettings().getIndustrySpec(newStructureId) == null) {
            niko_MPC_debugUtils.displayError("invalid industry id ($newStructureId) during nanoforge handler createStructure, aborting")
            return // whatever happens after this isnt exactly defined behavior, but its better than a crash
        }
        market.addIndustry(newStructureId)
        currentStructureId = newStructureId
    }

    var ImGoingToRefactorThisSohard = false
        get() {
            if (field == null) field = false
            return field
        }
    open fun removeStructure() {
        getStructure()?.deleteStructureOnDelete = ImGoingToRefactorThisSohard
        getStructure()?.delete()
        getStructure()?.deleteStructureOnDelete = true
    }

    fun getPositiveEffectDescData(): MutableList<overgrownNanoforgeEffectDescData> {
        return getEffectDescData(overgrownNanoforgeEffectCategories.BENEFIT)
    }

    fun getNegativeEffectDescData(): MutableList<overgrownNanoforgeEffectDescData> {
        return getEffectDescData(overgrownNanoforgeEffectCategories.DEFICIT)
    }

    fun getEffectDescData(category: overgrownNanoforgeEffectCategories): MutableList<overgrownNanoforgeEffectDescData> {
        return baseSource.getEffectDescData(category)
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
        return getDefaultName()
    }

    abstract fun getDefaultName(): String

    /** Should return the "master" handler, AKA the nanoforge industry's handler. */
    abstract fun getCoreHandler(): overgrownNanoforgeIndustryHandler
    fun startDestroyingStructure() {
        getCoreHandler().intelBrain.startDestroyingStructure(this)
    }

    fun getCategoryDisabledReasons(): MutableMap<String, Array<String>> {
        return baseSource.getCategoryDisabledReasons()
    }

    open fun isCorrupted(): Boolean {
        return (market == null)
    }

}
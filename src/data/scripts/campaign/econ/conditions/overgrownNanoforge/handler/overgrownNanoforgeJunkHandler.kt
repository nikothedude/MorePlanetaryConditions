package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.baseOvergrownNanoforgeManipulationIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeJunkStructureId
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkId
import data.utilities.niko_MPC_marketUtils.setOvergrownNanoforgeJunkHandler
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_JUNK_NAME
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN
import org.lazywizard.lazylib.MathUtils

// The data store and "permament" representation of junk spawned by a overgrown nanoforge

// WHAT THIS CLSAS SHOULD HOLD
// 1. A source we apply when needed
// 2. Okay just everything the junk should hold
class overgrownNanoforgeJunkHandler(
    initMarket: MarketAPI,
    val masterHandler: overgrownNanoforgeIndustryHandler,
    junkDesignation: Int? = null,
    growing: Boolean = false,
): overgrownNanoforgeHandler(initMarket, growing) {

    /**
     * Is our structure an industry?
     */
    var industry: Boolean = false

    /* Should be the String ID of the building we currently have active, or will instantiate later. */
    var cachedBuildingId: String? = if (junkDesignation == null) {
        market.getNextOvergrownJunkId()
    } else {
        baseStructureId + junkDesignation
    }
        set(value: String?) {
            if (value == null) {
                handleNullBuildingId()
            }
            field = value
        }

    private fun handleNullBuildingId() {
        delete()
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        masterHandler.junkHandlers -= this
        masterHandler.notifyJunkDeleted(this)

        return true
    }

    fun instantiate() {
        if (growing) {
            growing = false
            initWithGrowing()
        }
    }

    override fun initWithGrowing() {
        super.initWithGrowing()

        masterHandler.junkHandlers += this
    }

    override fun createStructure() {
        super.createStructure()
        cachedBuildingId = currentStructureId
    }

    override fun addSelfToMarket(market: MarketAPI) {
        if (growing) return
        market.setOvergrownNanoforgeJunkHandler(this)
        super.addSelfToMarket(market)
        masterHandler.notifyJunkAdded(this)
    }

    // Shouldn't cause issues, since this is only called during the building's instantiation, right? Riiiiiight?
    // No we still need to keep a copy of our structure ID so we can actually grab it huhgh
    override fun getNewStructureId(): String? {
        return cachedBuildingId
    }

    override fun getCoreHandler(): overgrownNanoforgeIndustryHandler {
        return masterHandler
    }

    override fun createBaseSource(): overgrownNanoforgeEffectSource {
        val sourceType = overgrownNanoforgeSourceTypes.adjustedPick() ?: overgrownNanoforgeSourceTypes.STRUCTURE
        val params = overgrownNanoforgeRandomizedSourceParams(this, sourceType)
        return overgrownNanoforgeRandomizedSource(this, params)
    }

    override fun createManipulationIntel(): baseOvergrownNanoforgeManipulationIntel {
        val intel = baseOvergrownNanoforgeManipulationIntel(getCoreHandler().intelBrain, this)
        intel.init(getOurBrain().hidden)
        return intel
    }

    override fun getStructure(): overgrownNanoforgeJunk? {
        return currentStructureId?.let { (market.getIndustry(it) as? overgrownNanoforgeJunk) }
    }

    override fun createBaseCullingResistance(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE
        )
    }

    override fun createBaseCullingResistanceRegeneration(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_JUNK_CULLING_RESISTANCE_REGEN,
            OVERGROWN_NANOFORGE_MAX_JUNK_CULLING_RESISTANCE_REGEN
        )
    }

    fun getBaseBudget(): Float? {
        return (baseSource as? overgrownNanoforgeRandomizedSource)?.params?.budget
    }

    override fun getDefaultName(): String {
        return OVERGROWN_NANOFORGE_JUNK_NAME
    }

    fun getOurDesignation(): Int? {
        if (cachedBuildingId == null) return null // shouldnt happen since this deletes if this is null i believe
        return (cachedBuildingId!!.filter { it.isDigit() }.toInt())
    }

    fun isIndustry(): Boolean {
        return industry
    }

    companion object {
        val maxStructuresPossible: Int = niko_MPC_settings.MAX_STRUCTURES_ALLOWED
        val baseStructureId: String = overgrownNanoforgeJunkStructureId
    }
}

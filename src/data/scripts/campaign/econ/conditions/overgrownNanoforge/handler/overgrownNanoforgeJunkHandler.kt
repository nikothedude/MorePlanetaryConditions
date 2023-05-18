package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.campaign.econ.MarketAPI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeJunk
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeRandomizedSource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeSourceTypes
import data.utilities.niko_MPC_ids.overgrownNanoforgeJunkHandlerMemoryId
import data.utilities.niko_MPC_marketUtils.getNextOvergrownJunkId
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
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
    junkDesignation: Int? = null
): overgrownNanoforgeHandler(initMarket) {

    var growing: Boolean = true

    /* Should be the String ID of the building we currently have active, or will instantiate later. */
    var cachedBuildingId: String? = if (junkDesignation == null) { market.getNextOvergrownJunkId() } else { overgrownNanoforgeJunkHandlerMemoryId + junkDesignation }
        set(value: String?) {
            if (value == null) {
                handleNullBuildingId()
            }
            field = value
        }

    private fun handleNullBuildingId() {
        delete()
    }

    override fun delete() {
        super.delete()

        masterHandler.junk -= this
        masterHandler.notifyJunkDeleted(this)
    }

    fun instantiate() {
        growing = false
        initWithGrowing()
    }

    override fun createStructure() {
        super.createStructure()
        cachedBuildingId = currentStructureId
    }

    override fun addSelfToMarket() {
        if (growing) return
        super.addSelfToMarket()

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
        val params = overgrownNanoforgeRandomizedSourceParams(masterHandler, sourceType)
        return overgrownNanoforgeRandomizedSource(masterHandler, params)
    }

    override fun getStructure(): overgrownNanoforgeJunk? {
        return (market.getIndustry(currentStructureId) as? overgrownNanoforgeJunk)
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
}
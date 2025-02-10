package data.scripts.campaign.econ.conditions.overgrownNanoforge.handler

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.overgrownNanoforge.MPC_overgrownNanoforgeExpeditionAssignmentAI
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.data.overgrownNanoforgeIndustrySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeIndustry
import data.scripts.campaign.econ.conditions.overgrownNanoforge.intel.plugins.overgrownNanoforgeIndustryManipulationIntel
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.budgetHolder
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeAlterSupplySource
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.effectTypes.overgrownNanoforgeEffect
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeEffectPrototypes.Companion.getWrappedInstantiationList
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.effects.overgrownNanoforgeRandomizedSourceParams
import data.scripts.campaign.econ.conditions.overgrownNanoforge.sources.overgrownNanoforgeEffectSource
import data.utilities.niko_MPC_debugUtils.displayError
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_industryIds.overgrownNanoforgeIndustryId
import data.utilities.niko_MPC_marketUtils
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforge
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeCondition
import data.utilities.niko_MPC_marketUtils.getOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.isInhabited
import data.utilities.niko_MPC_marketUtils.removeOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.setOvergrownNanoforgeIndustryHandler
import data.utilities.niko_MPC_marketUtils.shouldHaveOvergrownNanoforgeIndustry
import data.utilities.niko_MPC_mathUtils
import data.utilities.niko_MPC_settings.MAX_STRUCTURES_ALLOWED
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MAX
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_BASE_SCORE_MIN
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_INDUSTRY_NAME
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MAX_PREDEFINED_JUNK
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_MIN_PREDEFINED_JUNK
import data.utilities.niko_MPC_settings.OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.magiclib.kotlin.getSourceMarket
import org.magiclib.kotlin.isMilitary
import kotlin.math.abs

// WHAT THIS CLASS SHOULD HOLD
// 1. The base source of the industry
// 2. A list of structures spawned by this industry, or at least their parameters so they may be remade on demand
// 2.1: Alternatively, both, and just have this be the core's data hub it grabs everything from
// 3. The spreading scripts, as to allow spreading to happen when decivilized
// 4. The intel, to allow it to be active when uncolonized

// All in all, this is the permanent representation of the existance of a overgrown nanoforge
// The industry itself is only the hook we use to put this class into the game
class overgrownNanoforgeIndustryHandler(
    initMarket: MarketAPI,
    override var growing: Boolean = false,
    var generateJunk: Boolean = true
): overgrownNanoforgeHandler(initMarket, growing), EveryFrameScript {

    var focusingOnExistingCommodities: Boolean = true
        get() {
            if (field == null) field = true // TODO: remove after 3.3.4, here for compatability
            return field
        }

    var exposed: Boolean = false
        set(value) {
            val oldField = field
            field = value
            if (oldField != field) {
                getCastedManipulationIntel()?.exposed = field
            }
        }

    val junkHandlers: MutableSet<overgrownNanoforgeJunkHandler> = HashSet()

    override fun migrateToNewMarket(newMarket: MarketAPI) {
        super.migrateToNewMarket(newMarket)

        junkHandlers.forEach { it.market = newMarket }
    }

    var discovered: Boolean = false
        set(value: Boolean) {
            if (value != field) {
                intelBrain.hidden = !value
            }
            field = value
        }

    val intelBrain: overgrownNanoforgeSpreadingBrain = createIntelBrain()
    /** When this procs, spawns a expedition fleet to cull it. Purely flavor right now.*/
    var expeditionInterval = IntervalUtil(120f, 330f) // days
        get() {
            if (field == null) field = IntervalUtil(120f, 330f)
            return field
        }

    init {
        if (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] == null) Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] = HashSet<overgrownNanoforgeIndustryHandler>()
        (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>) += this
    }

    override fun createBaseSource(): overgrownNanoforgeIndustrySource {
        val baseScore = getBaseScore()
        val basePrototype = getBaseEffectPrototype()
        val wrappedPrototypes = getWrappedInstantiationList(basePrototype, this, baseScore)
        val scoredPrototypes = overgrownNanoforgeEffectPrototypes.scorePrototypes(this, budgetHolder(baseScore), wrappedPrototypes = wrappedPrototypes)
        var effects = overgrownNanoforgeEffectPrototypes.generateEffects(this, scoredPrototypes)

        if (effects.isEmpty()) {
            displayError("null supplyeffect on basestats oh god oh god oh god oh god oh god help")
            val source = overgrownNanoforgeIndustrySource(
                this, //shuld never happen
                mutableSetOf(overgrownNanoforgeAlterSupplySource(this, Commodities.ORGANS, 500))
            )
            return source
        }
        return overgrownNanoforgeIndustrySource(this, effects)
    }

    override fun init(initBaseSource: overgrownNanoforgeEffectSource?, resistance: Int?, resistanceRegen: Int?) {
        super.init(initBaseSource, resistance, resistanceRegen)
        if (market.getOvergrownNanoforgeIndustryHandler() != this) {
            displayError("nanoforge handler created on market with pre-existing handler: ${market.name}")
        }
        toggleExposed()

        if (generateJunk) {
            generatePreexistingJunk()
        }
    }

    private fun generatePreexistingJunk() {
        var completeJunkToInstantiate = getInitialJunkNumber()

        while (completeJunkToInstantiate-- > 0) {
            instantiateJunk()
        }
    }

    private fun instantiateJunk(): overgrownNanoforgeJunkHandler {
        val handler = overgrownNanoforgeJunkHandler(market, this, growing = false)
        handler.init()

        return handler
    }

    private fun getInitialJunkNumber(): Int {
        return MathUtils.getRandomNumberInRange(
            OVERGROWN_NANOFORGE_MIN_PREDEFINED_JUNK,
            OVERGROWN_NANOFORGE_MAX_PREDEFINED_JUNK
        )
    }

    override fun advance(amount: Float) {
        if (!market.isInhabited()) {
            val days = Misc.getDays(amount)
            expeditionInterval.advance(days)
            if (expeditionInterval.intervalElapsed()) {
                spawnExpedition()
            }
        } else {
            expeditionInterval.elapsed = 0f
        }
        //junkSpreader.advance(adjustedAmount)
    }

    private fun spawnExpedition() {
        val prepTime = MathUtils.getRandomNumberInRange(15f, 17f)
        val fleet = spawnExpeditionFleet()
        setupExpeditionFleet(fleet, prepTime)
    }

    private fun setupExpeditionFleet(fleet: CampaignFleetAPI, prepTime: Float) {
        fleet.cargo.addFuel(fleet.cargo.maxFuel)
        if (fleet.faction.id == Factions.INDEPENDENT) {
            fleet.isNoFactionInName = true
            fleet.name = "Black arrow expedition"
        } else {
            fleet.name = "Expedition"
        }

        fleet.memoryWithoutUpdate["\$MPC_overgrownExpeditionFleet"] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_LOW_REP_IMPACT] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE] = true
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true

        MPC_overgrownNanoforgeExpeditionAssignmentAI(fleet, target = market, homeMarket = fleet.getSourceMarket(), prepTime = prepTime).start()
    }

    private fun spawnExpeditionFleet(): CampaignFleetAPI {
        val picker = WeightedRandomPicker<String>()
        factionsToExpeditionChance.forEach {
            if (it.key == Factions.INDEPENDENT || Global.getSector().getFaction(it.key).getMarketsCopy().isNotEmpty()) {
                picker.add(it.key, it.value)
            }
        }
        val faction = Global.getSector().getFaction(picker.pick()) // guaranteed to have indies
        val sourceMarket = faction.getMarketsCopy().shuffled().firstOrNull { it.isMilitary() } ?: faction.getMarketsCopy().randomOrNull() ?: Global.getSector().economy.marketsCopy.random()

        val params = FleetParamsV3(
            sourceMarket,
            FleetTypes.PATROL_SMALL,
            BASE_EXPEDITION_FP,
            10f,
            40f,
            10f,
            0f,
            10f,
            0f
        )
        params.officerLevelLimit = 7
        params.officerLevelBonus = 1
        val fleet = FleetFactoryV3.createFleet(params)

        sourceMarket.containingLocation.addEntity(fleet)
        fleet.containingLocation = sourceMarket.containingLocation
        val primaryEntityLoc = sourceMarket.primaryEntity.location
        fleet.setLocation(primaryEntityLoc.x, primaryEntityLoc.y)
        val facingToUse = MathUtils.getRandomNumberInRange(0f, 360f)
        fleet.facing = facingToUse

        return fleet
    }

    override fun shouldCreateNewStructure(): Boolean {
        return (market.shouldHaveOvergrownNanoforgeIndustry() && super.shouldCreateNewStructure())
    }

    fun getAdjustedSpreadAmount(amount: Float): Float {
        var adjustedAmount = amount
        val isInhabited = market.isInhabited()
        if (!isInhabited) adjustedAmount *= OVERGROWN_NANOFORGE_UNINHABITED_SPREAD_MULT
        return adjustedAmount
    }

    override fun isDone(): Boolean {
        return deleted
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun applyEffects() {
        super.applyEffects()
        Global.getSector().addScript(this)
        if (market.getOvergrownNanoforgeIndustryHandler() != this) {
            displayError("nanoforge handler created on market with pre-existing handler: ${market.name}")
        }
        for (ourJunk in junkHandlers) {
            ourJunk.apply()
        }
    }

    override fun delete(): Boolean {
        if (!super.delete()) return false

        (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>) -= this
        for (ourJunk in junkHandlers.toMutableSet()) {
            ourJunk.delete()
        }

        market.getOvergrownNanoforgeCondition()?.delete()
        intelBrain.delete()

        return true
    }

    fun notifyJunkDeleted(junk: overgrownNanoforgeJunkHandler) {
        toggleExposed()
    }
    fun notifyJunkAdded(junk: overgrownNanoforgeJunkHandler) {
        toggleExposed()
    }

    fun notifySpreadingStarted() {
        toggleExposed()
    }
    fun notifySpreadingStopped() {
        toggleExposed()
    }

    fun shouldExposeSelf(): Boolean {
        if (junkHandlers.isEmpty() && intelBrain.spreadingState != spreadingStates.SPREADING) return true

        return false
    }
    fun toggleExposed() {
        exposed = shouldExposeSelf()
    }

    override fun unapply(): Boolean {
        if (super.unapply()) return false
        Global.getSector().removeScript(this)
        for (ourJunk in junkHandlers) {
            ourJunk.unapply()
        }
        return true
    }

    override fun addSelfToMarket(market: MarketAPI) {
        val preExistingHandler = market.getOvergrownNanoforgeIndustryHandler()
        if (preExistingHandler != null && preExistingHandler != this) {
            displayError("duplicate industy handler on ${market.name}")
            delete()
            return
        }
        market.setOvergrownNanoforgeIndustryHandler(this)
        super.addSelfToMarket(market)
    }

    override fun removeSelfFromMarket(market: MarketAPI) {
        val currHandler = market.getOvergrownNanoforgeIndustryHandler()
        if (currHandler != this) {
            displayError("bad deletion attempt for overgrown nanoforge handler on ${market.name}")
        }
        market.removeOvergrownNanoforgeIndustryHandler()
        super.removeSelfFromMarket(market)
    }

    private fun getBaseEffectPrototype(): overgrownNanoforgeEffectPrototypes {
        return overgrownNanoforgeEffectPrototypes.ALTER_SUPPLY 
        // Originally we just wanted the base effect to be alter supply
        // THis method is just here for like. Potential future changes
    }

    override fun getStructure(): overgrownNanoforgeIndustry? {
        return market.getOvergrownNanoforge()
    }

    override fun getNewStructureId(): String {
        return overgrownNanoforgeIndustryId
    }

    override fun getDefaultName(): String {
        return OVERGROWN_NANOFORGE_INDUSTRY_NAME
    }

    override fun getCoreHandler(): overgrownNanoforgeIndustryHandler {
        return this
    }

    fun createIntelBrain(): overgrownNanoforgeSpreadingBrain {
        val brain = overgrownNanoforgeSpreadingBrain(this)
        brain.init()
        return brain
    }

    fun getJunkSources(): MutableSet<overgrownNanoforgeEffectSource> {
        val sources: MutableSet<overgrownNanoforgeEffectSource> = HashSet()
        for (handler in junkHandlers) {
            sources.add(handler.baseSource)
        }
        return sources
    }

    fun getSupply(commodityId: String): Int { //TODO: convert to a int var that updates when any of the values change
        if (getStructure() != null) return getStructure()!!.getSupply(commodityId).quantity.modifiedInt
        var supply: Int = 0
        for (source in getAllSources()) {
            for (effect in source.effects) {
                if (effect is overgrownNanoforgeAlterSupplySource && effect.commodityId == commodityId) {
                    supply += effect.amount
                }
            }
        }
        return supply
    }

    override fun createManipulationIntel(): overgrownNanoforgeIndustryManipulationIntel {
        val intel = overgrownNanoforgeIndustryManipulationIntel(intelBrain, this)
        intel.init(intelBrain.hidden)
        return intel
    }

    fun getCastedManipulationIntel(): overgrownNanoforgeIndustryManipulationIntel? {
        return (manipulationIntel as? overgrownNanoforgeIndustryManipulationIntel)
    }

    fun isSpreading(): Boolean {
        return intelBrain.spreadingState == spreadingStates.SPREADING
    }

    override fun culled() {
        spawnSpecialItem()
        super.culled()
    }

    private fun spawnSpecialItem() {
        val overgrownNanoforgeData = SpecialItemData(niko_MPC_ids.overgrownNanoforgeItemId, null)
        Misc.getStorage(market).cargo.addSpecial(overgrownNanoforgeData, 1f)
    }

    fun getMaxJunkAllowed(): Int {
        if (market.isInhabited()) return MAX_STRUCTURES_ALLOWED - 1
        return MAX_STRUCTURES_ALLOWED - 2
    }

    companion object {
        fun getBaseScore(): Float {
            return MathUtils.getRandomNumberInRange(OVERGROWN_NANOFORGE_BASE_SCORE_MIN, OVERGROWN_NANOFORGE_BASE_SCORE_MAX)
        }

        fun allHandlerMarketsSynced(): Boolean {
            return (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>).all {
                it.market.primaryEntity?.market == it.market }
        }

        fun allHandlerMemorySynced(): Boolean {
            return (Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>).all {
                it.market.primaryEntity?.market?.getOvergrownNanoforgeIndustryHandler() == it}
        }

        fun getUnsyncedMarkets(): List<MarketAPI> {
            val handlers = Global.getSector().memoryWithoutUpdate["\$overgrownNanoforgeHandlerList"] as HashSet<overgrownNanoforgeIndustryHandler>
            val unsyncedMarkets = ArrayList<MarketAPI>()
            for (handler in handlers) {
                if (handler.market.primaryEntity?.market != handler.market) unsyncedMarkets += handler.market
            }
            return unsyncedMarkets
        }

        val factionsToExpeditionChance = mutableMapOf(
            Pair(Factions.HEGEMONY, 20f),
            Pair(Factions.LUDDIC_CHURCH, 5f),
            Pair(Factions.INDEPENDENT, 1f),
        )
        const val BASE_EXPEDITION_FP = 80f
    }

    override fun isCorrupted(): Boolean {
        return (super.isCorrupted() || junkHandlers == null)
    }
}

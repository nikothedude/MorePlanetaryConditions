package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignmentTypes
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings
import indevo.exploration.minefields.conditions.MineFieldCondition
import indevo.ids.Ids
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.makeImportant
import java.awt.Color
import java.util.*

class MPC_fractalCoreFactor(intel: HostileActivityEventIntel?) : BaseHostileActivityFactor(intel) {

    companion object {
        const val FOB_MARKET_ID = "MPC_arkFOB"
        const val FOB_MARKET_NAME = "Ark FOB"
        val fleetTypesToWeight = hashMapOf(
           // Pair(FleetTypes.TRADE, 5f),
            Pair(FleetTypes.TRADE_LINER, 5f),
            Pair(FleetTypes.TRADE_SMALL, 5f),
        )
        val contributingFactionsToWeight = hashMapOf(
            Pair(Factions.HEGEMONY, 30f),
            Pair(Factions.LUDDIC_CHURCH, 20f),
            Pair(Factions.INDEPENDENT, 10f),
            Pair(Factions.TRITACHYON, 5f),
            Pair(Factions.DIKTAT, 1f),
            Pair(Factions.LUDDIC_PATH, 1f),
        )

        fun getFOB(): MarketAPI? = Global.getSector().economy.getMarket(FOB_MARKET_ID)
        fun getContributingFactions(): List<FactionAPI> {
            return contributingFactionsToWeight.keys.map { Global.getSector().getFaction(it) }.filter { it != null && checkFactionExists(it.id, true) }
        }

        fun isActive(): Boolean {
            if (MPC_IAIICFobIntel.get() != null) return false
            if (Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.PLAYER_DEFENDED_FRACTAL_CORE)) return false
            if (!Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.DID_HEGEMONY_SPY_VISIT)) return false // this is the confirmation
            if (MPC_hegemonyFractalCoreCause.getFractalColony() == null) return false
            if (getContributingFactions().isEmpty()) return false
            if (!checkFactionExists(Factions.HEGEMONY, true)) return false

            return true
        }
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Hegemony"
    }

    override fun getNameForThreatList(first: Boolean): String {
        return "Hegemony"
    }

    override fun getProgressStr(intel: BaseEventIntel?): String? {
        return ""
    }

    override fun getDescColor(intel: BaseEventIntel?): Color {
        return Global.getSector().getFaction(Factions.HEGEMONY).color
    }

    override fun getNameColorForThreatList(): Color {
        return Global.getSector().getFaction(Factions.HEGEMONY).color
    }

    override fun addBulletPointForEvent(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?, info: TooltipMakerAPI?,
                                        mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float) {
        info!!.addPara("Unknown action imminent", initPad, tc)
    }

    override fun addBulletPointForEventReset(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?, info: TooltipMakerAPI?,
        mode: IntelInfoPlugin.ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float
    ) {
        info!!.addPara("Unknown action averted", tc, initPad)
    }

    override fun addStageDescriptionForEvent(
        intel: HostileActivityEventIntel?,
        stage: EventStageData,
        info: TooltipMakerAPI
    ) {
        var small = 0f
        val opad = 10f
        small = 8f
        val fractalSystem = MPC_hegemonyFractalCoreCause.getFractalColony()?.starSystem
        info.addPara(
            "IntSec is reporting high levels of activity in the outskirts of ${fractalSystem?.name}, and there is good intel " +
            "to suggest a major hostile operation may occur in the near future. %s",
            opad,
            Misc.getNegativeHighlightColor(),
            "MilInt encourages extreme caution over the threat, and advises aversive action to be taken if " +
                    "we are not prepared for a possibly unprecedented conflict."
        )
        stage.beginResetReqList(info, true, "crisis", opad)
        val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()?.name
        if (fractalColony != null) {
            info.addPara(
                "The %s is removed from %s",
                0f,
                Misc.getHighlightColor(),
                "fractal core", "${MPC_hegemonyFractalCoreCause.getFractalColony()?.name}"
            )
        } else {
            info.addPara(
                "The %s is not installed in any colonies",
                0f,
                Misc.getHighlightColor(),
                "fractal core"
            )
        }
        stage.endResetReqList(info, false, "crisis", -1, -1)
        addBorder(info, Global.getSector().getFaction(Factions.NEUTRAL).baseUIColor)
    }

    override fun getEventStageIcon(intel: HostileActivityEventIntel?, stage: EventStageData?): String? {
        return Global.getSector().getFaction(Factions.NEUTRAL).crest
    }

    override fun getEventFrequency(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?): Float {
        if (stage?.id == HostileActivityEventIntel.Stage.MINOR_EVENT) return 0f
        if (!isActive()) return 0f
        if (getFOB() != null) return 0f

        return 10f
    }

    override fun rollEvent(intel: HostileActivityEventIntel?, stage: EventStageData?) {
        super.rollEvent(intel, stage)

        val data = HAERandomEventData(this, stage)
        stage!!.rollData = data
        intel!!.sendUpdateIfPlayerHasIntel(data, false)
    }

    override fun fireEvent(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?): Boolean {
        super.fireEvent(intel, stage)

        if (!isActive()) return false

        val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return false
        val fractalSystem = fractalColony.starSystem ?: return false
        val foundDist = MathUtils.getDistance(MPC_fractalCrisisHelpers.getStationPoint(fractalSystem), fractalSystem.center.location)

        stage!!.rollData = null
        return spawnFOB(fractalColony, fractalSystem, foundDist)
    }

    override fun getStageTooltipImpl(intel: HostileActivityEventIntel?, stage: EventStageData): TooltipCreator? {
        return getDefaultEventTooltip("Unknown Action", intel, stage)
    }

    private fun spawnFOB(fractalColony: MarketAPI, fractalSystem: StarSystemAPI, foundDist: Float): Boolean {
        val station = fractalColony.starSystem.addCustomEntity(niko_MPC_ids.MPC_FOB_ID, FOB_MARKET_NAME, "MPC_IAIICFOB", niko_MPC_ids.IAIIC_FAC_ID)
        station.setCircularOrbitPointingDown(fractalSystem.center, MathUtils.getRandomNumberInRange(0f, 360f), foundDist, 420f)
        val market = createMarket(station)
        station.makeImportant(niko_MPC_ids.MPC_FOB_ID, Float.MAX_VALUE)

        initFOBFleets(station, market, fractalSystem)

        MPC_IAIICFobIntel()
        //FOBIntel.setListener(this)
        //Global.getSector().intelManager.addIntel(FOBIntel)

        return true
    }

    private fun initFOBFleets(station: CustomCampaignEntityAPI, market: MarketAPI, fractalSystem: StarSystemAPI) {
        initMercFleet(station, market, fractalSystem)
    }

    private fun initMercFleet(station: CustomCampaignEntityAPI, market: MarketAPI, fractalSystem: StarSystemAPI) {
        return
    }

    protected fun createMarket(FOBStation: SectorEntityToken): MarketAPI {
        val market = Global.getFactory().createMarket(FOB_MARKET_ID, FOB_MARKET_NAME, 4)
        market.factionId = niko_MPC_ids.IAIIC_FAC_ID
        market.primaryEntity = FOBStation
        FOBStation.market = market
        market.name = FOB_MARKET_NAME

        market.addIndustry(Industries.POPULATION)
        market.addIndustry(Industries.MEGAPORT)
        market.addIndustry(Industries.STARFORTRESS_HIGH)
        market.getIndustry(Industries.STARFORTRESS_HIGH).aiCoreId = Commodities.ALPHA_CORE // yes, theyre hypocrits
        market.addIndustry(Industries.HIGHCOMMAND)
        val HC = market.getIndustry(Industries.HIGHCOMMAND) as MilitaryBase
        HC.isImproved = true
        HC.aiCoreId = Commodities.ALPHA_CORE
        market.addIndustry(Industries.HEAVYBATTERIES)
        market.getIndustry(Industries.HEAVYBATTERIES).isImproved = true
        market.getIndustry(Industries.HEAVYBATTERIES).specialItem = SpecialItemData(Items.DRONE_REPLICATOR, null)
        market.getIndustry(Industries.HEAVYBATTERIES).aiCoreId = Commodities.ALPHA_CORE
        market.addIndustry(Industries.ORBITALWORKS)
        market.getIndustry(Industries.ORBITALWORKS).specialItem = SpecialItemData(Items.CORRUPTED_NANOFORGE, null)
        market.addIndustry(Industries.WAYSTATION)

        market.addCondition("MPC_FOB")
        market.addCondition(niko_MPC_ids.MPC_BENEFACTOR_CONDID)
        market.addCondition(Conditions.POPULATION_4)
        market.conditions.forEach { it.isSurveyed = true }
        market.surveyLevel = MarketAPI.SurveyLevel.FULL
        if (niko_MPC_settings.indEvoEnabled) {
            market.addCondition(Ids.COND_MINERING)
            (market.getCondition(Ids.COND_MINERING).plugin as MineFieldCondition).addMineField()
        }

        market.addSubmarket(Submarkets.SUBMARKET_OPEN)
        market.addSubmarket(Submarkets.SUBMARKET_STORAGE)

        market.isUseStockpilesForShortages = true
        market.commDirectory.addPerson(MPC_People.getImportantPeople()[MPC_People.IAIIC_LEADER])
        market.admin = MPC_People.getImportantPeople()[MPC_People.IAIIC_LEADER]
        /*val submarket = market.getLocalResources() as LocalResourcesSubmarketPlugin
        for (com in market.commoditiesCopy) {
            val bonus = market.getStockpileNumConsumedOverTime(com, 365f, 0)
            submarket.getStockpilingBonus(com.id).modifyFlat("MPC_reserveMaterials", bonus)
            com.stockpile = bonus
        }*/

        Global.getSector().economy.addMarket(market, true)
        return market
    }

    override fun createFleet(system: StarSystemAPI?, random: Random?): CampaignFleetAPI? {

        var difficulty = 3 + random!!.nextInt(2)

        val m = FleetCreatorMission(random)
        m.beginFleet()

        val loc = system!!.location

        val factionId = Factions.INDEPENDENT
        m.createStandardFleet(difficulty, factionId, loc)

        val factionPicker = WeightedRandomPicker<String>()
        contributingFactionsToWeight.forEach { factionPicker.add(it.key, it.value) }
        var pickedFaction: FactionAPI? = null
        while (!factionPicker.isEmpty) {
            val tempPickedFaction = Global.getSector().getFaction(factionPicker.pickAndRemove()) ?: continue
            if (tempPickedFaction.getMarketsCopy().isNotEmpty()) {
                pickedFaction = tempPickedFaction
                break
            }
        }
        if (pickedFaction == null) {
            niko_MPC_debugUtils.log.error("SOMEHOW THERES NO VALID CONTRIBUTING FACTIONS FOR THE BACKLASH")
        } else {
            m.setFleetSource(pickedFaction.getMarketsCopy().random())
        }

        //val factionId = pickedFaction?.id ?: Factions.INDEPENDENT

        val picker = WeightedRandomPicker<String>()
        fleetTypesToWeight.entries.forEach { picker.add(it.key, it.value) }
        val fleetType = picker.pick()

        m.triggerSetFleetType(fleetType)
        m.triggerSetTraderFleet()
        m.triggerSetFleetDoctrineComp(3, 0, 2)
        m.triggerSetFleetComposition(2f, 0.3f, 0.3f, 0.0f, 0f)

        //m.triggerSetFleetHasslePlayer(SPY_HASSLE_REASON)
        m.addTag(niko_MPC_ids.MPC_SPY_FLEET_TAG)

        //m.triggerMakeLowRepImpact() // this is basically just a q-ship

        val fleet = m.createFleet()
        //fleet.setFaction(Factions.INDEPENDENT, true)

        //fleet?.addScript(NPCHassler(fleet, system))
        val assignmentPicker = WeightedRandomPicker<MPC_spyAssignmentTypes>()
        assignmentPicker.addAll(MPC_spyAssignmentTypes.values().toList())
        val assignment = assignmentPicker.pick().getInstance()
        MPC_spyFleetScript(fleet, system, assignment).start()
        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.addTag(niko_MPC_ids.MPC_SPY_FLEET_TAG)
        //fleet.addEventListener(MPC_spyFleetEventListener())

        return fleet
    }

    override fun getSpawnFrequency(system: StarSystemAPI?): Float {
        return super.getSpawnFrequency(system) * 0.1f
    }

    override fun getMaxNumFleets(system: StarSystemAPI?): Int {
        return 1
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator? {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val opad = 10f
                tooltip.addPara(
                    "The Hegemony considers the use of AI cores illegal and will not tolerate it "
                            + "even outside the volume of the core worlds.", 0f
                )
                tooltip.addPara(
                    ("After their first defeat, the hegemony has resorted to more covert methods of interference, with the help of " +
                    "other major factions. Their methods are unknown, but their motives certainly aren't."), opad
                )
            }
        }
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        var shouldShowDueToCause = false
        for (cause in getCauses()) {
            shouldShowDueToCause = shouldShowDueToCause || cause.shouldShow()
        }
        return getProgress(intel) > 0 || shouldShowDueToCause
    }

}
package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.InteractionDialogImageVisual
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.MPC_delayedExecutionNonLambda
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.MPC_fractalCrisisHelpers.respawnAllFleets
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignmentTypes
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.addMarketPeople
import data.utilities.niko_MPC_settings
import data.utilities.niko_MPC_settings.grandColoniesEnabled
import exerelin.ExerelinConstants
import indevo.exploration.minefields.conditions.MineFieldCondition
import indevo.ids.Ids
import lunalib.lunaExtensions.getMarketsCopy
import org.lazywizard.lazylib.MathUtils
import org.magiclib.kotlin.makeImportant
import org.magiclib.kotlin.makeNonStoryCritical
import org.magiclib.kotlin.makeStoryCritical
import java.awt.Color
import java.util.*
import niko_SA.MarketUtils.addStationAugment
import niko_SA.augments.core.BuiltInMode

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
            if (Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.IAIIC_EVENT_CONCLUDED)) return false
            if (!Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.DID_HEGEMONY_SPY_VISIT)) return false // this is the confirmation
            if (MPC_hegemonyFractalCoreCause.getFractalColony() == null) return false
            if (getContributingFactions().isEmpty()) return false
            if (!checkFactionExists(Factions.HEGEMONY, true)) return false

            return true
        }

        fun MarketAPI.addSpecialItems() {
            industries.forEach { it.aiCoreId = Commodities.BETA_CORE }

            getIndustry(Industries.HIGHCOMMAND)?.aiCoreId = Commodities.ALPHA_CORE
            getIndustry(Industries.STARFORTRESS_HIGH)?.aiCoreId = Commodities.ALPHA_CORE
            getIndustry(Industries.HEAVYBATTERIES)?.aiCoreId = Commodities.ALPHA_CORE
            getIndustry("IndEvo_embassy")?.aiCoreId = Commodities.GAMMA_CORE
            getIndustry("IndEvo_Academy")?.aiCoreId = Commodities.GAMMA_CORE
            //getIndustry(Industries.ORBITALWORKS)?.aiCoreId = Commodities.BETA_CORE
            getIndustry(Industries.HEAVYBATTERIES)?.specialItem = SpecialItemData(Items.DRONE_REPLICATOR, null)
            getIndustry(Industries.ORBITALWORKS)?.specialItem = SpecialItemData(Items.CORRUPTED_NANOFORGE, null)
            getIndustry(Industries.ORBITALWORKS)?.aiCoreId = Commodities.BETA_CORE
            getIndustry("triheavy")?.specialItem = SpecialItemData(Items.CORRUPTED_NANOFORGE, null)
            getIndustry("triheavy")?.aiCoreId = Commodities.BETA_CORE
            //getIndustry("militarygarrison")?.aiCoreId = Commodities.ALPHA_CORE
            getIndustry("IndEvo_IntArray")?.specialItem = SpecialItemData("IndEvo_transmitter", null)
            getIndustry("logisitcbureau")?.aiCoreId = Commodities.BETA_CORE
            getIndustry(Industries.WAYSTATION)?.aiCoreId = Commodities.BETA_CORE
            getIndustry("yunru_shipbays")?.aiCoreId = Commodities.ALPHA_CORE
        }

        fun setImportantMarkets() {
            Global.getSector().economy.getMarket("sindria")?.makeStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("umbra")?.makeStoryCritical("\$MPC_IAIICEvent")
            //Global.getSector().economy.getMarket("culann")?.makeStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("eochu_bres")?.makeStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("new_maxios")?.makeStoryCritical("\$MPC_IAIICEvent")
        }
        fun unSetImportantMarkets() {
            Global.getSector().economy.getMarket("sindria")?.makeNonStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("umbra")?.makeNonStoryCritical("\$MPC_IAIICEvent")
            //Global.getSector().economy.getMarket("culann")?.makeNonStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("eochu_bres")?.makeNonStoryCritical("\$MPC_IAIICEvent")
            Global.getSector().economy.getMarket("new_maxios")?.makeNonStoryCritical("\$MPC_IAIICEvent")
        }

        fun createMarket(FOBStation: SectorEntityToken, isReclaimed: Boolean = false): MarketAPI {
            val market = Global.getFactory().createMarket(FOB_MARKET_ID, FOB_MARKET_NAME, 3)
            if (isReclaimed) {
                market.factionId = Factions.PLAYER
                market.isPlayerOwned = true
            } else {
                market.factionId = niko_MPC_ids.IAIIC_FAC_ID
                market.makeStoryCritical("\$MPC_IAIICEvent")
            }
            market.primaryEntity = FOBStation
            FOBStation.market = market
            market.name = FOB_MARKET_NAME
            if (!isReclaimed) {
                FOBStation.setFaction(niko_MPC_ids.IAIIC_FAC_ID)
            } else {
                FOBStation.setFaction(Factions.PLAYER)
            }

            market.addIndustry(Industries.POPULATION)
            market.addIndustry(Industries.MEGAPORT)
            market.addIndustry(Industries.STARFORTRESS_HIGH)
            market.addIndustry(Industries.HIGHCOMMAND)
            val HC = market.getIndustry(Industries.HIGHCOMMAND) as MilitaryBase
            HC.isImproved = true
            market.addIndustry(Industries.HEAVYBATTERIES)
            market.getIndustry(Industries.HEAVYBATTERIES).isImproved = true
            if (niko_MPC_settings.AOTD_vaultsEnabled) {
                market.addIndustry("triheavy") // skunkworks
                //market.addIndustry("militarygarrison")
                market.addIndustry("logisitcbureau") // terminus
                //market.getIndustry("militarygarrison")?.isImproved = true
            } else {
                market.addIndustry(Industries.ORBITALWORKS)
                market.addIndustry(Industries.WAYSTATION)
            }
            var shouldAddReq = true
            if (niko_MPC_settings.yunruIndustriesEnabled) {
                market.addIndustry("yunru_shipbays")
                market.getIndustry("yunru_shipbays")?.isImproved = true
                shouldAddReq = grandColoniesEnabled
            }
            if (niko_MPC_settings.indEvoEnabled) {
                market.addIndustry("IndEvo_embassy")
                if (shouldAddReq) market.addIndustry("IndEvo_ReqCenter")
                market.addIndustry("IndEvo_IntArray")
                market.addIndustry("IndEvo_Academy")
            }
            if (!isReclaimed) market.addIndustry("MPC_FOBIAIICPatherResist")

            if (niko_MPC_settings.stationAugmentsLoaded) {
                market.getIndustry(Industries.STARFORTRESS_HIGH).isImproved = true

                market.addStationAugment("SA_tacticalLink")?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_bubbleShield")?.builtInMode = BuiltInMode.NORMAL

                market.addStationAugment("SA_commsCenter")
                market.addStationAugment("SA_ecmPackage")
                market.addStationAugment("SA_navRelay")
            }

            market.addSpecialItems()

            market.addCondition("MPC_FOB")
            if (!isReclaimed) {
                market.addCondition(niko_MPC_ids.MPC_BENEFACTOR_CONDID)
                market.addCondition(Conditions.POPULATION_4)
                market.size = 4
            } else market.addCondition(Conditions.POPULATION_3)
            market.conditions.forEach { it.isSurveyed = true }
            market.surveyLevel = MarketAPI.SurveyLevel.FULL
            if (niko_MPC_settings.indEvoEnabled) {
                market.memoryWithoutUpdate[MineFieldCondition.NO_ADD_BELT_VISUAL] = true
                market.addCondition(Ids.COND_MINERING)
                (market.getCondition(Ids.COND_MINERING).plugin as MineFieldCondition).addMineField()
            }

            if (!isReclaimed) {
                market.addSubmarket(Submarkets.SUBMARKET_OPEN)
                market.addSubmarket(Submarkets.SUBMARKET_BLACK)
            } else {
                market.addSubmarket(Submarkets.LOCAL_RESOURCES)
            }
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE)
            if (isReclaimed) (market.getSubmarket(Submarkets.SUBMARKET_STORAGE)?.plugin as? StoragePlugin)?.setPlayerPaidToUnlock(true)

            if (!isReclaimed) market.isUseStockpilesForShortages = true
            if (!isReclaimed) {
                market.commDirectory.addPerson(MPC_People.getImportantPeople()[MPC_People.IAIIC_LEADER])
                MPC_People.getImportantPeople()[MPC_People.IAIIC_LEADER]?.makeImportant("\$MPC_IAIICLeader")
                market.admin = MPC_People.getImportantPeople()[MPC_People.IAIIC_LEADER]

                market.memoryWithoutUpdate[ExerelinConstants.MEMORY_KEY_UNINVADABLE] = true
                /*val submarket = market.getLocalResources() as LocalResourcesSubmarketPlugin
            for (com in market.commoditiesCopy) {
                val bonus = market.getStockpileNumConsumedOverTime(com, 365f, 0)
                submarket.getStockpilingBonus(com.id).modifyFlat("MPC_reserveMaterials", bonus)
                com.stockpile = bonus
            }*/
            } else {
                market.admin = Global.getSector().playerPerson
            }
            addMarketPeople(market)
            Global.getSector().economy.addMarket(market, true)

            market.reapplyConditions()
            market.reapplyIndustries()

            if (isReclaimed) FOBStation.customDescriptionId = "MPC_IAIICFOBReclaimed"

            return market
        }
    }

    val checkInterval = IntervalUtil(0.2f, 0.2f)

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

    override fun advance(amount: Float) {
        super.advance(amount)

        checkInterval.advance(Misc.getDays(amount))
        if (checkInterval.intervalElapsed()) {
            abortIfNeeded()
        }
    }

    private fun abortIfNeeded() {
        val hostileActivity = HostileActivityEventIntel.get() ?: return
        val ourStage = hostileActivity.stages.firstOrNull { (it.rollData as? HAERandomEventData)?.factor == this }

        if (ourStage != null) {
            val fractalColony = MPC_hegemonyFractalCoreCause.getFractalColony()
            if (fractalColony == null) {
                hostileActivity.resetRandomizedStage(ourStage)
            }
        }
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
        val fobSpawned = spawnFOB(fractalColony, fractalSystem, foundDist) ?: return false
        setImportantMarkets()

        doDialog(getFOB()?.starSystem)

        return true
    }

    private fun doDialog(system: StarSystemAPI?) {
        Global.getSector().memoryWithoutUpdate["\$MPC_FOBSystemName"] = system?.name
        Global.getSector().campaignUI.showInteractionDialog(RuleBasedInteractionDialogPluginImpl("MPC_IAIICSpawned"), Global.getSector().playerFleet)

    }

    override fun getStageTooltipImpl(intel: HostileActivityEventIntel?, stage: EventStageData): TooltipCreator? {
        return getDefaultEventTooltip("Unknown Action", intel, stage)
    }

    private fun spawnFOB(fractalColony: MarketAPI, fractalSystem: StarSystemAPI, foundDist: Float): Boolean {
        val station = fractalColony.starSystem.addCustomEntity(niko_MPC_ids.MPC_FOB_ID, FOB_MARKET_NAME, "MPC_IAIICFOB", niko_MPC_ids.IAIIC_FAC_ID)
        station.customInteractionDialogImageVisual = InteractionDialogImageVisual("graphics/illustrations/industrial_megafacility.jpg", 480f, 300f)
        station.setCircularOrbitPointingDown(fractalSystem.center, MathUtils.getRandomNumberInRange(0f, 360f), foundDist, 420f)
        val market = createMarket(station)
        station.makeImportant(niko_MPC_ids.MPC_FOB_ID, Float.MAX_VALUE)

        initFOBFleets(station, market, fractalSystem)

        class MPC_respawnFleets(interval: IntervalUtil) : MPC_delayedExecutionNonLambda(interval) {
            override fun executeImpl() {
                market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("AAA", 500f)
                market.respawnAllFleets()
                market.stats.dynamic.getMod(Stats.FLEET_QUALITY_MOD).unmodify("AAA") // i dont know why
                // but the fleets spawn with a fuckload of dmods here for some reason. this is just to counter it
            }
        }
        MPC_respawnFleets(IntervalUtil(0.2f, 0.2f)).start()
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

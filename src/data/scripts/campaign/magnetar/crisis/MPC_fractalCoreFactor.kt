package data.scripts.campaign.magnetar.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.submarkets.LocalResourcesSubmarketPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.magnetar.crisis.assignments.MPC_spyAssignmentTypes
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_marketUtils.getStockpileNumConsumedOverTime
import lunalib.lunaExtensions.getMarketsCopy
import org.magiclib.kotlin.getLocalResources
import java.awt.Color
import java.util.*

class MPC_fractalCoreFactor(intel: HostileActivityEventIntel?) : BaseHostileActivityFactor(intel) {

    companion object {
        const val FOB_MARKET_ID = "MPC_fractalBacklashFOB"
        val fleetTypesToWeight = hashMapOf(
            Pair(FleetTypes.TRADE, 5f),
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
        const val SPY_HASSLE_REASON = "MPC_spyFleet"

        fun getFOB(): MarketAPI? = Global.getSector().economy.getMarket(FOB_MARKET_ID)
        fun getContributingFactions(): List<FactionAPI> {
            return contributingFactionsToWeight.keys.map { Global.getSector().getFaction(it) }.filter { it != null && it.getMarketsCopy().isNotEmpty() }
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
            "to suggest a major hostile operation may occur in the near future. Little more is known - whoever is doing this likely " +
            "has good OpSec.", opad
        )
        stage.beginResetReqList(info, true, "crisis", opad)
        info.addPara("Unknown", 0f)
        stage.endResetReqList(info, false, "crisis", -1, -1)
        addBorder(info, Global.getSector().getFaction(Factions.NEUTRAL).baseUIColor)
    }

    override fun getEventStageIcon(intel: HostileActivityEventIntel?, stage: EventStageData?): String? {
        return Global.getSector().getFaction(Factions.NEUTRAL).crest
    }

    override fun getEventFrequency(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?): Float {
        if (getContributingFactions().isEmpty()) return 0f
        if (getFOB() != null) return 0f
        if (Global.getSector().memoryWithoutUpdate[niko_MPC_ids.PLAYER_DEFENDED_FRACTAL_CORE] == true) return 0f

        return 10f
    }

    override fun fireEvent(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?): Boolean {
        return super.fireEvent(intel, stage)
    }

    protected fun createMarket(FOBStation: SectorEntityToken) {
        val market = Global.getFactory().createMarket(FOB_MARKET_ID, "IAIC FOB", 4)
        market.factionId = Factions.INDEPENDENT
        FOBStation.market = market

        market.addIndustry(Industries.STARFORTRESS_HIGH)
        market.addIndustry(Industries.HIGHCOMMAND)
        market.getIndustry(Industries.HIGHCOMMAND).isImproved = true
        market.addIndustry(Industries.HEAVYBATTERIES)
        market.getIndustry(Industries.HEAVYBATTERIES).isImproved = true
        market.addIndustry(Industries.ORBITALWORKS)
        market.addIndustry(Industries.WAYSTATION)

        market.addCondition("MPC_FOB")

        market.isUseStockpilesForShortages = true
        val submarket = market.getLocalResources() as? LocalResourcesSubmarketPlugin ?: return
        for (com in market.commoditiesCopy) {
            val bonus = market.getStockpileNumConsumedOverTime(com, 365f, 0)
            submarket.getStockpilingBonus(com.id).modifyFlat("MPC_reserveMaterials", bonus)
            com.stockpile = market.getStockpileNumConsumedOverTime(com, 365f, 0)
        }
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
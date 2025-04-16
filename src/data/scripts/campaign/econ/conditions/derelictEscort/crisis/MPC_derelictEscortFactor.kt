package data.scripts.campaign.econ.conditions.derelictEscort.crisis

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseIntel
import com.fs.starfarer.api.impl.campaign.intel.events.*
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel.EventStageData
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel.HAERandomEventData
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel.FGIEventListener
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI.GenericRaidParams
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission.FleetStyle
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.rulecmd.KantaCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.econ.conditions.derelictEscort.niko_MPC_derelictEscort
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_mathUtils.trimHangingZero
import lunalib.lunaExtensions.getMarketsCopy
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.sign

class MPC_derelictEscortFactor(intel: HostileActivityEventIntel?): BaseHostileActivityFactor(intel), FGIEventListener {

    companion object {

        fun getMarketWithHighestFRCTime(): MarketAPI? {
            var highestUptime = 0f
            var market: MarketAPI? = null
            for (iterMarket in Global.getSector().playerFaction.getMarketsCopy()) {
                val FRC = niko_MPC_derelictEscort.get(iterMarket) ?: continue
                if (FRC.daysActive > highestUptime) {
                    market = iterMarket
                    highestUptime = FRC.daysActive
                }
            }
            return market
        }

        fun isActive(): Boolean {
            if (!Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_sawPiratesComplainingAboutFRC")) return false
            if (Global.getSector().memoryWithoutUpdate.getBoolean("\$MPC_FRCRaidDone")) return false
            if (getMarketWithHighestFRCTime() == null) return false
            return Global.getSector().getFaction(Factions.PIRATES).relToPlayer.isHostile
        }
    }

    override fun getNameColorForThreatList(): Color? {
        return Global.getSector().getFaction(Factions.PIRATES).baseUIColor
    }

    override fun getNameForThreatList(first: Boolean): String {
        return "Disgruntled Pirates"
    }

    override fun getDescColor(intel: BaseEventIntel?): Color? {
        return if (getProgress(intel) <= 0) {
            Misc.getGrayColor()
        } else Global.getSector().getFaction(Factions.PIRATES).baseUIColor
    }

    override fun getDesc(intel: BaseEventIntel?): String {
        return "Disgruntled Pirates"
    }

    override fun getMainRowTooltip(intel: BaseEventIntel?): TooltipCreator? {
        return object : BaseFactorTooltip() {
            override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                val market = getMarketWithHighestFRCTime() ?: return
                val opad = 10f
                tooltip.addPara(
                    "Your extended use of the %s on %s has managed to uniquely anger nearly every pirate in the sector.",
                    0f,
                    Misc.getHighlightColor(),
                    "frontier reinforcement center", market.name
                ).setHighlightColors(
                    Misc.getHighlightColor(),
                    market.faction.baseUIColor
                )
                if (KantaCMD.playerHasProtection()) {
                    tooltip.addPara(
                        "While some pirates are angry enough to ignore %s, the vast majority will choose to leave you alone.",
                        opad, Misc.getPositiveHighlightColor(), "Kanta's protection"
                    )
                } else {
                    if (KantaCMD.playerEverHadProtection()) {
                        tooltip.addPara(
                            "You've %s, and it's not the sort of thing you can do over.",
                            opad, Misc.getNegativeHighlightColor(), "lost Kanta's protection"
                        )
                    } else {
                        tooltip.addPara(
                            "Having %s, however, should be enough dissuade most pirates from attacking your interests - but such a large frustration as this %s.",
                            opad, Misc.getHighlightColor(), "Kanta's protection", "cannot be erased"
                        ).setHighlightColors(
                            Misc.getHighlightColor(), Misc.getNegativeHighlightColor()
                        )
                    }
                }
            }
        }
    }

    override fun shouldShow(intel: BaseEventIntel?): Boolean {
        return getProgress(intel) > 0
    }

    override fun getMaxNumFleets(system: StarSystemAPI?): Int {
        return if (getProgress(intel) <= 0) {
            1
        } else super.getMaxNumFleets(system)
    }

    override fun createFleet(system: StarSystemAPI, random: Random): CampaignFleetAPI? {
        var f = 0f
        f += getEffectMagnitude(system)
        if (f > 1f) f = 1f
        var difficulty = 0
        difficulty += (f * 7f).roundToInt()

//		int size = 0;
//		for (MarketAPI market : Misc.getMarketsInLocation(system, Factions.PLAYER)) {
//			size = Math.max(market.getSize(), size);
//		}
//		int minDiff = Math.max(0, size - 2);
        var mult = 1f
        if (getProgress(intel) <= 0) {
            mult = 0.5f
        }
        val minDiff = (intel.getMarketPresenceFactor(system) * 6f * mult).roundToInt()
        if (difficulty < minDiff) difficulty = minDiff
        difficulty += random.nextInt(4)
        val m = FleetCreatorMission(random)
        m.beginFleet()
        val loc = system.location
        val factionId = Factions.PIRATES
        m.createStandardFleet(difficulty, factionId, loc)
        m.triggerSetPirateFleet()
        m.triggerMakeLowRepImpact()
        //m.triggerFleetAllowLongPursuit();
        return m.createFleet()
    }

    override fun getEventFrequency(intel: HostileActivityEventIntel?, stage: BaseEventIntel.EventStageData?): Float {
        return 3f
    }

    //	public void resetEvent(HostileActivityEventIntel intel, EventStageData stage) {
    //		super.resetEvent(intel, stage);
    //	}
    override fun rollEvent(intel: HostileActivityEventIntel, stage: EventStageData) {
//		if (true) return;
        val data = HAERandomEventData(this, stage)
        stage.rollData = data
        intel.sendUpdateIfPlayerHasIntel(data, false)
    }

    override fun fireEvent(intel: HostileActivityEventIntel?, stage: EventStageData): Boolean {
        val target: MarketAPI? = getMarketWithHighestFRCTime()
        val source: MarketAPI? = findRaidSource(intel, stage, target?.starSystem)
        if (source == null || target == null) {
            return false
        }
        stage.rollData = null
        return startRaid(source, target, stage, getRandomizedStageRandom(5))
    }

    fun findRaidSource(intel: HostileActivityEventIntel?, stage: EventStageData?, target: StarSystemAPI?): MarketAPI? {
        if (target == null) return null
        val list: MutableList<MarketAPI> = ArrayList()
        val maxDist = Global.getSettings().getFloat("sectorWidth") * 0.5f
        for (curr in Global.getSector().intelManager.getIntel(PirateBaseIntel::class.java)) {
            val base = curr as PirateBaseIntel
            if (base.playerHasDealWithBaseCommander()) continue
            val dist = Misc.getDistance(target.location, base.market.locationInHyperspace)
            if (dist > maxDist) continue
            list.add(base.market)
        }
        for (market in Global.getSector().economy.marketsCopy) {
            if (Factions.PIRATES == market.faction.id) {
                for (other in Misc.getMarketsInLocation(market.containingLocation)) {
                    if (other === market) continue
                    if (!other.faction.isHostileTo(market.faction)) continue
                    if (other.size <= market.size - 2) continue
                    val dist = Misc.getDistance(market.primaryEntity.location, other.primaryEntity.location)
                    if (dist < 8000) continue
                    list.add(market)
                }
            }
        }
        list.sortWith { m1, m2 ->
            val d1 = Misc.getDistance(target.location, m1.locationInHyperspace)
            val d2 = Misc.getDistance(target.location, m2.locationInHyperspace)
            sign(d1 - d2).toInt()
        }
        val picker = WeightedRandomPicker<MarketAPI>(randomizedStageRandom)
        var i = 0
        while (i < list.size && i < 4) {
            val market = list[i]
            val dist = Misc.getDistance(target.location, market.locationInHyperspace)
            val w = 100000f / (dist * dist)
            picker.add(market, w)
            i++
        }
        return picker.pick()
    }

    fun startRaid(source: MarketAPI, target: MarketAPI, stage: EventStageData, random: Random): Boolean {
        val params = GenericRaidParams(Random(random.nextLong()), true)
        params.factionId = source.factionId
        params.source = source
        params.prepDays = 7f + random.nextFloat() * 14f
        params.payloadDays = 27f + 7f * random.nextFloat()
        params.raidParams.where = target.starSystem
        val targetSystem = target.starSystem
        for (market in Misc.getMarketsInLocation(target.starSystem)) {
            if (market.faction.isHostileTo(source.faction) || market.faction.isPlayerFaction) {
                params.raidParams.allowedTargets.add(market)
            }
        }
        if (params.raidParams.allowedTargets.isEmpty()) return false
        params.raidParams.allowNonHostileTargets = true
        params.style = FleetStyle.STANDARD
        if (stage.id == HostileActivityEventIntel.Stage.MINOR_EVENT) {
            params.fleetSizes.add(5)
            params.fleetSizes.add(3)
            params.memoryKey = PirateHostileActivityFactor.SMALL_RAID_KEY
        } else {
            params.memoryKey = PirateHostileActivityFactor.RAID_KEY
            var mag1 = getEffectMagnitude(targetSystem)
            if (mag1 > 1f) mag1 = 1f
            val mag2 = intel.getMarketPresenceFactor(targetSystem)

            var totalDifficulty = 2f + mag1 * 0.25f + mag2 * 0.5f * 100f

            val r = getRandomizedStageRandom(7)
            if (r.nextFloat() < 0.33f) {
                params.style = FleetStyle.QUANTITY
            }
            while (totalDifficulty > 0) {
                var max = Math.min(10f, totalDifficulty * 0.5f)
                val min = Math.max(2f, max - 2)
                if (max < min) max = min
                val diff = Math.round(StarSystemGenerator.getNormalRandom(r, min, max))
                params.fleetSizes.add(diff)
                totalDifficulty -= diff.toFloat()
            }
        }
        val base = PirateBaseIntel.getIntelFor(source)
        if (base != null) {
            if (Misc.isHiddenBase(source) && !base.isPlayerVisible) {
                base.makeKnown()
                base.sendUpdateIfPlayerHasIntel(PirateBaseIntel.DISCOVERED_PARAM, false)
            }
        }
        val raid = GenericRaidFGI(params)
        raid.listener = this
        Global.getSector().intelManager.addIntel(raid)
        Global.getSector().memoryWithoutUpdate["\$MPC_FRCRaidDone"] = true
        return true
    }

    override fun addBulletPointForEvent(
        intel: HostileActivityEventIntel?, stage: EventStageData?, info: TooltipMakerAPI,
        mode: ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float
    ) {
        info.addPara("Rumors of pirate raid", tc, initPad)
//		Color c = Global.getSector().getFaction(Factions.PIRATES).getBaseUIColor();
//		info.addPara("Rumors of pirate raid", initPad, tc, c, "pirate raid");
    }

    override fun addBulletPointForEventReset(
        intel: HostileActivityEventIntel?, stage: EventStageData?, info: TooltipMakerAPI,
        mode: ListInfoMode?, isUpdate: Boolean, tc: Color?, initPad: Float
    ) {
        info.addPara("Pirate raid averted", tc, initPad)
    }

    override fun addStageDescriptionForEvent(
        intel: HostileActivityEventIntel?,
        stage: EventStageData,
        info: TooltipMakerAPI
    ) {
        var small = 0f
        val opad = 10f
        small = 8f
        val market = getMarketWithHighestFRCTime() ?: return
        //		info.addPara("There are rumors that a pirate raid targeting your colonies "
//				+ "may be organized in the near future.", opad);
//
//		info.addPara(BaseIntelPlugin.BULLET + "If the raid is successful, the targeted colonies will suffer from reduced stability.", opad,
//				Misc.getNegativeHighlightColor(), "reduced stability");
//
//		info.addPara(BaseIntelPlugin.BULLET + "If the raid is defeated, your colonies will gain "
//				+ "increased accessibility for several cycles.",
//				0f, Misc.getPositiveHighlightColor(), "increased accessibility");
        info.addPara(
            "Pirate frustration over your use of a %s on %s has reached a breaking point, and rumors of a raid to disrupt the dockyards are spreading." +
            "This will most likely fail - however, the disruption effect on your colonies cannot be understated.", small,
            Misc.getHighlightColor(), "frontier reinforcement center", market.name
        ).setHighlightColors(
            Misc.getHighlightColor(),
            market.faction.baseUIColor
        )
        info.addPara(
            "The raid will primarily target %s, but will attempt to raid other colonies if possible, resulting in %s for each.",
            opad,
            Misc.getNegativeHighlightColor(),
            market.name, "reduced stability"
        ).setHighlightColors(
            market.faction.baseUIColor,
            Misc.getNegativeHighlightColor()
        )

        info.addPara(
            "If the raid is defeated, FRCs across the sector will add an additional %s to each in-system colony - including it's host.", opad,
            Misc.getPositiveHighlightColor(),
            "${(niko_MPC_derelictEscort.RAID_DEFEATED_ACCESSIBILITY_BONUS * 100f).trimHangingZero()}% accessibility"
        )

        //if (stage.id == Stage.MINOR_EVENT) {
        stage.addResetReq(info, false, "crisis", -1, -1, opad)
        //		} else {
//			stage.beginResetReqList(info, true, "crisis", opad);
        // want to keep this less prominent, actually, so: just the above
//			info.addPara("An agreement is reached with Kanta, the pirate queen",
//					0f, Global.getSector().getFaction(Factions.LUDDIC_PATH).getBaseUIColor(), "Luddic Path");
//			stage.endResetReqList(info, false, "crisis", -1, -1);
//		}
        addBorder(info, Global.getSector().getFaction(Factions.PIRATES).baseUIColor)


//		Color c = Global.getSector().getFaction(Factions.PIRATES).getBaseUIColor();
//		UIComponentAPI rect = info.createRect(c, 2f);
//		info.addCustomDoNotSetPosition(rect).getPosition().inTL(-small, 0).setSize(
//				info.getWidthSoFar() + small * 2f, Math.max(64f, info.getHeightSoFar() + small));
    }

    override fun getEventStageIcon(intel: HostileActivityEventIntel?, stage: EventStageData?): String? {
        return Global.getSector().getFaction(Factions.PIRATES).crest
    }

    override fun reportFGIAborted(intel: FleetGroupIntel?) {
        Global.getSector().memoryWithoutUpdate[niko_MPC_ids.DEFEATED_FRC_RAID] = true
    }

}
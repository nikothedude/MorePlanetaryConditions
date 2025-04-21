package data.scripts.campaign.magnetar.crisis.intel.hegemony

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.ColonyPlayerHostileActListener
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetAssignmentAI.EconomyRouteData
import com.fs.starfarer.api.impl.campaign.fleets.EconomyFleetRouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.intel.events.BaseEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.impl.campaign.intel.events.ttcr.TriTachyonCommerceRaiding
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.*
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.TimeoutTracker
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.MPC_People
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel.RetaliateReason
import data.utilities.niko_MPC_ids
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.isPatrol
import org.magiclib.kotlin.isWarFleet
import java.awt.Color
import java.util.*
import kotlin.math.roundToInt

class MPC_hegemonyMilitaristicHouseEventIntel: BaseEventIntel(), ColonyPlayerHostileActListener, FleetEventListener {

    companion object {
        fun get(withUpdate: Boolean = false): MPC_hegemonyMilitaristicHouseEventIntel? {
            if (withUpdate) {
                if (Global.getSector().memoryWithoutUpdate[KEY] == null) {
                    val intel = MPC_hegemonyMilitaristicHouseEventIntel()
                    Global.getSector().intelManager.addIntel(intel)
                    Global.getSector().memoryWithoutUpdate[KEY] = intel
                }
            }
            return Global.getSector().memoryWithoutUpdate[KEY] as? MPC_hegemonyMilitaristicHouseEventIntel
        }

        fun addStartLabel(info: TooltipMakerAPI) {
            val faction = Global.getSector().getFaction(Factions.HEGEMONY)
            val hege = faction.baseUIColor

            val IAIICfac = Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
            val IAIIC = IAIICfac.baseUIColor

            val label = info.addPara(
                "The noble house %s of Eventide has expressed discontent at the involvement the %s has with the %s, but " +
                        "are unwilling to act out of loyalty to the establishment and lack of threat posed by your faction. You must 'convince' them " +
                        "that continued collaboration with the %s will result in a rather %s for them and their investments.",
                0f,
                Misc.getHighlightColor(),
                "Mellour", "hegemony", "IAIIC", "IAIIC", "unfavorable outcome"
            ).setHighlightColors(
                hege, hege, IAIIC, IAIIC, Misc.getHighlightColor()
            )
        }

        fun isTraderServingAHegColony(fleet: CampaignFleetAPI): Boolean {
            val trader = Misc.isTrader(fleet)
            val smuggler = Misc.isSmuggler(fleet)

            if (!trader && !smuggler) return false

            val route = RouteManager.getInstance().getRoute(EconomyFleetRouteManager.SOURCE_ID, fleet)
            if (route == null) return false

            val data = route.getCustom() as EconomyRouteData?
            if (data == null) return false

            if (data.from != null && Factions.HEGEMONY == data.from.factionId) {
                return true
            }
            if (data.to != null && Factions.HEGEMONY == data.to.factionId) {
                return true
            }

            return false
        }

        fun isHegePatrolOrWarFLeet(fleet: CampaignFleetAPI): Boolean {
            val patrol = fleet.isPatrol()
            val warFleet = fleet.isWarFleet()

            if (!patrol && !warFleet) return false

            return fleet.faction.id == Factions.HEGEMONY
        }

        fun computeHegeCRProgressPoints(fleetPointsDestroyed: Float): Int {
            if (fleetPointsDestroyed <= 0) return 0

            var points = (fleetPointsDestroyed / FP_PER_POINT.toFloat()).roundToInt()
            if (points < 1) points = 1
            return points.coerceAtMost(MAXIMUM_FLEET_POINTS)
        }

        fun computeIndustryDisruptedPoints(ind: Industry): Int {
            val base = ind.spec.disruptDanger.disruptionDays
            val per = BASE_POINTS_FOR_INDUSTRY_DISRUPT.toFloat()

            val days = ind.disruptedDays

            val points = (days / base * per).roundToInt().coerceAtMost(MAX_POINTS_FOR_INDUSTRY_DISRUPT)
            return points
        }

        const val BASE_POINTS_FOR_INDUSTRY_DISRUPT = 10f
        const val MAX_POINTS_FOR_INDUSTRY_DISRUPT = 50

        const val TRADE_FLEET_FP_MULT = 4f
        const val MIL_FLEET_FP_MULT = 2f
        const val FP_PER_POINT = 10f

        const val MAXIMUM_FLEET_POINTS = 75

        const val MAX_PROGRESS = 300
        const val KEY = "\$MPC_hegemonyMilitaristicHouseEventIntel"
    }

    protected var recentlyDisrupted: TimeoutTracker<Industry> = TimeoutTracker<Industry>()

    enum class Stage {
        START,
        KILLFLEETS,
        SABOTAGE,
        END;
    }

    init {
        maxProgress = MAX_PROGRESS

        addStage(
            Stage.START,
            0
        )

        addStage(
            Stage.KILLFLEETS,
            (maxProgress * 0.25f).toInt(),
            true,
            StageIconSize.MEDIUM
        )

        addStage(
            Stage.SABOTAGE,
            (maxProgress * 0.75f).toInt(),
            true,
            StageIconSize.MEDIUM
        )

        addStage(
            Stage.END,
            maxProgress,
            true,
            StageIconSize.LARGE
        )

        addFactor(MPC_hegemonyMilHouseRaidIndFactorHint())
        addFactor(MPC_hegemonyPatrolsDestroyedFactorHint())
        addFactor(MPC_hegemonyTradeFleetsDestroyedFactorHint())
        //addFactor(MPC_hegemonyTradeFleetsDestroyedFactorHint())

        Global.getSector().listenerManager.addListener(this)
    }

    override fun getName(): String? {
        return "\"Convincing\" a noble house"
    }

    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String?> {
        val tags = super.getIntelTags(map)
        tags.add(Tags.INTEL_COLONIES)
        tags.add(Factions.HEGEMONY)
        tags.add(niko_MPC_ids.IAIIC_FAC_ID)
        return tags
    }

    override fun getBarColor(): Color {
        var color = getFaction().baseUIColor
        color = Misc.interpolateColor(color, Color.black, 0.25f)
        return color
    }

    override fun getIcon(): String? {
        return getFaction().crest
    }

    override fun getStageIconImpl(stageId: Any?): String? {
        return getFaction().crest
    }

    override fun getFactionForUIColors(): FactionAPI? {
        return Global.getSector().getFaction(Factions.HEGEMONY)
    }

    override fun advance(amount: Float) {
        super.advance(amount)

        val days = Misc.getDays(amount)
        recentlyDisrupted.advance(days)
    }

    override fun addBulletPoints(
        info: TooltipMakerAPI, mode: ListInfoMode?, isUpdate: Boolean,
        tc: Color?, initPad: Float
    ) {
        if (addEventFactorBulletPoints(info, mode, isUpdate, tc, initPad)) {
            return
        }

        val h = Misc.getHighlightColor()
        if (isUpdate && getListInfoParam() is EventStageData) {
            val esd = getListInfoParam() as EventStageData
            if (esd.id == Stage.END) {
                info.addPara("You've made your point - return to %s", initPad, Misc.getHighlightColor(), "eventide")
            } else if (esd.id == Stage.SABOTAGE) {
                info.addPara("Sabotage imminent", initPad)
            } else if (esd.id == Stage.KILLFLEETS) {
                info.addPara("Bounty hunters dispatched", initPad)
            }
            return
        }
    }

    override fun addStageDescriptionText(info: TooltipMakerAPI, width: Float, stageId: Any?) {
        val small = 0f
        Misc.getHighlightColor()


        //setProgress(0);
        //setProgress(199);
        //setProgress(600);
        //setProgress(899);
        //setProgress(1000);
        //setProgress(499);
        //setProgress(600);
        val stage = getDataFor(stageId)
        if (stage == null) return

        if (isStageActive(stageId)) {
            addStageDesc(info, stageId, small, false)
        }
    }

    fun getFaction(): FactionAPI {
        return Global.getSector().getFaction(Factions.HEGEMONY)
    }

    fun getIAIIC(): FactionAPI {
        return Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID)
    }

    fun getRep(): PersonAPI {
        return Global.getSector().importantPeople.getPerson(MPC_People.HEGE_MILITARIST_ARISTO_REP)
    }

    fun addStageDesc(info: TooltipMakerAPI, stageId: Any?, initPad: Float, forTooltip: Boolean) {
        Misc.getHighlightColor()

        val faction = getFaction()
        val hege = faction.baseUIColor

        val IAIICfac = getIAIIC()
        val IAIIC = IAIICfac.baseUIColor

        if (stageId == Stage.START) {
            addStartLabel(info)

        } else if (stageId == Stage.END) {
            info.addPara(
                "You've made your point clear. Return to %s to talk with %s.", initPad,
                hege,
                "eventide", getRep().nameString
            ).setHighlightColors(
                Misc.getHighlightColor(), hege
            )
        } else if (stageId == Stage.KILLFLEETS) {
            info.addPara(
                "A number of independent parties, negatively impacted by your actions, have opted to send a number of bounty hunters to eliminate you. " +
                "You're likely to encounter them in the coming months.", initPad
            )
        } else if (stageId == Stage.SABOTAGE) {
            val sabotagePower = MPC_IAIICFobIntel.get()!!.getSabotagePowerString()
            info.addPara(
                "INTSEC has become highly aggravated by your attacks, and is working with the IAIIC to sabotage your colonies. The IAIIC's current threat of sabotage is %s.",
                initPad,
                sabotagePower.second,
                sabotagePower.first
            )
        }
    }

    override fun getStageTooltipImpl(stageId: Any): TooltipCreator? {
        val esd = getDataFor(stageId)

        if (esd != null && EnumSet.of<Stage>(Stage.END, Stage.SABOTAGE, Stage.KILLFLEETS).contains(esd.id)) {
            return object : BaseFactorTooltip() {
                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any?) {
                    val opad = 10f

                    if (esd.id == Stage.END) {
                        tooltip.addTitle("Success!")
                    } else if (esd.id == Stage.KILLFLEETS) {
                        tooltip.addTitle("Killfleets dispatched")
                    } else if (esd.id == Stage.SABOTAGE) {
                        tooltip.addTitle("Sabotage imminent")
                    }

                    addStageDesc(tooltip, esd.id, opad, true)

                    esd.addProgressReq(tooltip, opad)
                }
            }
        }
        return null
    }

    override fun withMonthlyFactors(): Boolean {
        return false
    }

    override fun notifyStageReached(stage: EventStageData?) {
        super.notifyStageReached(stage)

        if (stage?.id == Stage.KILLFLEETS) {
            sendKillFleets()
        } else if (stage?.id == Stage.SABOTAGE) {
            MPC_IAIICFobIntel.get()!!.retaliate(RetaliateReason.MILITARY_HOUSE_PROGRESS)
        }
    }

    override fun notifyEnded() {
        super.notifyEnding()

        Global.getSector().listenerManager.removeListener(this)
        Global.getSector().memoryWithoutUpdate[KEY] = null
    }

    // LISTENERS

    override fun reportRaidForValuablesFinishedBeforeCargoShown(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        cargo: CargoAPI?
    ) {
        return
    }

    override fun reportRaidToDisruptFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?,
        industry: Industry?
    ) {
        if (market == null) return
        if (industry == null) return

        applyIndustryDisruption(industry, dialog)
    }

    override fun reportTacticalBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == null) return

        applyMassIndustryDisruption(
            market,
            dialog
        )
    }

    override fun reportSaturationBombardmentFinished(
        dialog: InteractionDialogAPI?,
        market: MarketAPI?,
        actionData: MarketCMD.TempData?
    ) {
        if (market == null) return

        applyMassIndustryDisruption(
            market,
            dialog
        )
    }

    fun applyMassIndustryDisruption(market: MarketAPI, dialog: InteractionDialogAPI?) {
        //if (TriTachyonHostileActivityFactor.isPlayerCounterRaidedTriTach()) return
        //if (getProgress(null) <= 0 && TriTachyonCommerceRaiding.get() == null) return

        var points = 0
        for (industry in market.industries) {
            if (recentlyDisrupted.contains(industry)) continue
            if (industry.spec.hasTag(Industries.TAG_UNRAIDABLE)) continue

            val curr = computeIndustryDisruptedPoints(industry)
            if (curr > 0) {
                points += curr
                recentlyDisrupted.add(industry, industry.disruptedDays)
            }
        }

        if (points > 0) {
            val factor = MPC_hegemonyMilHouseRaidIndFactor(
                points, "Disrupted industries " + market.onOrAt + " " + market.name
            )
            addFactor(factor, dialog)
        }
    }

    fun applyIndustryDisruption(industry: Industry, dialog: InteractionDialogAPI?) {
        //if (TriTachyonHostileActivityFactor.isPlayerCounterRaidedTriTach()) return
        //if (getProgress() <= 0) return

        if (!recentlyDisrupted.contains(industry)) {
            if (industry.spec.hasTag(Industries.TAG_UNRAIDABLE)) return
            val market = industry.market
            if (market == null) return

            val points = computeIndustryDisruptedPoints(industry)
            if (points > 0) {
                val factor = MPC_hegemonyMilHouseRaidIndFactor(
                    points,
                    industry.currentName + " " + market.onOrAt + " " + market.name +
                            " disrupted"
                )
                addFactor(factor, dialog)
                recentlyDisrupted.add(industry, industry.disruptedDays)
            }
        }
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {
        return
    }

    override fun reportBattleOccurred(
        fleet: CampaignFleetAPI?,
        primaryWinner: CampaignFleetAPI?,
        battle: BattleAPI?
    ) {
        //if (TriTachyonHostileActivityFactor.isPlayerCounterRaidedTriTach()) return

        if (!battle!!.isPlayerInvolved) return

        //if (getProgress(null) <= 0 && TriTachyonCommerceRaiding.get() == null) return

        var traderFP = 0f
        var militaryFP = 0f
        for (otherFleet in battle.nonPlayerSideSnapshot) {
            //if (!Global.getSector().getPlayerFaction().isHostileTo(otherFleet.getFaction())) continue;
            val trader = isTraderServingAHegColony(otherFleet)
            val military = isHegePatrolOrWarFLeet(otherFleet)

            if (!trader) {
                if (!MPC_hegemonyPatrolsDestroyedFactor.isActive() || !military) continue
            }

            var mult = 1f
            if (trader) {
                mult = TRADE_FLEET_FP_MULT
            } else if (military) {
                mult = MIL_FLEET_FP_MULT
            }

            for (loss in Misc.getSnapshotMembersLost(otherFleet)) {
                val fp = loss.fleetPointCost * mult
                if (trader) {
                    traderFP += fp
                } else if (military) {
                    militaryFP += fp
                }
            }
        }

        if (traderFP > 0) {
            val points = computeHegeCRProgressPoints(traderFP.toFloat())
            if (points > 0) {
                val factor = MPC_hegemonyTradeFleetsDestroyedFactor(points)
                addFactor(factor, null)
            }
        }
        if (militaryFP > 0) {
            val points = computeHegeCRProgressPoints(militaryFP.toFloat())
            if (points > 0) {
                val factor = MPC_hegemonyPatrolsDestroyedFactor(points)
                addFactor(factor, null)
            }
        }
    }

    protected fun sendKillFleets() {
        // diktat
        run {
            val r = Misc.getRandom(random.nextLong(), 7)
            val e = DelayedFleetEncounter(r, "MPC_IAIICHMilDiktatBountyHunter")
            if (Global.getSettings().isDevMode) {
                e.setDelayNone()
            } else {
                e.setDelayVeryShort()
            }            //e.setDelayNone();
            e.setDoNotAbortWhenPlayerFleetTooStrong() // small ships, few FP, but a strong fleet
            e.setLocationOuterSector(true, Factions.INDEPENDENT)
            e.beginCreate()
            e.triggerCreateFleet(
                FleetSize.MAXIMUM,
                HubMissionWithTriggers.FleetQuality.SMOD_3,
                Factions.LIONS_GUARD,
                FleetTypes.MERC_BOUNTY_HUNTER,
                Vector2f()
            )
            e.triggerSetFleetCombatFleetPoints(340f)
            //e.triggerSetFleetMaxShipSize(1)
            e.triggerSetFleetDoctrineOther(5, 4)

            e.triggerSetFleetDoctrineComp(4, 1, 0)

            e.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
            e.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1)
            e.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1)
            e.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1)
            e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.UNUSUALLY_HIGH)

            e.triggerFleetMakeFaster(true, 0, true)

            e.triggerSetFleetFaction(Factions.INDEPENDENT)
            e.triggerMakeNoRepImpact()
            e.triggerSetStandardAggroInterceptFlags()
            e.triggerMakeFleetIgnoreOtherFleets()
            e.triggerSetFleetGenericHailPermanent("MPC_IAIICHMilBountyHunterHail")
            e.triggerSetFleetFlagPermanent("\$MPC_IAIICHMilBountyHunterDiktat")
            e.endCreate()
        }

        // phase
        run {
            val r = Misc.getRandom(random.nextLong(), 3)
            val e = DelayedFleetEncounter(r, "MPC_IAIICHMilPhaseBountyHunter")
            if (Global.getSettings().isDevMode) {
                e.setDelayNone()
            } else {
                e.setDelayVeryShort()
            }            //e.setDelayNone();
            e.setLocationInnerSector(true, Factions.INDEPENDENT)
            e.beginCreate()
            e.triggerCreateFleet(
                FleetSize.MAXIMUM,
                HubMissionWithTriggers.FleetQuality.SMOD_3,
                Factions.MERCENARY,
                FleetTypes.MERC_BOUNTY_HUNTER,
                Vector2f()
            )
            e.triggerSetFleetCombatFleetPoints(340f)

            e.triggerSetFleetDoctrineComp(0, 0, 5)

            e.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
            e.triggerFleetAddCommanderSkill(Skills.PHASE_CORPS, 1)
            e.triggerFleetAddCommanderSkill(Skills.ELECTRONIC_WARFARE, 1)
            e.triggerFleetAddCommanderSkill(Skills.FLUX_REGULATION, 1)
            e.triggerFleetAddCommanderSkill(Skills.TACTICAL_DRILLS, 1)
            e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.HIGHER)

            e.triggerFleetMakeFaster(true, 0, true)

            e.triggerSetFleetFaction(Factions.INDEPENDENT)
            e.triggerMakeNoRepImpact()
            e.triggerSetStandardAggroInterceptFlags()
            e.triggerMakeFleetIgnoreOtherFleets()
            e.triggerSetFleetGenericHailPermanent("MPC_IAIICHMilBountyHunterHail")
            e.triggerSetFleetFlagPermanent("\$MPC_IAIICHMilBountyHunterPhase")
            e.endCreate()
        }


        // derelict
        run {
            val r = Misc.getRandom(random.nextLong(), 11)
            val e = DelayedFleetEncounter(r, "MPC_IAIICHMilFighterBountyHunter")
            if (Global.getSettings().isDevMode) {
                e.setDelayNone()
            } else {
                e.setDelayVeryShort()
            }
            //e.setDelayNone();
            //e.setLocationCoreOnly(true, market.getFactionId());
            e.setLocationCoreOnly(true, Factions.INDEPENDENT)
            e.beginCreate()
            e.triggerCreateFleet(
                FleetSize.MAXIMUM,
                HubMissionWithTriggers.FleetQuality.SMOD_2,
                Factions.PERSEAN,
                FleetTypes.MERC_BOUNTY_HUNTER,
                Vector2f()
            )
            e.triggerSetFleetCombatFleetPoints(340f)

            //e.triggerSetFleetDoctrineComp(4, 2, 1);
            e.triggerSetFleetDoctrineOther(4, 3)
            e.triggerSetFleetDoctrineComp(1, 5, 1)

            e.triggerFleetAddCommanderSkill(Skills.CARRIER_GROUP, 1)
            e.triggerFleetAddCommanderSkill(Skills.FIGHTER_UPLINK, 1)
            e.triggerFleetAddCommanderSkill(Skills.COORDINATED_MANEUVERS, 1)
            e.triggerSetFleetOfficers(OfficerNum.MORE, OfficerQuality.LOWER)

            e.triggerFleetMakeFaster(true, 2, true)

            e.triggerSetFleetFaction(Factions.INDEPENDENT)
            e.triggerMakeNoRepImpact()
            e.triggerSetStandardAggroInterceptFlags()
            e.triggerMakeFleetIgnoreOtherFleets()
            e.triggerSetFleetGenericHailPermanent("MPC_IAIICHMilBountyHunterHail")
            e.triggerSetFleetFlagPermanent("\$MPC_IAIICHMilBountyHunterFighter")
            e.endCreate()
        }
    }

}
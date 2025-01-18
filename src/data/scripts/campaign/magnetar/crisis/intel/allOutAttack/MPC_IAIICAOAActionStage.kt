package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript
import com.fs.starfarer.api.impl.campaign.command.WarSimScript
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.raid.ActionStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionOrders

class MPC_IAIICAOAActionStage(raid: RaidIntel?, val target: MarketAPI) : ActionStage(raid), BaseAssignmentAI.FleetActionDelegate {

    protected var untilAutoresolve = 10f
    protected var scripts: MutableList<MilitaryResponseScript>? = ArrayList()
    var didScripts = false
    var gotCore = false

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)
        untilAutoresolve -= days
        if (!didScripts) {
            didScripts = true
            removeMilScripts()

            // getMaxDays() is always 1 here
            // scripts get removed anyway so we don't care about when they expire naturally
            // just make sure they're around for long enough
            val duration = 100f
            val params = MilitaryResponseScript.MilitaryResponseParams(
                CampaignFleetAIAPI.ActionType.HOSTILE,
                "defMPC_IAIIC_AOA" + target.id,
                intel.faction,
                target.primaryEntity,
                1f,
                duration
            )
            val script = MilitaryResponseScript(params)
            target.containingLocation.addScript(script)
            scripts?.add(script)
            val defParams = MilitaryResponseScript.MilitaryResponseParams(
                CampaignFleetAIAPI.ActionType.HOSTILE,
                "defMPC_IAIIC_AOA" + target.id,
                target.faction,
                target.primaryEntity,
                1f,
                duration
            )
            val defScript = MilitaryResponseScript(defParams)
            target.containingLocation.addScript(defScript)
            scripts?.add(defScript)

            (intel as? MPC_IAIICAllOutAttack)?.makeHostileAndSendUpdate()
        }
    }

    override fun updateRoutes() {
        resetRoutes()
        //FactionAPI faction = intel.getFaction();
        val routes = RouteManager.getInstance().getRoutesForSource(intel.routeSourceId)
        for (route: RouteManager.RouteData in routes) {
            if (target.starSystem != null) { // so that fleet may spawn NOT at the target
                route.addSegment(RouteManager.RouteSegment(5f.coerceAtMost(untilAutoresolve), target.starSystem.center))
            }
            route.addSegment(RouteManager.RouteSegment(1000f, target.primaryEntity))
            if (route.activeFleet != null) {
                route.activeFleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, target.primaryEntity, 30f, "raiding", null)
            }
        }
    }

    protected fun removeMilScripts() {
        if (scripts != null) {
            for (s: MilitaryResponseScript in scripts!!) {
                s.forceDone()
            }
        }
    }

    protected fun autoresolve() {
        val str = WarSimScript.getFactionStrength(intel.faction, target.starSystem)
        val enemyStr = WarSimScript.getEnemyStrength(intel.faction, target.starSystem, true)
        val hostile = target.faction.isHostileTo(intel.faction)

        //MPC_IAIICInspectionOrders orders = ((MPC_IAIICInspectionIntel) intel).getOrders();

        //if (hostile || )
        val defensiveStr = enemyStr + WarSimScript.getStationStrength(
            target.faction,
            target.starSystem, target.primaryEntity
        )
        if (hostile && defensiveStr >= str) {
            status = RaidIntel.RaidStageStatus.FAILURE
            removeMilScripts()
            giveReturnOrdersToStragglers(routes)
            return
        }

        if (hostile) {
            val station = Misc.getStationIndustry(target)
            if (station != null) {
                OrbitalStation.disrupt(station)
            }
        }
        performRaid(null, target)

        //removeMilScripts();
    }

    override fun updateStatus() {
//		if (true) {
//			status = RaidStageStatus.SUCCESS;
//			return;
//		}
        abortIfNeededBasedOnFP(true)
        if (status != RaidIntel.RaidStageStatus.ONGOING) return
        val inSpawnRange = RouteManager.isPlayerInSpawnRange(target.primaryEntity)
        if (!inSpawnRange && untilAutoresolve <= 0) {
            autoresolve()
            return
        }
        if (!target.isInEconomy || !target.isPlayerOwned) {
            status = RaidIntel.RaidStageStatus.FAILURE
            removeMilScripts()
            giveReturnOrdersToStragglers(routes)
            return
        }
    }

    override fun getRaidActionText(fleet: CampaignFleetAPI?, market: MarketAPI): String {
        return "raiding ${market.name}"
    }

    override fun getRaidApproachText(fleet: CampaignFleetAPI?, market: MarketAPI): String {
        return "moving to raid " + market.name
    }

    override fun isPlayerTargeted(): Boolean = true

    override fun canRaid(fleet: CampaignFleetAPI?, market: MarketAPI): Boolean {
        return market == target
    }

    override fun performRaid(fleet: CampaignFleetAPI?, market: MarketAPI?) {
        var market = market
        if (market == null) {
            market = target
        }
        var str = intel.assembleStage.origSpawnFP * 3f
        if (fleet != null) str = MarketCMD.getRaidStr(fleet)
        val re = MarketCMD.getRaidEffectiveness(market, str)
        MarketCMD.applyRaidStabiltyPenalty(
            market,
            Misc.ucFirst(intel.faction.personNamePrefix) + " raid", re
        )
        Misc.setFlagWithReason(
            market.memoryWithoutUpdate, MemFlags.RECENTLY_RAIDED,
            intel.faction.id, true, 30f
        )
        Misc.setRaidedTimestamp(market)

        market.admin = null
        status = RaidIntel.RaidStageStatus.SUCCESS
        gotCore = true
        MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
        removeMilScripts()
    }

    override fun getRaidPrepText(fleet: CampaignFleetAPI?, from: SectorEntityToken?): String {
        return "preparing for raid"
    }

    override fun getRaidInSystemText(fleet: CampaignFleetAPI?): String {
        return "raiding"
    }

    override fun getRaidDefaultText(fleet: CampaignFleetAPI?): String {
        return "raiding"
    }

    override fun showStageInfo(info: TooltipMakerAPI) {
        val curr: Int = intel.currentStage
        val index: Int = intel.getStageIndex(this)
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f
        if (status == RaidIntel.RaidStageStatus.FAILURE) {
            info.addPara(
                "The raiding forces have been defeated by the defenders of the " +
                        intel.system.nameWithLowercaseType + ", sealing the coffin of the %s.", opad, intel.faction.color, "IAIIC"
            )
        } else if (status == RaidIntel.RaidStageStatus.SUCCESS) {
            info.addPara(
                "The attacking forces have successfully stolen the %s from %s.", opad,
                Misc.getHighlightColor(),
                "fractal core", target.name
            ).setHighlightColors(
                Misc.getHighlightColor(),
                target.faction.baseUIColor
            )
        } else if (curr == index) {
            info.addPara(
                ("The raiding forces are currently operating in the " +
                        intel.system.nameWithLowercaseType + "."), opad
            )
        }
    }
}
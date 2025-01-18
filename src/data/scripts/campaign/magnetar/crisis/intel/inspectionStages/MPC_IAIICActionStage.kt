package data.scripts.campaign.magnetar.crisis.intel.inspectionStages

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI.ActionType
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript
import com.fs.starfarer.api.impl.campaign.MilitaryResponseScript.MilitaryResponseParams
import com.fs.starfarer.api.impl.campaign.command.WarSimScript
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteData
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager.RouteSegment
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.raid.ActionStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidStageStatus
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseAssignmentAI.FleetActionDelegate
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionIntel
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICInspectionOrders
import data.utilities.niko_MPC_ids

class MPC_IAIICActionStage(raid: RaidIntel?, val target: MarketAPI) : ActionStage(raid), FleetActionDelegate {
    var REP_PENALTY_HID_STUFF = -0.2f
    var REP_PENALTY_DISRUPTED = -0.2f
    var REP_PENALTY_NORMAL = -0.1f

    protected var playerTargeted = false
    protected var scripts: MutableList<MilitaryResponseScript>? = ArrayList()
    protected var gaveOrders = true // will be set to false in updateRoutes()

    protected var untilAutoresolve = 0f

    companion object {
        const val HIDE_CHANCE = 80f
    }

    init {
        playerTargeted = target.isPlayerOwned // I mean, it's player-targeted by nature, but still
        untilAutoresolve = 5f
        val intel = intel as MPC_IAIICInspectionIntel
        if (intel.orders == MPC_IAIICInspectionOrders.RESIST) {
            //untilAutoresolve = 30f;
            untilAutoresolve = 15f + 5f * Math.random().toFloat()
        }
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)
        untilAutoresolve -= days
        if (!gaveOrders) {
            gaveOrders = true
            removeMilScripts()

            // getMaxDays() is always 1 here
            // scripts get removed anyway so we don't care about when they expire naturally
            // just make sure they're around for long enough
            val duration = 100f
            val params = MilitaryResponseParams(
                ActionType.HOSTILE,
                "MPC_IAIIC_" + target.id,
                intel.faction,
                target.primaryEntity,
                1f,
                duration
            )
            val script = MilitaryResponseScript(params)
            target.containingLocation.addScript(script)
            scripts?.add(script)
            val defParams = MilitaryResponseParams(
                ActionType.HOSTILE,
                "defMPC_IAIIC_" + target.id,
                target.faction,
                target.primaryEntity,
                1f,
                duration
            )
            val defScript = MilitaryResponseScript(defParams)
            target.containingLocation.addScript(defScript)
            scripts?.add(defScript)
        }
    }

    protected fun removeMilScripts() {
        if (scripts != null) {
            for (s: MilitaryResponseScript in scripts!!) {
                s.forceDone()
            }
        }
    }

    override fun updateStatus() {
//		if (true) {
//			status = RaidStageStatus.SUCCESS;
//			return;
//		}
        abortIfNeededBasedOnFP(true)
        if (status != RaidStageStatus.ONGOING) return
        val inSpawnRange = RouteManager.isPlayerInSpawnRange(target.primaryEntity)
        if (!inSpawnRange && untilAutoresolve <= 0) {
            autoresolve()
            return
        }
        if (!target.isInEconomy || !target.isPlayerOwned) {
            status = RaidStageStatus.FAILURE
            removeMilScripts()
            giveReturnOrdersToStragglers(routes)
            return
        }
    }

    override fun getRaidActionText(fleet: CampaignFleetAPI?, market: MarketAPI): String? {
        return "performing inspection of " + market.name
    }

    override fun getRaidApproachText(fleet: CampaignFleetAPI?, market: MarketAPI): String? {
        return "moving to inspect " + market.name
    }

    override fun performRaid(fleet: CampaignFleetAPI?, market: MarketAPI?) {
        var market = market
        removeMilScripts()
        if (market == null) {
            market = target
        }
        val intel = intel as MPC_IAIICInspectionIntel
        status = RaidStageStatus.SUCCESS
        val hostile = market.faction.isHostileTo(intel.faction)
        val orders = intel.orders
        if (hostile || orders == MPC_IAIICInspectionOrders.RESIST) {
            //RecentUnrest.get(target).add(3, Misc.ucFirst(intel.getFaction().getPersonNamePrefix()) + " inspection");
            //float str = MPC_IAIICInspectionIntel.DEFAULT_INSPECTION_GROUND_STRENGTH;
            var str = intel.assembleStage.origSpawnFP * 3f
            if (fleet != null) str = MarketCMD.getRaidStr(fleet)
            val re = MarketCMD.getRaidEffectiveness(market, str)
            MarketCMD.applyRaidStabiltyPenalty(
                market,
                Misc.ucFirst(intel.faction.personNamePrefix) + " inspection", re
            )
            Misc.setFlagWithReason(
                market.memoryWithoutUpdate, MemFlags.RECENTLY_RAIDED,
                intel.faction.id, true, 30f
            )
            Misc.setRaidedTimestamp(market)
            removeCoresAndApplyResult(fleet)
        } else if (orders == MPC_IAIICInspectionOrders.COMPLY) {
            removeCoresAndApplyResult(fleet)
        } else if (orders == MPC_IAIICInspectionOrders.BRIBE) {
            intel.outcome = MPC_IAIICInspectionOutcomes.BRIBED
        }

//		if (fleet != null) {
//			fleet.getMemoryWithoutUpdate().unset(MemFlags.MEMORY_KEY_RAIDER);
//		}

        //if (intel.getOutcome() != null) {
//		if (intel.getOutcome() != null && status == RaidStageStatus.SUCCESS) {
//			intel.sendOutcomeUpdate();
//		}
        if (intel.outcome != null) {
            if (status == RaidStageStatus.SUCCESS) {
                intel.sendOutcomeUpdate()
            } else {
                removeMilScripts()
                giveReturnOrdersToStragglers(routes)
            }
        }
        if (coresRemoved?.contains(niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) == true) {
            MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
        }
    }

    var coresRemoved: MutableList<String>? = ArrayList()
    protected fun removeCoresAndApplyResult(fleet: CampaignFleetAPI?) {
        val intel = intel as MPC_IAIICInspectionIntel
        val orders = intel.orders
        val resist = orders == MPC_IAIICInspectionOrders.RESIST
        //val hiding = orders == MPC_IAIICInspectionOrders.HIDE_CORES
        //val hidingEffective = !Global.getSector().memoryWithoutUpdate.getBoolean(niko_MPC_ids.ALREADY_HID_CORES)
        val found = removeCores(fleet, resist, false, false)
        if (coresRemoved == null) coresRemoved = ArrayList()
        coresRemoved!!.clear()
        coresRemoved!!.addAll(found)
        val expected = intel.expectedCores
        var valFound = 0
        var valExpected = 0
        for (id: String? in found) {
            val spec = Global.getSettings().getCommoditySpec(id)
            valFound += spec.basePrice.toInt()
            if (id == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {
                continue
            } // it just gets destroyed outright
            fleet?.cargo?.addCommodity(id, 1f)
        }
        for (id: String? in expected) {
            val spec = Global.getSettings().getCommoditySpec(id)
            valExpected += spec.basePrice.toInt()
        }
        if (valExpected < 30000) {
            valExpected = 30000
        }

        /*if (hiding && hidingEffective) {
            intel.outcome = MPC_IAIICInspectionOutcomes.INVESTIGATION_DISRUPTED
            Global.getSector().memoryWithoutUpdate[niko_MPC_ids.ALREADY_HID_CORES] = true
            for (curr: Industry in target.industries) {
                curr.setDisrupted(intel.random.nextFloat() * 35f + 15f)
            }
            intel.applyRepPenalty(REP_PENALTY_DISRUPTED)
        }*/
        //resist = false;
        else if (!resist && valExpected > valFound * 1.25f) {
            intel.outcome = MPC_IAIICInspectionOutcomes.FOUND_EVIDENCE_NO_CORES
            for (curr: Industry in target.industries) {
                curr.setDisrupted(intel.random.nextFloat() * 45f + 15f)
            }
            intel.applyRepPenalty(REP_PENALTY_HID_STUFF)
        } else {
            intel.outcome = MPC_IAIICInspectionOutcomes.CONFISCATE_CORES
            intel.applyRepPenalty(REP_PENALTY_NORMAL)
        }
    }

    protected fun removeCores(inspector: CampaignFleetAPI?, resist: Boolean, hiding: Boolean, hidingEffective: Boolean): List<String> {
        val intel = intel as MPC_IAIICInspectionIntel
        //float str = MPC_IAIICInspectionIntel.DEFAULT_INSPECTION_GROUND_STRENGTH;
        //float str = intel.getAssembleStage().getOrigSpawnFP() * Misc.FP_TO_GROUND_RAID_STR_APPROX_MULT;
        var str = intel.raidFPAdjusted / intel.numFleets * Misc.FP_TO_GROUND_RAID_STR_APPROX_MULT
        if (inspector != null) str = MarketCMD.getRaidStr(inspector)

        //str = 100000f;
        val re = MarketCMD.getRaidEffectiveness(target, str)
        val result: MutableList<String> = ArrayList()
        for (curr: Industry in target.industries) {
            val id = curr.aiCoreId
            if (id != null) {
                if (resist && intel.random.nextFloat() > re) continue
                if (hiding && hidingEffective && intel.random.nextFloat() <= HIDE_CHANCE) continue
                result.add(id)
                curr.aiCoreId = null
            }
        }
        val admin = target.admin
        if (admin.isAICore) {
            if (hiding && hidingEffective && admin.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) {

            } else if (!resist || intel.random.nextFloat() < re) {
                result.add(admin.aiCoreId)
                target.admin = null
            }
        }
        target.reapplyIndustries()
        val missing: MutableList<String> = ArrayList(intel.expectedCores)
        for (id: String in result) {
            missing.remove(id)
        }
        var cargo = Misc.getStorageCargo(target)
        if (cargo != null) {
            for (id: String in ArrayList<String>(missing)) {
                val qty = cargo.getCommodityQuantity(id)
                if (qty >= 1) {
                    if (resist && intel.random.nextFloat() > re) continue
                    cargo.removeCommodity(id, 1f)
                    missing.remove(id)
                    result.add(id)
                }
            }
        }
        cargo = Misc.getLocalResourcesCargo(target)
        if (cargo != null) {
            for (id: String in ArrayList<String>(missing)) {
                val qty = cargo.getCommodityQuantity(id)
                if (qty >= 1) {
                    if (resist && intel.random.nextFloat() > re) continue
                    cargo.removeCommodity(id, 1f)
                    missing.remove(id)
                    result.add(id)
                }
            }
        }
        return result
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
            status = RaidStageStatus.FAILURE
            removeMilScripts()
            giveReturnOrdersToStragglers(routes)
            return
        }

        //status = RaidStageStatus.FAILURE;
        if (hostile) {
            val station = Misc.getStationIndustry(target)
            if (station != null) {
                OrbitalStation.disrupt(station)
            }
        }
        performRaid(null, target)

        //removeMilScripts();
    }

    override fun updateRoutes() {
        resetRoutes()
        val hostile = target.faction.isHostileTo(intel.faction)
        val orders = (intel as MPC_IAIICInspectionIntel).orders
        if (!hostile && orders == MPC_IAIICInspectionOrders.RESIST) {
            (intel as MPC_IAIICInspectionIntel).makeHostileAndSendUpdate()
        } else {
            (intel as MPC_IAIICInspectionIntel).sendInSystemUpdate()
        }
        gaveOrders = false
        (intel as MPC_IAIICInspectionIntel).enteredSystem = true

        //FactionAPI faction = intel.getFaction();
        val routes = RouteManager.getInstance().getRoutesForSource(intel.routeSourceId)
        for (route: RouteData in routes) {
            if (target.starSystem != null) { // so that fleet may spawn NOT at the target
                route.addSegment(RouteSegment(Math.min(5f, untilAutoresolve), target.starSystem.center))
            }
            route.addSegment(RouteSegment(1000f, target.primaryEntity))
            if (route.activeFleet != null) {
                route.activeFleet.addAssignmentAtStart(FleetAssignment.GO_TO_LOCATION, target.primaryEntity, 30f, "raiding", null)
            }
        }
    }


    override fun showStageInfo(info: TooltipMakerAPI) {
        val curr = intel.currentStage
        val index = intel.getStageIndex(this)
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f
        val intel = intel as MPC_IAIICInspectionIntel
        val orders = intel.orders
        val resist = orders == MPC_IAIICInspectionOrders.RESIST
        if (status == RaidStageStatus.FAILURE) {
            if (intel.outcome == MPC_IAIICInspectionOutcomes.COLONY_NO_LONGER_EXISTS) {
                info.addPara("The inspection has been aborted.", opad)
            } else {
                info.addPara(
                    "The inspection task force has been defeated by the defenders of " +
                            target.name + ". The inspection is now over.", opad
                )
            }
        } else if (status == RaidStageStatus.SUCCESS) {
            val cores = Global.getFactory().createCargo(true)
            for (id: String? in coresRemoved!!) {
                cores.addCommodity(id, 1f)
            }
            cores.sort()
            when (intel.outcome) {
                MPC_IAIICInspectionOutcomes.INVESTIGATION_DISRUPTED -> {
                    if (!cores.isEmpty) {
                        info.addPara("The inspectors have confiscated the following AI cores:", opad)
                        info.showCargo(cores, 10, true, opad)
                    } else {
                        info.addPara("The inspectors have not found any AI cores.", opad)
                    }
                    info.addPara(
                        "There was clear evidence of hastily-done obscuration work, inspiring the inspectors to investigate with great zeal. Local operations have been heavily disrupted.", opad
                    )
                }

                MPC_IAIICInspectionOutcomes.CONFISCATE_CORES -> if (!cores.isEmpty) {
                    info.addPara("The inspectors have confiscated the following AI cores:", opad)
                    info.showCargo(cores, 10, true, opad)
                } else {
                    if (resist) {
                        info.addPara("The inspectors have not been able to confiscate any AI cores.", opad)
                    } else {
                        info.addPara("The inspectors have not found any AI cores.", opad)
                    }
                }

                MPC_IAIICInspectionOutcomes.FOUND_EVIDENCE_NO_CORES -> {
                    if (!cores.isEmpty) {
                        info.addPara("The inspectors have confiscated the following AI cores:", opad)
                        info.showCargo(cores, 10, true, opad)
                    } else {
                        info.addPara("The inspectors have not found any AI cores.", opad)
                    }
                    info.addPara(
                        "There was ample evidence of AI core use, spurring the inspectors to great zeal " +
                                "in trying to find them. Local operations have been significantly disrupted.", opad
                    )
                }
                MPC_IAIICInspectionOutcomes.COLONY_NO_LONGER_EXISTS -> {
                    info.addPara("The target colony no longer exists. The inspection is now over.", opad)
                }
                MPC_IAIICInspectionOutcomes.DEFEATED -> {
                    info.addPara(
                        "The inspection task force has been defeated by the defenders of " +
                                target.name + ". The inspection is now over.", opad
                    )
                }
                MPC_IAIICInspectionOutcomes.BRIBED -> {
                    info.addPara(
                        "The funds you've allocated have been used to resolve the inspection to the " +
                                "satisfaction of all parties.", opad
                    )
                }
                null -> return
            }
        } else if (curr == index) {
            info.addPara("The inspection of " + target.name + " is currently under way.", opad)
        }
    }

    override fun canRaid(fleet: CampaignFleetAPI?, market: MarketAPI): Boolean {
        val intel = (intel as MPC_IAIICInspectionIntel)
        return if (intel.outcome != null) false else market === target
    }

    override fun getRaidPrepText(fleet: CampaignFleetAPI?, from: SectorEntityToken): String? {
        return "orbiting " + from.name
    }

    override fun getRaidInSystemText(fleet: CampaignFleetAPI?): String? {
        return "traveling"
    }

    override fun getRaidDefaultText(fleet: CampaignFleetAPI?): String? {
        return "traveling"
    }

    override fun isPlayerTargeted(): Boolean {
        return playerTargeted
    }
}

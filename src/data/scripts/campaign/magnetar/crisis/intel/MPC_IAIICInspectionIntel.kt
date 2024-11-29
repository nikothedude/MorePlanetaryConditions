package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.*
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIAssembleStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIOrganizeStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIReturnStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HITravelStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HegemonyInspectionIntel.HegemonyInspectionOutcome
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.inspectionStages.MPC_IAIICActionStage
import data.scripts.campaign.magnetar.crisis.intel.inspectionStages.MPC_IAIICInspectionOutcomes
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import java.util.*

class MPC_IAIICInspectionIntel(val from: MarketAPI, val target: MarketAPI, val inspectionFP: Float): RaidIntel(target.starSystem, from.faction, null), RaidDelegate {

    val random: Random = MathUtils.getRandom()
    var expectedCores: MutableList<String> = ArrayList()
    var targettingFractalCore: Boolean = false
    var orders: MPC_IAIICInspectionOrders = MPC_IAIICInspectionOrders.RESIST
    var outcome: MPC_IAIICInspectionOutcomes? = null
    protected var action: MPC_IAIICActionStage? = null


    companion object {
        val MADE_HOSTILE_UPDATE = Any()
        val ENTERED_SYSTEM_UPDATE = Any()
        val OUTCOME_UPDATE = Any()
    }

    override fun notifyRaidEnded(raid: RaidIntel?, status: RaidStageStatus?) {
        if (outcome == null && failStage >= 0) {
            if (!target.isInEconomy || !target.isPlayerOwned) {
                outcome = MPC_IAIICInspectionOutcomes.COLONY_NO_LONGER_EXISTS
            } else {
                outcome = MPC_IAIICInspectionOutcomes.DEFEATED
            }
            //sendOutcomeUpdate(); // don't do this - base raid sends an UPDATE_FAILED so we're good already
        }
        if (action?.getCoresRemoved()?.contains(niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) == true) {
            MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
        }
        /*if (listener != null && outcome != null) {
            listener.notifyInspectionEnded(outcome)
        }*/
    }

    init {
        setup()
    }
    fun setup() {
        for (curr in target.industries) {
            val id = curr?.aiCoreId
            if (id != null) {
                expectedCores.add(id)
            }
        }
        val admin: PersonAPI = target.admin
        if (admin.isAICore) {
            expectedCores += (admin.aiCoreId)
            if (admin.aiCoreId == niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) targettingFractalCore = true
        }

        val orgDur = 20f + 10f * Math.random().toFloat()
        addStage(HIOrganizeStage(this, from, orgDur))

        val gather = from.primaryEntity
        val raidJump: SectorEntityToken? = RouteLocationCalculator.findJumpPointToUse(factionForUIColors, target.primaryEntity)

        if (gather == null || raidJump == null) {
            endImmediately()
            return
        }

        val successMult = 0.5f

        val assemble = HIAssembleStage(this, gather)
        assemble.addSource(from)
        assemble.spawnFP = inspectionFP
        assemble.abortFP = inspectionFP * successMult
        addStage(assemble)


        val travel = HITravelStage(this, gather, raidJump, false)
        travel.abortFP = inspectionFP * successMult
        addStage(travel)

        action = MPC_IAIICActionStage(this, target)
        action!!.abortFP = inspectionFP * successMult
        addStage(action)

        addStage(HIReturnStage(this))

        isImportant = true

        Global.getSector().intelManager.addIntel(this)
    }

    protected var repResult: ReputationAdjustmentResult? = null
    fun makeHostileAndSendUpdate() {
        val hostile = getFaction().isHostileTo(Factions.PLAYER)
        if (!hostile) {
            repResult = Global.getSector().adjustPlayerReputation(
                RepActionEnvelope(
                    RepActions.MAKE_HOSTILE_AT_BEST,
                    null, null, null, false, false
                ),
                niko_MPC_ids.IAIIC_FAC_ID
            )
            sendUpdateIfPlayerHasIntel(MADE_HOSTILE_UPDATE, false)
        }
    }

    fun sendInSystemUpdate() {
        sendUpdateIfPlayerHasIntel(ENTERED_SYSTEM_UPDATE, false)
    }

    fun sendOutcomeUpdate() {
        sendUpdateIfPlayerHasIntel(OUTCOME_UPDATE, false)
    }

    fun applyRepPenalty(delta: Float) {
        val impact = CustomRepImpact()
        impact.delta = delta
        repResult = Global.getSector().adjustPlayerReputation(
            RepActionEnvelope(
                RepActions.CUSTOM,
                impact, null, null, false, false
            ),
            getFaction().id
        )
    }

    override fun getName(): String {
        val base = "IAIIC AI Inspection"
        if (outcome == MPC_IAIICInspectionOutcomes.DEFEATED ||
            outcome == MPC_IAIICInspectionOutcomes.COLONY_NO_LONGER_EXISTS
        ) return "$base - Failed"
        return if (outcome != null) "$base - Completed" else base
    }

    override fun addBulletPoints(info: TooltipMakerAPI, mode: ListInfoMode) {
        //super.addBulletPoints(info, mode);
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val pad = 3f
        val opad = 10f
        var initPad = pad
        if (mode == ListInfoMode.IN_DESC) initPad = opad
        val tc = getBulletColorForMode(mode)
        bullet(info)
        val isUpdate = getListInfoParam() != null
        val hostile = getFaction().isHostileTo(Factions.PLAYER)
        if (hostile) {
            orders = MPC_IAIICInspectionOrders.RESIST
        }
        if (getListInfoParam() == MADE_HOSTILE_UPDATE) {
            val other = target.faction
            info.addPara(
                "Target: %s", initPad, tc,
                other.baseUIColor, target.name
            )
            initPad = 0f
            info.addPara(
                "" + faction.displayName + " forces launch and encounter resistance", initPad, tc,
                faction.baseUIColor, faction.displayName
            )
            initPad = 0f
            CoreReputationPlugin.addAdjustmentMessage(
                repResult!!.delta, faction, null,
                null, null, info, tc, isUpdate, initPad
            )
            return
        }
        if (getListInfoParam() == ENTERED_SYSTEM_UPDATE) {
            val other = target.faction
            info.addPara(
                "Target: %s", initPad, tc,
                other.baseUIColor, target.name
            )
            initPad = 0f
            info.addPara("Launched", tc, initPad)
            //			info.addPara("" + faction.getDisplayName() + " inspection arrives in-system", initPad, tc,
//					faction.getBaseUIColor(), faction.getDisplayName());
            return
        }
        if (getListInfoParam() == OUTCOME_UPDATE) {
            if (action !is MPC_IAIICActionStage) return
            val num: Int = (action as MPC_IAIICActionStage).getCoresRemoved()?.size ?: return
            if (num > 0) {
                var cores = "cores"
                if (num == 1) cores = "core"
                info.addPara("%s AI $cores confiscated", initPad, tc, h, "" + num)
                initPad = 0f
            }
            if (outcome == MPC_IAIICInspectionOutcomes.FOUND_EVIDENCE_NO_CORES) {
                val other = target.faction
                info.addPara(
                    "Operations at %s disrupted", initPad, tc,
                    other.baseUIColor, target.name
                )
                //info.addPara("Operations disrupted by inspection", initPad, h, "" + num);
            } else if (outcome == MPC_IAIICInspectionOutcomes.CONFISCATE_CORES) {
            }
            initPad = 0f
            if (repResult != null) {
                CoreReputationPlugin.addAdjustmentMessage(
                    repResult!!.delta, faction, null,
                    null, null, info, tc, isUpdate, initPad
                )
            }
            return
        }

//		if (getListInfoParam() == UPDATE_FAILED) {
//			FactionAPI other = target.getFaction();
//			info.addPara("Target: %s", initPad, tc,
//					     other.getBaseUIColor(), target.getName());
//			initPad = 0f;
//			info.addPara("Inspection failed", tc, initPad);
//			return;
//		}
        val eta = eta
        val other = target.faction
        info.addPara(
            "Target: %s", initPad, tc,
            other.baseUIColor, target.name
        )
        initPad = 0f
        if (eta > 1 && outcome == null) {
            val days = getDaysString(eta)
            info.addPara(
                "Estimated %s $days until arrival",
                initPad, tc, h, "" + Math.round(eta)
            )
            initPad = 0f
            if (hostile || orders == MPC_IAIICInspectionOrders.RESIST) {
                info.addPara("Defenders will resist", tc, initPad)
            } else if (orders == MPC_IAIICInspectionOrders.COMPLY) {
                info.addPara("Defenders will comply", tc, initPad)
            } else if (orders == MPC_IAIICInspectionOrders.HIDE_CORES) {
                info.addPara("Cores will be hidden", tc, initPad)
            }
        } else if (outcome == null && action!!.elapsed > 0) {
            info.addPara("Inspection under way", tc, initPad)
            initPad = 0f
        } else if (outcome != null) {
            val num: Int = (action as MPC_IAIICActionStage).getCoresRemoved()?.size ?: return
            if (num > 0) {
                var cores = "cores"
                if (num == 1) cores = "core"
                info.addPara("%s AI $cores confiscated", initPad, tc, h, "" + num)
                initPad = 0f
            } else if (outcome == MPC_IAIICInspectionOutcomes.DEFEATED) {
                //info.addPara("Inspection failed", tc, initPad);
            }
            //			info.addPara("Inspection under way", tc, initPad);
//			initPad = 0f;
        }
        unindent(info)
    }

}
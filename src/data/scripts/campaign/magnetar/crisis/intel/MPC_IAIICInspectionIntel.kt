package data.scripts.campaign.magnetar.crisis.intel

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.*
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator
import com.fs.starfarer.api.impl.campaign.fleets.RouteManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIAssembleStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIOrganizeStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HIReturnStage
import com.fs.starfarer.api.impl.campaign.intel.inspection.HITravelStage
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.IntelUIAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.intel.inspectionStages.MPC_IAIICActionStage
import data.scripts.campaign.magnetar.crisis.intel.inspectionStages.MPC_IAIICInspectionOutcomes
import data.utilities.niko_MPC_ids
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Vector2f
import java.util.*

class MPC_IAIICInspectionIntel(val from: MarketAPI, val target: MarketAPI, val inspectionFP: Float): RaidIntel(target.starSystem, from.faction, null), RaidDelegate {

    val random: Random = MathUtils.getRandom()
    var expectedCores: MutableList<String> = ArrayList()
    var targettingFractalCore: Boolean = false
    var orders: MPC_IAIICInspectionOrders = MPC_IAIICInspectionOrders.RESIST
    var outcome: MPC_IAIICInspectionOutcomes? = null
    protected var action: MPC_IAIICActionStage? = null
    var enteredSystem: Boolean = false
    var investedCredits: Int = 0

    companion object {
        val MADE_HOSTILE_UPDATE = Any()
        val ENTERED_SYSTEM_UPDATE = Any()
        val OUTCOME_UPDATE = Any()
        val BUTTON_CHANGE_ORDERS: Any = Any()
        const val BASE_BRIBE_VALUE = 1000000
        const val BRIBE_REPEAT_MULT_EXP_BASE = 4f // it REALLY starts to take off

        fun getAICores(target: MarketAPI): MutableList<String> {
            val cores = ArrayList<String>()

            for (curr in target.industries) {
                val id = curr?.aiCoreId
                if (id != null) {
                    cores.add(id)
                }
            }
            val admin: PersonAPI = target.admin
            if (admin.isAICore) {
                cores += (admin.aiCoreId)
            }
            return cores
        }
    }

    override fun notifyRaidEnded(raid: RaidIntel?, status: RaidStageStatus?) {
        MPC_IAIICInspectionPrepIntel.get()?.inspectionEnded(this)
        if (outcome == null && failStage >= 0) {
            if (!target.isInEconomy || !target.isPlayerOwned) {
                outcome = MPC_IAIICInspectionOutcomes.COLONY_NO_LONGER_EXISTS
            } else {
                outcome = MPC_IAIICInspectionOutcomes.DEFEATED
            }
            //sendOutcomeUpdate(); // don't do this - base raid sends an UPDATE_FAILED so we're good already
        }
        if (action?.coresRemoved?.contains(niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID) == true) {
            MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
        }
        if (repResult != null && repResult!!.delta != 0f) {
            Global.getSector().adjustPlayerReputation(
                RepActionEnvelope(
                    RepActions.CUSTOM,
                    -repResult!!.delta, null, null, false, true, "Hostilities Ended"
                ),
                getFaction().id
            )
        }
        /*if (listener != null && outcome != null) {
            listener.notifyInspectionEnded(outcome)
        }*/
    }

    init {
        setup()
    }
    fun setup() {
        expectedCores.addAll(getAICores(target))
        if (expectedCores.contains(niko_MPC_ids.SLAVED_OMEGA_CORE_COMMID)) {
            targettingFractalCore = true
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
        MPC_IAIICFobIntel.get()?.currentAction = this
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        //super.createSmallDescription(info, width, height);
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        val tc = Misc.getTextColor()
        val pad = 3f
        val opad = 10f
        info.addImage(factionForUIColors.logo, width, 128f, opad)
        val faction = getFaction()
        val has = faction.displayNameHasOrHave
        val `is` = faction.displayNameIsOrAre

        //AssembleStage as = getAssembleStage();
        //MarketAPI source = as.getSources().get(0);
        val strDesc = raidStrDesc
        val numFleets = origNumFleets.toInt()
        var fleets = "fleets"
        if (numFleets == 1) fleets = "fleet"
        var label = info.addPara(
            Misc.ucFirst(faction.displayNameWithArticle) + " " + `is` +
                    " targeting %s for an inspection due to the suspected use of AI cores there." +
                    " The task force is projected to be " + strDesc + " and is likely comprised of " +
                    "" + numFleets + " " + fleets + ".",
            opad, faction.baseUIColor, target.name
        )
        label.setHighlight(faction.displayNameWithArticleWithoutArticle, target.name, strDesc, "" + numFleets)
        label.setHighlightColors(faction.baseUIColor, target.faction.baseUIColor, h, h)
        if (outcome == null) {
            addStandardStrengthComparisons(info, target, target.faction, true, false, "inspection", "inspection's")
        }
        info.addSectionHeading(
            "Status",
            faction.baseUIColor, faction.darkUIColor, Alignment.MID, opad
        )
        for (stage: RaidStage in stages) {
            stage.showStageInfo(info)
            if (getStageIndex(stage) == failStage) break
        }
        if (outcome == null) {
            val pf = Global.getSector().playerFaction
            info.addSectionHeading(
                "Your orders",
                pf.baseUIColor, pf.darkUIColor, Alignment.MID, opad
            )
            val hostile = getFaction().isHostileTo(Factions.PLAYER)
            if (hostile) {
                label = info.addPara(
                    (Misc.ucFirst(faction.displayNameWithArticle) + " " + `is` +
                            " hostile towards " + pf.displayNameWithArticle + ". Your forces will attempt to resist the inspection."),
                    opad
                )
                label.setHighlight(
                    faction.displayNameWithArticleWithoutArticle,
                    pf.displayNameWithArticleWithoutArticle
                )
                label.setHighlightColors(faction.baseUIColor, pf.baseUIColor)
                if (orders == MPC_IAIICInspectionOrders.BRIBE) {
                    info.addPara(
                        "Luckily, your allocated funds should still ensure a satisfactory outcome all-round.",
                        opad
                    )
                }
            } else {
                when (orders) {
                    MPC_IAIICInspectionOrders.COMPLY -> info.addPara(
                        ("The authorities at " + target.name + " will comply with the inspection. " +
                                "It is certain to find any AI cores currently in use."), opad
                    )

                    MPC_IAIICInspectionOrders.BRIBE -> info.addPara(
                        "You've allocated enough funds to ensure the inspection " +
                                "will produce a satisfactory outcome all around.", opad
                    )

                    MPC_IAIICInspectionOrders.RESIST -> info.addPara(
                        "Your space and ground forces will attempt to resist the inspection.",
                        opad
                    )
                    /*MPC_IAIICInspectionOrders.HIDE_CORES -> info.addPara(
                        "The authorities at ${target.name} will hide the AI cores. It is likely this will only work once.",
                        opad
                    )*/
                }
            }
            if (!enteredSystem) {
                val button = info.addButton(
                    "Change orders", BUTTON_CHANGE_ORDERS,
                    pf.baseUIColor, pf.darkUIColor,
                    (width).toInt().toFloat(), 20f, opad * 2f
                )
                button.setShortcut(Keyboard.KEY_T, true)
            } else {
                info.addPara(
                    "The inspection task force is active and there's no time to implement new orders.",
                    opad
                )
            }
        } else {
            //addBulletPoints(info, ListInfoMode.IN_DESC);
            bullet(info)
            if (repResult != null) {
                addAdjustmentMessage(
                    repResult!!.delta, faction, null,
                    null, null, info, tc, false, opad
                )
            }
            unindent(info)
        }
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
        MPC_IAIICInspectionPrepIntel.get()?.inspectionEnded(this)
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
        if (hostile && orders != MPC_IAIICInspectionOrders.BRIBE) { // bribes ALWAYS work
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
            addAdjustmentMessage(
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
            val num: Int = (action as MPC_IAIICActionStage).coresRemoved?.size ?: return
            if (num > 0) {
                var cores = "cores"
                if (num == 1) cores = "core"
                info.addPara("%s AI $cores confiscated", initPad, tc, h, "" + num)
                initPad = 0f
            }
            if (outcome == MPC_IAIICInspectionOutcomes.FOUND_EVIDENCE_NO_CORES || outcome == MPC_IAIICInspectionOutcomes.INVESTIGATION_DISRUPTED) {
                val other = target.faction
                info.addPara(
                    "Operations at %s disrupted", initPad, tc,
                    other.baseUIColor, target.name
                )
                //info.addPara("Operations disrupted by inspection", initPad, h, "" + num);
            } else if (outcome == MPC_IAIICInspectionOutcomes.CONFISCATE_CORES) {
            } else if (outcome == MPC_IAIICInspectionOutcomes.DEFEATED) {
                info.addPara(
                    "Inspection defeated", initPad
                )
            }
            initPad = 0f
            if (repResult != null) {
                addAdjustmentMessage(
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
                if (orders == MPC_IAIICInspectionOrders.BRIBE) {
                    info.addPara("Inspectors bribed", tc, 0f)
                }
            } else if (orders == MPC_IAIICInspectionOrders.COMPLY) {
                info.addPara("Defenders will comply", tc, initPad)
                /*} else if (orders == MPC_IAIICInspectionOrders.HIDE_CORES) {
                info.addPara("Cores will be hidden", tc, initPad)
            }*/
            } else if (outcome == null && action!!.elapsed > 0) {
                info.addPara("Inspection under way", tc, initPad)
                initPad = 0f
            } else if (outcome != null) {
                val num: Int = (action as MPC_IAIICActionStage).coresRemoved?.size ?: return
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

    override fun sendUpdateIfPlayerHasIntel(listInfoParam: Any, onlyIfImportant: Boolean, sendIfHidden: Boolean) {
        if (listInfoParam === UPDATE_RETURNING) {
            // we're using sendOutcomeUpdate() to send an end-of-event update instead
            return
        }
        super.sendUpdateIfPlayerHasIntel(listInfoParam, onlyIfImportant, sendIfHidden)
    }

    override fun buttonPressConfirmed(buttonId: Any?, ui: IntelUIAPI?) {
        if (buttonId == BUTTON_CHANGE_ORDERS) {
            ui!!.showDialog(null, MPC_IAIICInspectionDialogPluginImpl(this, ui))
        }
    }

    override fun createFleet(
        factionId: String?,
        route: RouteManager.RouteData?,
        market: MarketAPI?,
        locInHyper: Vector2f?,
        random: Random?
    ): CampaignFleetAPI? {
        val fleet = super.createFleet(factionId, route, market, locInHyper, random) ?: return null
        fleet.memoryWithoutUpdate[niko_MPC_ids.IAIIC_INSPECTION_FLEET] = true
        return fleet
    }

}
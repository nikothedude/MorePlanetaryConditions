package data.scripts.campaign.magnetar.crisis.intel.allOutAttack

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.command.WarSimScript
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import data.scripts.campaign.magnetar.crisis.MPC_hegemonyFractalCoreCause
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobEndReason
import data.scripts.campaign.magnetar.crisis.intel.MPC_IAIICFobIntel
import data.scripts.campaign.magnetar.crisis.intel.bombard.MPC_IAIICBombardAction
import data.utilities.niko_MPC_ids
import java.awt.Color

class MPC_IAIICAllOutAttackFGI(params: GenericRaidParams?): GenericRaidFGI(params) {

    companion object {
        const val MPC_IAIIC_ALL_OUT_ATTACK_FLEET = "\$MPC_IAIIC_allOutAttackFleet"
    }

    protected var interval = IntervalUtil(0.1f, 0.3f)

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            if (isCurrent(PAYLOAD_ACTION)) {
                val reason = "MPC_IAIICAllOutAttack"
                for (curr in getFleets()) {
                    Misc.setFlagWithReason(
                        curr.memoryWithoutUpdate, MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                        reason, true, 1f
                    )
                }
                Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).setRelationship(Factions.PLAYER, -1f)
            }
        }
    }

    override fun preConfigureFleet(size: Int, m: FleetCreatorMission) {
        m.fleetTypeMedium = FleetTypes.TASK_FORCE // default would be "Patrol", don't want that
        m.fleetTypeLarge = FleetTypes.TASK_FORCE
    }

    override fun configureFleet(size: Int, m: FleetCreatorMission) {
        m.triggerSetFleetFlag(MPC_IAIIC_ALL_OUT_ATTACK_FLEET)
        /*if (size >= 8) {
            m.triggerSetFleetDoctrineOther(5, 0) // more capitals in large fleets
        }*/
        m.triggerGetFleetParams().averageSMods = 1
        m.triggerSetFleetOfficers(HubMissionWithTriggers.OfficerNum.MORE, HubMissionWithTriggers.OfficerQuality.UNUSUALLY_HIGH)

        m.triggerGetFleetParams().tankerPts += 100f
    }

    override fun abort() {
        if (!isAborted) {
            for (curr in getFleets()) {
                curr.memoryWithoutUpdate.unset(MPC_IAIIC_ALL_OUT_ATTACK_FLEET)
            }
        }
        super.abort()
    }

    override fun addBasicDescription(info: TooltipMakerAPI?, width: Float, height: Float, opad: Float) {
        info!!.addImage(getFaction().logo, width, 128f, opad)

        //String aOrAn = Misc.getAOrAnFor(noun);
        //info.addPara(Misc.ucFirst(aOrAn) + " %s " + noun + " against "

        //String aOrAn = Misc.getAOrAnFor(noun);
        //info.addPara(Misc.ucFirst(aOrAn) + " %s " + noun + " against "

        val colony = MPC_hegemonyFractalCoreCause.getFractalColony() ?: return

        info.addPara(
            "Pushed to their limit, the %s is pouring all of their remaining fleetpower into one final mission: A %s against %s.",
            opad,
            Misc.getHighlightColor(),
            "IAIIC", "raid", "${colony.name}"
        ).setHighlightColors(
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            Misc.getNegativeHighlightColor(),
            colony.faction.baseUIColor
        )

        info.addPara(
            "Should this attack be defeated, the %s will have suffered a %s, and in all likelihood, will %s.",
            opad,
            Misc.getHighlightColor(),
            "IAIIC", "crushing defeat", "cease to exist"
        ).setHighlightColors(
            Global.getSector().getFaction(niko_MPC_ids.IAIIC_FAC_ID).baseUIColor,
            Misc.getHighlightColor(),
            Misc.getPositiveHighlightColor()
        )
    }

    override fun addAssessmentSection(info: TooltipMakerAPI?, width: Float, height: Float, opad: Float) {
        val h = Misc.getHighlightColor()

        val faction = getFaction()

        val targets = params.raidParams.allowedTargets

        val noun = noun
        if (!isEnding && !isSucceeded && !isFailed) {
            info!!.addSectionHeading(
                "Assessment",
                faction.baseUIColor, faction.darkUIColor, Alignment.MID, opad
            )
            if (targets.isEmpty()) {
                info!!.addPara("There are no colonies for the $noun to target in the system.", opad)
            } else {
                val system = raidAction.where
                val forces = forcesNoun
                val potentialDanger = addStrengthDesc(
                    info, opad, system, forces,
                    "the $noun is unlikely to find success",
                    "the outcome of the $noun is uncertain",
                    "the $noun is likely to find success"
                )
                if (potentialDanger) {
                    val safe = "should be safe from the $noun"
                    var risk: String? = "are at risk of being raided and losing both stability and their fractal core:"
                    var highlight: String? = "losing both stability and their fractal core:"
                    /*if (params.raidParams.bombardment == MarketCMD.BombardType.SATURATION) {
                        risk = "are at risk of suffering a saturation bombardment resulting in catastrophic damage:"
                        highlight = "catastrophic damage"
                    } else if (params.raidParams.bombardment == MarketCMD.BombardType.TACTICAL) {
                        risk =
                            "are at risk of suffering a targeted bombardment and having their military and ship-making infrastructure disrupted:"
                        highlight = "military and ship-making infrastructure disrupted"
                    } else if (params.raidParams.disrupt.isNotEmpty()) {
                        risk = "are at risk of being raided and having their operations severely disrupted"
                        highlight = "operations severely disrupted"
                    }*/
                    if (assessmentRiskStringOverride != null) {
                        risk = assessmentRiskStringOverride
                    }
                    if (assessmentRiskStringHighlightOverride != null) {
                        highlight = assessmentRiskStringHighlightOverride
                    }
                    showMarketsInDanger(
                        info, opad, width, system, targets,
                        safe, risk, highlight
                    )
                }
            }
            addPostAssessmentSection(info, width, height, opad)
        }
    }

    /**
     * Returns true if the defenses in the target system are weaker.
     * @return
     */
    override fun addStrengthDesc(
        info: TooltipMakerAPI, opad: Float, system: StarSystemAPI?,
        forces: String, outcomeFailure: String?, outcomeUncertain: String?, outcomeSuccess: String?
    ): Boolean {
        val h = Misc.getHighlightColor()
        val raidStr = getRoute().extra.strengthModifiedByDamage
        var defenderStr = 0f
        if (system != null) defenderStr = WarSimScript.getEnemyStrength(getFaction(), system, isPlayerTargeted)
        val strDesc = Misc.getStrengthDesc(raidStr)
        val numFleets = getApproximateNumberOfFleets()
        var fleets = "fleets"
        if (numFleets == 1) fleets = "fleet"
        var defenderDesc = ""
        var defenderHighlight: String = ""
        var defenderHighlightColor: Color? = h
        var potentialDanger = false
        var outcome: String? = null
        if (raidStr < defenderStr * 0.75f) {
            defenderDesc = "The defending fleets are superior"
            defenderHighlightColor = Misc.getPositiveHighlightColor()
            defenderHighlight = "superior"
            outcome = outcomeFailure
        } else if (raidStr < defenderStr * 1.25f) {
            defenderDesc = "The defending fleets are evenly matched"
            defenderHighlightColor = h
            defenderHighlight = "evenly matched"
            outcome = outcomeUncertain
            potentialDanger = true
        } else {
            defenderDesc = "The defending fleets are outmatched"
            defenderHighlightColor = Misc.getNegativeHighlightColor()
            defenderHighlight = "outmatched"
            outcome = outcomeSuccess
            potentialDanger = true
        }
        if (outcome != null) {
            defenderDesc += ", and $outcome."
        } else {
            defenderDesc += "."
        }
        if (system == null) defenderDesc = ""
        val label = info.addPara(
            "The " + forces + " are " +
                    "projected to be %s and likely comprised of %s " + fleets + ", each of which is estimated to have %s and %s.",
            opad, h, strDesc, "" + numFleets, "s-mods", "exceptional officers"
        )
        //label.setHighlight(strDesc, "" + numFleets)
        //label.setHighlightColors(h, h)
        info.addPara(
            defenderDesc,
            0f,
            defenderHighlightColor,
            defenderHighlight
        )
        return potentialDanger
    }


    /**
     * Returns true if the defenses in the target system are weaker.
     * @return
     */
    override fun addStrengthDesc(
        info: TooltipMakerAPI, opad: Float, target: MarketAPI,
        forces: String, outcomeFailure: String?, outcomeUncertain: String?, outcomeSuccess: String?
    ): Boolean {
        val h = Misc.getHighlightColor()
        val raidStr = getRoute().extra.strengthModifiedByDamage
        var defenderStr = 0f
        val system = target.starSystem
        if (system != null) defenderStr = WarSimScript.getEnemyStrength(getFaction(), system, isPlayerTargeted)
        defenderStr += WarSimScript.getStationStrength(target.faction, system, target.primaryEntity)
        val strDesc = Misc.getStrengthDesc(raidStr)
        val numFleets = getApproximateNumberOfFleets()
        var fleets = "fleets"
        if (numFleets == 1) fleets = "fleet"
        var defenderDesc = ""
        var defenderHighlight: String = ""
        var defenderHighlightColor: Color? = h
        var potentialDanger = false
        var outcome: String? = null
        if (raidStr < defenderStr * 0.75f) {
            defenderDesc = "The defending forces are superior"
            defenderHighlightColor = Misc.getPositiveHighlightColor()
            defenderHighlight = "superior"
            outcome = outcomeFailure
        } else if (raidStr < defenderStr * 1.25f) {
            defenderDesc = "The defending forces are evenly matched"
            defenderHighlightColor = h
            defenderHighlight = "evenly matched"
            outcome = outcomeUncertain
            potentialDanger = true
        } else {
            defenderDesc = "The defending forces are outmatched"
            defenderHighlightColor = Misc.getNegativeHighlightColor()
            defenderHighlight = "outmatched"
            outcome = outcomeSuccess
            potentialDanger = true
        }
        if (outcome != null) {
            defenderDesc += ", and $outcome."
        } else {
            defenderDesc += "."
        }
        if (system == null) defenderDesc = ""
        val label = info.addPara(
            "The " + forces + " are " +
                    "projected to be %s and likely comprised of %s " + fleets + ", each of which is estimated to have %s and %s.",
            opad, h, strDesc, "" + numFleets, "s-mods", "exceptional officers"
        )
        //label.setHighlight(strDesc, "" + numFleets)
        //label.setHighlightColors(h, h)
        info.addPara(
            defenderDesc,
            0f,
            defenderHighlightColor,
            defenderHighlight
        )
        return potentialDanger
    }

    override fun createPayloadAction(): GenericPayloadAction {
        return MPC_IAIICAllOutAttackAction(params.raidParams, params.payloadDays)
    }

    override fun hasCustomRaidAction(): Boolean {
        return true
    }

    override fun doCustomRaidAction(fleet: CampaignFleetAPI?, market: MarketAPI?, raidStr: Float) {
        MarketCMD(market!!.primaryEntity).doGenericRaid(faction, raidStr, 3f, false)
        market.admin = null
        if (!isSucceeded) {
            finish(false)
        }
        MPC_IAIICFobIntel.get()?.end(MPC_IAIICFobEndReason.FRACTAL_CORE_OBTAINED)
    }
}
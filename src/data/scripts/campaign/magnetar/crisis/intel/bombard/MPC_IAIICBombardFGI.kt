package data.scripts.campaign.magnetar.crisis.intel.bombard

import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.BombardType
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc

class MPC_IAIICBombardFGI(params: GenericRaidParams?) : GenericRaidFGI(params) {

    companion object {
        const val MPC_IAIIC_BOMBARD_FLEET = "\$MPC_IAIIC_BOMBARD_FLEET"
    }

    protected var interval = IntervalUtil(0.1f, 0.3f)

    override fun advance(amount: Float) {
        super.advance(amount)
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (interval.intervalElapsed()) {
            if (isCurrent(PAYLOAD_ACTION)) {
                val reason = "MPC_IAIICBombard"
                for (curr in getFleets()) {
                    Misc.setFlagWithReason(
                        curr.memoryWithoutUpdate, MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                        reason, true, 1f
                    )
                }
            }
        }
    }

    override fun preConfigureFleet(size: Int, m: FleetCreatorMission) {
        m.fleetTypeMedium = FleetTypes.TASK_FORCE // default would be "Patrol", don't want that
        m.fleetTypeLarge = FleetTypes.TASK_FORCE
    }

    override fun configureFleet(size: Int, m: FleetCreatorMission) {
        m.triggerSetFleetFlag(MPC_IAIIC_BOMBARD_FLEET)
        if (size >= 8) {
            m.triggerSetFleetDoctrineOther(5, 0) // more capitals in large fleets
        }
        m.triggerGetFleetParams().tankerPts += 100f
    }

    override fun abort() {
        if (!isAborted) {
            for (curr in getFleets()) {
                curr.memoryWithoutUpdate.unset(MPC_IAIIC_BOMBARD_FLEET)
            }
        }
        super.abort()
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
                    var risk: String? = "are at risk of being raided and losing stability:"
                    var highlight: String? = "losing stability:"
                    if (params.raidParams.bombardment == BombardType.SATURATION) {
                        risk = "are at risk of suffering a saturation bombardment resulting in catastrophic damage:"
                        highlight = "catastrophic damage"
                    } else if (params.raidParams.bombardment == BombardType.TACTICAL) {
                        risk =
                            "are at risk of suffering a targeted bombardment and having their military and ship-making infrastructure disrupted:"
                        highlight = "military and ship-making infrastructure disrupted"
                    } else if (params.raidParams.disrupt.isNotEmpty()) {
                        risk = "are at risk of being raided and having their operations severely disrupted"
                        highlight = "operations severely disrupted"
                    }
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

    override fun createPayloadAction(): GenericPayloadAction {
        return MPC_IAIICBombardAction(params.raidParams, params.payloadDays)
    }
}